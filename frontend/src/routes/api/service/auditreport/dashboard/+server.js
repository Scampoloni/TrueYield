import { API_BASE_URL } from '$env/static/private';
import { json } from '@sveltejs/kit';

export async function GET({ locals }) {
    const res = await fetch(`${API_BASE_URL}/api/service/auditreport/dashboard`, {
        headers: { 'Authorization': `Bearer ${locals.jwt_token}` }
    });
    const data = await res.json();
    return json(data, { status: res.status });
}
