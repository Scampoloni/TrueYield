import { redirect } from '@sveltejs/kit';

export async function load({ locals }) {
    if (!locals.user) throw redirect(303, '/login');
    return { roles: locals.user?.user_roles ?? [] };
}
