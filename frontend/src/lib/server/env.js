// @ts-ignore SvelteKit virtual module
import { env } from '$env/dynamic/private';

/**
 * @param {string} key
 * @returns {string}
 */
function requireEnv(key) {
    const value = env[key];
    if (!value) {
        throw new Error(`Missing required environment variable: ${key}`);
    }
    return value;
}

export function getApiBaseUrl() {
    return requireEnv('API_BASE_URL');
}

export function getAuthConfig() {
    return {
        domain: requireEnv('AUTH0_DOMAIN'),
        clientId: requireEnv('AUTH0_CLIENT_ID'),
        audience: requireEnv('AUTH0_AUDIENCE')
    };
}