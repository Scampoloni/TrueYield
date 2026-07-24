import { AuthServiceError, signIn } from '$lib/server/auth.service.js';
import { json } from '@sveltejs/kit';

/**
 * @param {unknown} value
 * @param {string} fallback
 * @returns {string}
 */
function asMessage(value, fallback) {
    if (typeof value === 'string' && value.trim()) return value;
    if (value && typeof value === 'object') {
        try {
            return JSON.stringify(value);
        } catch {
            return fallback;
        }
    }
    return fallback;
}

export async function POST({ request, cookies }) {
    try {
        const { email, password } = await request.json();
        await signIn(email, password, cookies);
        return json({ success: true });
    } catch (e) {
        let message = 'Login failed';
        if (e instanceof AuthServiceError) {
            const data = e.data;
            message = asMessage(
                data?.error_description || data?.message || data || e.message,
                message
            );
        } else if (e instanceof Error) {
            message = e.message;
        }
        const status = e instanceof AuthServiceError && e.status === 429
            ? 429
            : e instanceof AuthServiceError && e.status >= 500
                ? 503
                : 401;
        return json({ error: message }, { status });
    }
}
