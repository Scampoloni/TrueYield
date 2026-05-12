import { json } from '@sveltejs/kit';
import { getApiBaseUrl } from '$lib/server/env.js';

const API_BASE_URL = getApiBaseUrl();

export async function GET({ locals, url }) {
    const portfolioId = url.searchParams.get('portfolioId');
    const res = await fetch(`${API_BASE_URL}/api/service/auditreport/latest?portfolioId=${portfolioId}`, {
        headers: { 'Authorization': `Bearer ${locals.jwt_token}` }
    });
    if (!res.ok) return json(null, { status: res.status });
    const data = await res.json();
    return json(data, { status: res.status });
}
