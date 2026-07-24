import { backendRequest } from '$lib/server/backend.js';

export async function GET(event) {
    const holdingId = event.url.searchParams.get('holdingId') ?? '';
    return backendRequest(
        event,
        `/api/evidence?${new URLSearchParams({ holdingId })}`
    );
}

export async function POST(event) {
    return backendRequest(event, '/api/evidence', {
        method: 'POST',
        headers: {
            'Content-Type': event.request.headers.get('content-type') ?? 'application/json'
        },
        body: await event.request.text(),
        timeoutMs: 60_000
    });
}
