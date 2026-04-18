import { json } from '@sveltejs/kit';
import { getApiBaseUrl } from '$lib/server/env.js';

const API_BASE_URL = getApiBaseUrl();

export async function DELETE({ locals, params }) {
    const res = await fetch(`${API_BASE_URL}/api/evidence/${params.id}`, {
        method: 'DELETE',
        headers: { 'Authorization': `Bearer ${locals.jwt_token}` }
    });
    if (res.status === 204) return new Response(null, { status: 204 });
    const data = await res.json();
    return json(data, { status: res.status });
}
