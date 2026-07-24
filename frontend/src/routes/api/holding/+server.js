import { backendRequest } from '$lib/server/backend.js';

export async function GET(event) {
    const portfolioId = event.url.searchParams.get('portfolioId') ?? '';
    return backendRequest(
        event,
        `/api/holding?${new URLSearchParams({ portfolioId })}`
    );
}

export async function POST(event) {
    return backendRequest(event, '/api/holding', {
        method: 'POST',
        headers: {
            'Content-Type': event.request.headers.get('content-type') ?? 'application/json'
        },
        body: await event.request.text(),
        timeoutMs: 60_000
    });
}
