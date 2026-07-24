import { backendRequest } from '$lib/server/backend.js';

export async function DELETE(event) {
    return backendRequest(event, `/api/holding/${encodeURIComponent(event.params.id)}`, {
        method: 'DELETE'
    });
}
