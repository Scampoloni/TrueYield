import { json } from '@sveltejs/kit';
import { getApiBaseUrl } from '$lib/server/env.js';

const API_BASE_URL = getApiBaseUrl();

export async function GET({ locals, url }) {
    const pageNumber = url.searchParams.get('pageNumber');
    const pageSize = url.searchParams.get('pageSize');

    let backendUrl = `${API_BASE_URL}/api/portfolio`;
    if (pageNumber !== null && pageSize !== null) {
        backendUrl += `?pageNumber=${pageNumber}&pageSize=${pageSize}`;
    }

    const res = await fetch(backendUrl, {
        headers: { 'Authorization': `Bearer ${locals.jwt_token}` }
    });
    const data = await res.json();
    return json(data, { status: res.status });
}

export async function POST({ locals, request }) {
    const body = await request.json();
    const res = await fetch(`${API_BASE_URL}/api/portfolio`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${locals.jwt_token}`
        },
        body: JSON.stringify(body)
    });
    const data = await res.json();
    return json(data, { status: res.status });
}
