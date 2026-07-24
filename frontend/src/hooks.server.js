import { clearSession } from '$lib/server/backend.js';

export async function handle({ event, resolve }) {
    const jwt_token = event.cookies.get('jwt_token');
    const user_info_raw = event.cookies.get('user_info');

    if (jwt_token && user_info_raw) {
        try {
            const payload = parseJwtPayload(jwt_token);
            const expiresAt = typeof payload.exp === 'number' ? payload.exp * 1000 : 0;
            if (!expiresAt || expiresAt <= Date.now()) {
                throw new Error('Expired or invalid access token');
            }

            event.locals.jwt_token = jwt_token;
            const user = JSON.parse(user_info_raw);
            const tokenRoles = extractRoles(payload);
            if (tokenRoles.length > 0) {
                user.user_roles = tokenRoles;
            }
            event.locals.user = user;
            event.locals.isAuthenticated = true;
        } catch {
            clearSession(event.cookies);
            event.locals.isAuthenticated = false;
            event.locals.user = null;
            event.locals.jwt_token = null;
        }
    } else {
        event.locals.isAuthenticated = false;
        event.locals.user = null;
        event.locals.jwt_token = null;
    }

    // Protect main app routes — redirect to /login if not authenticated
    const protectedPaths = ['/', '/portfolios', '/holdings', '/audit', '/account', '/compliance', '/chat'];
    const isProtected = protectedPaths.some(
        (p) => event.url.pathname === p || event.url.pathname.startsWith(p + '/')
    );

    if (isProtected && !event.locals.isAuthenticated) {
        return new Response(null, {
            status: 302,
            headers: { Location: '/login' }
        });
    }

    return resolve(event);
}

/**
 * @param {string} token
 * @returns {Record<string, any>}
 */
function parseJwtPayload(token) {
    const parts = token.split('.');
    if (parts.length !== 3) {
        throw new Error('Malformed access token');
    }
    return JSON.parse(decodeBase64Url(parts[1]));
}

/**
 * @param {Record<string, any>} payload
 * @returns {string[]}
 */
function extractRoles(payload) {
    const rawRoles =
        payload.user_roles ??
        Object.entries(payload).find(([key]) => key.endsWith('/user_roles'))?.[1] ??
        payload.roles ??
        [];
    if (!Array.isArray(rawRoles)) {
        return [];
    }
    return rawRoles
        .filter((role) => typeof role === 'string')
        .map((role) => role.trim().toLowerCase().replace(/\s+/g, '-'));
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
