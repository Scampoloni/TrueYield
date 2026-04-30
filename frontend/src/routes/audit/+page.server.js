import { redirect } from '@sveltejs/kit';
export async function load({ locals }) {
    if (!locals.user) throw redirect(303, '/login');
    if (!locals.user.user_roles?.includes('auditor') && !locals.user.user_roles?.includes('fund-manager') && !locals.user.user_roles?.includes('compliance-officer')) throw redirect(303, '/login');
    return { user: locals.user };
}
