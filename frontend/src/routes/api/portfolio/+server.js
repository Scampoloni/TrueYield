import { backendRequest } from '$lib/server/backend.js';

export async function GET(event) {
    return backendRequest(event, '/api/portfolio');
}

export async function POST(event) {
    return backendRequest(event, '/api/portfolio', {
        method: 'POST',
        headers: {
            'Content-Type': event.request.headers.get('content-type') ?? 'application/json'
        },
        body: await event.request.text()
    });
}
