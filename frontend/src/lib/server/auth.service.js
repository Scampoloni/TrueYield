import axios from 'axios';
import { getAuthConfig } from './env.js';

const authConfig = getAuthConfig();

/** @typedef {import('@sveltejs/kit').Cookies} Cookies */

/**
 * @param {string} email
 * @param {string} password
 */
export async function signUp(email, password) {
    await axios.post(`https://${authConfig.domain}/dbconnections/signup`, {
        client_id: authConfig.clientId,
        email,
        password,
        connection: 'Username-Password-Authentication'
    });
    return signIn(email, password);
}

/**
 * @param {string} email
 * @param {string} password
 * @param {Cookies=} cookies
 */
export async function signIn(email, password, cookies) {
    const response = await axios.post(`https://${authConfig.domain}/oauth/token`, {
        grant_type: 'password',
        username: email,
        password,
        audience: authConfig.audience,
        scope: 'openid profile email',
        client_id: authConfig.clientId,
        connection: 'Username-Password-Authentication'
    });

    const { access_token, id_token } = response.data;
    const userInfo = await getUserInfo(access_token);

    if (cookies) {
        /** @type {Parameters<Cookies['set']>[2]} */
        const cookieOpts = {
            path: '/',
            httpOnly: true,
            secure: false,
            sameSite: 'lax',
            maxAge: 60 * 60 * 24
        };
        cookies.set('jwt_token', access_token, cookieOpts);
        cookies.set('user_info', JSON.stringify(userInfo), cookieOpts);
    }

    return { access_token, id_token, userInfo };
}

/**
 * @param {string} accessToken
 */
export async function getUserInfo(accessToken) {
    const response = await axios.get(`https://${authConfig.domain}/userinfo`, {
        headers: { Authorization: `Bearer ${accessToken}` }
    });
    return response.data;
}

/**
 * @param {Cookies} cookies
 */
export async function signOut(cookies) {
    cookies.delete('jwt_token', { path: '/' });
    cookies.delete('user_info', { path: '/' });
}
