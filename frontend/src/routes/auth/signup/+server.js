import { signUp } from '$lib/server/auth.service.js';
import { json } from '@sveltejs/kit';

export async function POST({ request, cookies }) {
    try {
        const { email, password } = await request.json();
        // signUp calls signIn internally and sets cookies
        const result = await signUp(email, password);
        // Set cookies from the sign-in result
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
        const message =
            e.response?.data?.description ||
            e.response?.data?.message ||
            e.message ||
            'Signup failed';
        return json({ error: message }, { status: 400 });
    }
}
