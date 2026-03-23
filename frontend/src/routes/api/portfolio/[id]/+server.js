import { API_BASE_URL } from '$env/static/private';
import { json } from '@sveltejs/kit';

export async function GET({ locals, params }) {
    const res = await fetch(`${API_BASE_URL}/api/portfolio/${params.id}`, {
        headers: { 'Authorization': `Bearer ${locals.jwt_token}` }
    });
    const data = await res.json();
    return json(data, { status: res.status });
}

export async function PUT({ locals, params, request }) {
    const body = await request.json();
    const res = await fetch(`${API_BASE_URL}/api/portfolio/${params.id}`, {
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

export async function DELETE({ locals, params }) {
    const res = await fetch(`${API_BASE_URL}/api/portfolio/${params.id}`, {
        method: 'DELETE',
        headers: { 'Authorization': `Bearer ${locals.jwt_token}` }
    });
    if (res.status === 204) return new Response(null, { status: 204 });
    const data = await res.json();
    return json(data, { status: res.status });
}
