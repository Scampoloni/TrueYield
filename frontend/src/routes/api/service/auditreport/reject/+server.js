import { json } from '@sveltejs/kit';
import { getApiBaseUrl } from '$lib/server/env.js';

const API_BASE_URL = getApiBaseUrl();

export async function PUT({ locals, request }) {
    const body = await request.json();
    const res = await fetch(`${API_BASE_URL}/api/service/auditreport/reject`, {
        method: 'PUT',
        headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${locals.jwt_token}`
        },
        body: JSON.stringify(body)
    });
    const data = await res.json();
    return json(data, { status: res.status });
}
