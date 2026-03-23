import axios from 'axios';
import { AUTH0_DOMAIN, AUTH0_CLIENT_ID, AUTH0_AUDIENCE } from '$env/static/private';

export async function signUp(email, password) {
    await axios.post(`https://${AUTH0_DOMAIN}/dbconnections/signup`, {
        client_id: AUTH0_CLIENT_ID,
        email,
        password,
        connection: 'Username-Password-Authentication'
    });
    return signIn(email, password);
}

export async function signIn(email, password, cookies) {
    const response = await axios.post(`https://${AUTH0_DOMAIN}/oauth/token`, {
        grant_type: 'password',
        username: email,
        password,
        audience: AUTH0_AUDIENCE,
        scope: 'openid profile email',
        client_id: AUTH0_CLIENT_ID,
        connection: 'Username-Password-Authentication'
    });

    const { access_token, id_token } = response.data;
    const userInfo = await getUserInfo(access_token);

    if (cookies) {
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

export async function getUserInfo(accessToken) {
    const response = await axios.get(`https://${AUTH0_DOMAIN}/userinfo`, {
        headers: { Authorization: `Bearer ${accessToken}` }
    });
    return response.data;
}

export async function signOut(cookies) {
    cookies.delete('jwt_token', { path: '/' });
    cookies.delete('user_info', { path: '/' });
}
