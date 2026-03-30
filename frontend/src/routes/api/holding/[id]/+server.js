import { getApiBaseUrl } from '$lib/server/env.js';

const API_BASE_URL = getApiBaseUrl();

export async function DELETE({ locals, params }) {
    const res = await fetch(`${API_BASE_URL}/api/holding/${params.id}`, {
        method: 'DELETE',
        headers: { 'Authorization': `Bearer ${locals.jwt_token}` }
    });
    return new Response(null, { status: res.status });
}
