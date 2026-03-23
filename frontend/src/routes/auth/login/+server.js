import { signIn } from '$lib/server/auth.service.js';
import { json } from '@sveltejs/kit';

export async function POST({ request, cookies }) {
    try {
        const { email, password } = await request.json();
        await signIn(email, password, cookies);
        return json({ success: true });
    } catch (e) {
        const message =
            e.response?.data?.error_description ||
            e.response?.data?.message ||
            e.message ||
            'Login failed';
        return json({ error: message }, { status: 401 });
    }
}
