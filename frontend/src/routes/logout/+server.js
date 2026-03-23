import { signOut } from '$lib/server/auth.service.js';
import { redirect } from '@sveltejs/kit';

export async function POST({ cookies }) {
    await signOut(cookies);
    throw redirect(302, '/login');
}
