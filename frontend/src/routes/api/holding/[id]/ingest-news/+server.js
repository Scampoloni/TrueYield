import { backendRequest } from '$lib/server/backend.js';

export async function POST(event) {
    return backendRequest(
        event,
        `/api/holding/${encodeURIComponent(event.params.id)}/ingest-news`,
        { method: 'POST', timeoutMs: 60_000 }
    );
}
