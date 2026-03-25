import { json } from '@sveltejs/kit';
import { getApiBaseUrl } from '$lib/server/env.js';

const API_BASE_URL = getApiBaseUrl();

export async function GET({ locals, params }) {
    const res = await fetch(`${API_BASE_URL}/api/service/auditreport/${params.id}`, {
        headers: { 'Authorization': `Bearer ${locals.jwt_token}` }
    });
    const data = await res.json();
    return json(data, { status: res.status });
}
