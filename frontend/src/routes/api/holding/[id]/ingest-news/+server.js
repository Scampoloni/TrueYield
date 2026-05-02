import { getApiBaseUrl } from '$lib/server/env.js';

const API_BASE_URL = getApiBaseUrl();

export async function POST({ locals, params }) {
    const res = await fetch(`${API_BASE_URL}/api/holding/${params.id}/ingest-news`, {
        method: 'POST',
        headers: { 'Authorization': `Bearer ${locals.jwt_token}` }
    });
    return new Response(null, { status: res.status });
}
