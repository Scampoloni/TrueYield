import { backendRequest } from '$lib/server/backend.js';

export async function POST(event) {
    return backendRequest(event, '/api/chat', {
        method: 'POST',
        headers: {
            'Content-Type': event.request.headers.get('content-type') ?? 'application/json'
        },
        body: await event.request.text(),
        timeoutMs: 90_000
    });
}
