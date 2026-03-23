import { API_BASE_URL } from '$env/static/private';
import { json } from '@sveltejs/kit';

export async function GET({ locals }) {
    const res = await fetch(`${API_BASE_URL}/api/portfolio`, {
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
