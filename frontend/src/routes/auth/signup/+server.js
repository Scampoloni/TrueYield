import { AuthServiceError, signUp } from '$lib/server/auth.service.js';
import { isSignupEnabled } from '$lib/server/env.js';
import { json } from '@sveltejs/kit';

/** @typedef {import('@sveltejs/kit').Cookies} Cookies */

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

/**
 * @param {unknown} data
 * @returns {string|null}
 */
function auth0SignupMessage(data) {
    if (!data || typeof data !== 'object') return null;
    /** @type {{[key: string]: any}} */
    const payload = data;

    // Auth0 often returns this for duplicate accounts
    if (payload.code === 'user_exists' || payload.name === 'user_exists') {
        return 'This email is already registered. Please sign in instead.';
    }

    // Auth0 password strength payload includes rules[] with verified flags
    if (Array.isArray(payload.rules) && payload.rules.length > 0) {
        return 'Password does not meet policy. Use at least 8 characters and include 3 of 4: lowercase, uppercase, number, special character.';
    }

    const description =
        (typeof payload.error_description === 'string' && payload.error_description) ||
        (typeof payload.description === 'string' && payload.description) ||
        (typeof payload.message === 'string' && payload.message) ||
        '';

    if (description.toLowerCase().includes('invalid sign up')) {
        return 'Signup is invalid. The email may already exist or the password policy is not met.';
    }

    return null;
}

export async function POST({ request, cookies }) {
    if (!isSignupEnabled()) {
        return json(
            { error: 'Registration is invitation-only for this demo environment.' },
            { status: 403 }
        );
    }

    try {
        const { email, password } = await request.json();
        await signUp(email, password, cookies);
        return json({ success: true });
    } catch (e) {
        let message = 'Signup failed';
        if (e instanceof AuthServiceError) {
            const data = e.data;
            const mapped = auth0SignupMessage(data);
            if (mapped) {
                message = mapped;
            } else {
            message = asMessage(
                data?.error_description || data?.description || data?.message || data || e.message,
                message
            );
            }
            if (e.status === 400 && (!data || message === 'Signup failed')) {
                message = 'Signup failed. The email may already exist or the password policy is not met.';
            }
        } else if (e instanceof Error) {
            message = e.message;
        }
        const status = e instanceof AuthServiceError && e.status === 429
            ? 429
            : e instanceof AuthServiceError && e.status >= 500
                ? 503
                : 400;
        return json({ error: message }, { status });
    }
}
