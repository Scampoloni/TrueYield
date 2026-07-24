import { backendRequest } from '$lib/server/backend.js';

export async function GET(event) {
    return backendRequest(event, `/api/portfolio/${encodeURIComponent(event.params.id)}`);
}

export async function PUT(event) {
    return backendRequest(event, `/api/portfolio/${encodeURIComponent(event.params.id)}`, {
        method: 'PUT',
        headers: {
            'Content-Type': event.request.headers.get('content-type') ?? 'application/json'
        },
        body: await event.request.text()
    });
}

export async function DELETE(event) {
    return backendRequest(event, `/api/portfolio/${encodeURIComponent(event.params.id)}`, {
        method: 'DELETE'
    });
}
