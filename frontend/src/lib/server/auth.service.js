import { getAuthConfig } from './env.js';
import { dev } from '$app/environment';

const authConfig = getAuthConfig();
const AUTH_TIMEOUT_MS = 15_000;

export class AuthServiceError extends Error {
    /**
     * @param {string} message
     * @param {number} status
     * @param {any} data
     */
    constructor(message, status, data = null) {
        super(message);
        this.name = 'AuthServiceError';
        this.status = status;
        this.data = data;
    }
}

/** @typedef {import('@sveltejs/kit').Cookies} Cookies */

/**
 * @param {string} email
 * @param {string} password
 * @param {Cookies=} cookies
 */
export async function signUp(email, password, cookies) {
    await authRequest(`https://${authConfig.domain}/dbconnections/signup`, {
        method: 'POST',
        body: JSON.stringify({
            client_id: authConfig.clientId,
            email,
            password,
            connection: 'Username-Password-Authentication'
        })
    });
    return signIn(email, password, cookies);
}

/**
 * @param {string} email
 * @param {string} password
 * @param {Cookies=} cookies
 */
export async function signIn(email, password, cookies) {
    const tokenResponse = await authRequest(`https://${authConfig.domain}/oauth/token`, {
        method: 'POST',
        body: JSON.stringify({
            grant_type: 'password',
            username: email,
            password,
            audience: authConfig.audience,
            scope: 'openid profile email',
            client_id: authConfig.clientId,
            connection: 'Username-Password-Authentication'
        })
    });

    const { access_token, id_token } = tokenResponse;
    if (!access_token || typeof access_token !== 'string') {
        throw new AuthServiceError('Identity provider returned no access token.', 502, tokenResponse);
    }
    const userInfo = await getUserInfo(access_token);

    const jwtPayload = JSON.parse(decodeBase64Url(access_token.split('.')[1]));
    const rawRoles = jwtPayload.user_roles
        ?? Object.entries(jwtPayload).find(([key]) => key.endsWith('/user_roles'))?.[1]
        ?? jwtPayload.roles
        ?? null;
    if (Array.isArray(rawRoles)) {
        userInfo.user_roles = rawRoles
            .filter((role) => typeof role === 'string')
            .map((role) => role.trim().toLowerCase().replace(/\s+/g, '-'));
    }

    if (cookies) {
        /** @type {Parameters<Cookies['set']>[2]} */
        const cookieOptions = {
            path: '/',
            httpOnly: true,
            secure: !dev,
            sameSite: 'lax',
            maxAge: tokenLifetimeSeconds(jwtPayload)
        };
        cookies.set('jwt_token', access_token, cookieOptions);
        cookies.set('user_info', JSON.stringify(userInfo), cookieOptions);
    }

    return { access_token, id_token, userInfo };
}

/**
 * @param {string} accessToken
 */
export async function getUserInfo(accessToken) {
    return authRequest(`https://${authConfig.domain}/userinfo`, {
        headers: { Authorization: `Bearer ${accessToken}` }
    });
}

/**
 * @param {Cookies} cookies
 */
export async function signOut(cookies) {
    cookies.delete('jwt_token', { path: '/' });
    cookies.delete('user_info', { path: '/' });
}

/**
 * @param {string} url
 * @param {RequestInit} init
 */
async function authRequest(url, init = {}) {
    const controller = new AbortController();
    const timeout = setTimeout(() => controller.abort(), AUTH_TIMEOUT_MS);
    const headers = new Headers(init.headers);
    headers.set('Accept', 'application/json');
    if (init.body) {
        headers.set('Content-Type', 'application/json');
    }

    try {
        const response = await fetch(url, { ...init, headers, signal: controller.signal });
        const contentType = response.headers.get('content-type') ?? '';
        const data = contentType.includes('application/json')
            ? await response.json()
            : await response.text();
        if (!response.ok) {
            const message =
                data?.error_description ??
                data?.description ??
                data?.message ??
                (typeof data === 'string' ? data : 'Authentication request failed.');
            throw new AuthServiceError(message, response.status, data);
        }
        return data;
    } catch (error) {
        if (error instanceof AuthServiceError) {
            throw error;
        }
        const timedOut = error instanceof Error && error.name === 'AbortError';
        throw new AuthServiceError(
            timedOut
                ? 'The identity provider is taking longer than expected. Please retry.'
                : 'The identity provider is temporarily unavailable. Please retry.',
            503
        );
    } finally {
        clearTimeout(timeout);
    }
}

/**
 * @param {Record<string, any>} jwtPayload
 */
function tokenLifetimeSeconds(jwtPayload) {
    if (typeof jwtPayload.exp !== 'number') {
        return 60 * 60;
    }
    return Math.max(1, Math.min(24 * 60 * 60, jwtPayload.exp - Math.floor(Date.now() / 1000)));
}

/**
 * @param {string} value
 */
function decodeBase64Url(value) {
    const normalized = value.replace(/-/g, '+').replace(/_/g, '/');
    const padded = normalized.padEnd(Math.ceil(normalized.length / 4) * 4, '=');
    const bytes = Uint8Array.from(atob(padded), (character) => character.charCodeAt(0));
    return new TextDecoder().decode(bytes);
}
