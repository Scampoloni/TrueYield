import { isSignupEnabled } from '$lib/server/env.js';
import { redirect } from '@sveltejs/kit';

export function load() {
    if (!isSignupEnabled()) {
        redirect(303, '/login');
    }
}
