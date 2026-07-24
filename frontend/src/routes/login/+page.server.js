import { isSignupEnabled } from '$lib/server/env.js';

export function load() {
    return {
        signupEnabled: isSignupEnabled()
    };
}
