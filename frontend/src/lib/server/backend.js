import { getApiBaseUrl } from './env.js';

const DEFAULT_TIMEOUT_MS = 30_000;

/**
 * Forward a same-origin SvelteKit API request to the protected backend.
 * Authentication failures and connectivity problems are normalized so the UI
 * can distinguish an expired session from an unavailable service.
 *
 * @param {import('@sveltejs/kit').RequestEvent} event
 * @param {string} path
 * @param {RequestInit & { timeoutMs?: number }} [init]
 * @returns {Promise<Response>}
 */
export async function backendRequest(event, path, init = {}) {
    const requestId = crypto.randomUUID();
    const token = event.locals.jwt_token;

    if (!token) {
        clearSession(event.cookies);
        return apiError(401, 'SESSION_EXPIRED', 'Your session has expired. Please sign in again.', requestId);
    }

    const { timeoutMs = DEFAULT_TIMEOUT_MS, ...requestInit } = init;
    const controller = new AbortController();
    const timeout = setTimeout(() => controller.abort(), timeoutMs);
    const headers = new Headers(requestInit.headers);
    headers.set('Authorization', `Bearer ${token}`);
    headers.set('Accept', 'application/json');
    headers.set('X-Request-ID', requestId);

    try {
        const response = await fetch(`${getApiBaseUrl()}${path}`, {
            ...requestInit,
            headers,
            signal: controller.signal
        });

        if (response.status === 401) {
            clearSession(event.cookies);
            return apiError(
                401,
                'SESSION_EXPIRED',
                'Your session has expired. Please sign in again.',
                requestId
            );
        }

        const body = response.status === 204 ? null : await response.text();
        const responseHeaders = new Headers();
        responseHeaders.set('Cache-Control', 'no-store');
        responseHeaders.set('X-Request-ID', requestId);
        const contentType = response.headers.get('content-type');
        if (contentType) {
            responseHeaders.set('Content-Type', contentType);
        }

        return new Response(body, {
            status: response.status,
            headers: responseHeaders
        });
    } catch (error) {
        const timedOut = error instanceof Error && error.name === 'AbortError';
        console.error(
            `[backend:${requestId}] ${timedOut ? 'request timed out' : 'request failed'}:`,
            error instanceof Error ? error.message : 'unknown error'
        );
        return apiError(
            503,
            timedOut ? 'BACKEND_TIMEOUT' : 'BACKEND_UNAVAILABLE',
            timedOut
                ? 'The data service is taking longer than expected. Please retry in a moment.'
                : 'The data service is temporarily unavailable. Please retry in a moment.',
            requestId
        );
    } finally {
        clearTimeout(timeout);
    }
}

/**
 * @param {import('@sveltejs/kit').Cookies} cookies
 */
export function clearSession(cookies) {
    cookies.delete('jwt_token', { path: '/' });
    cookies.delete('user_info', { path: '/' });
}

/**
 * @param {number} status
 * @param {string} code
 * @param {string} message
 * @param {string} requestId
 */
function apiError(status, code, message, requestId) {
    return Response.json(
        { error: { code, message, requestId } },
        {
            status,
            headers: {
                'Cache-Control': 'no-store',
                'X-Request-ID': requestId
            }
        }
    );
}
