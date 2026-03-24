import { signUp } from '$lib/server/auth.service.js';
import { json } from '@sveltejs/kit';
import axios from 'axios';

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
    try {
        const { email, password } = await request.json();
        // signUp calls signIn internally and sets cookies
        const result = await signUp(email, password);
        // Set cookies from the sign-in result
        /** @type {Parameters<Cookies['set']>[2]} */
        const cookieOpts = {
            path: '/',
            httpOnly: true,
            secure: false,
            sameSite: 'lax',
            maxAge: 60 * 60 * 24
        };
        cookies.set('jwt_token', result.access_token, cookieOpts);
        cookies.set('user_info', JSON.stringify(result.userInfo), cookieOpts);
        return json({ success: true });
    } catch (e) {
        let message = 'Signup failed';
        if (axios.isAxiosError(e)) {
            const data = e.response?.data;
            const mapped = auth0SignupMessage(data);
            if (mapped) {
                message = mapped;
            } else {
            message = asMessage(
                data?.error_description || data?.description || data?.message || data || e.message,
                message
            );
            }
            if (e.response?.status === 400 && (!data || message === 'Signup failed')) {
                message = 'Signup failed. The email may already exist or the password policy is not met.';
            }
        } else if (e instanceof Error) {
            message = e.message;
        }
        return json({ error: message }, { status: 400 });
    }
}
