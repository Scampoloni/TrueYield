import { json } from '@sveltejs/kit';
import { getApiBaseUrl } from '$lib/server/env.js';

const API_BASE_URL = getApiBaseUrl();

export async function GET({ locals, url }) {
    const auditReportId = url.searchParams.get('auditReportId') ?? '';
    const res = await fetch(`${API_BASE_URL}/api/service/auditcomment?auditReportId=${auditReportId}`, {
        headers: { 'Authorization': `Bearer ${locals.jwt_token}` }
    });
    const data = await res.json();
    return json(data, { status: res.status });
}

export async function POST({ locals, request }) {
    const body = await request.json();
    const res = await fetch(`${API_BASE_URL}/api/service/auditcomment`, {
        method: 'POST',
        headers: {
            'Authorization': `Bearer ${locals.jwt_token}`,
            'Content-Type': 'application/json'
        },
        body: JSON.stringify(body)
    });
    const data = await res.json();
    return json(data, { status: res.status });
}
