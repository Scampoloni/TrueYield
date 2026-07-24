import { backendRequest } from '$lib/server/backend.js';

export async function GET(event) {
    return backendRequest(
        event,
        `/api/service/auditreport/${encodeURIComponent(event.params.id)}`
    );
}
