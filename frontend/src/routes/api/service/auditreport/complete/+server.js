import { backendRequest } from '$lib/server/backend.js';

export async function PUT(event) {
    return backendRequest(event, '/api/service/auditreport/complete', {
        method: 'PUT',
        headers: {
            'Content-Type': event.request.headers.get('content-type') ?? 'application/json'
        },
        body: await event.request.text()
    });
}
