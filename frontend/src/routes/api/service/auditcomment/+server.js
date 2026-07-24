import { backendRequest } from '$lib/server/backend.js';

export async function GET(event) {
    const auditReportId = event.url.searchParams.get('auditReportId') ?? '';
    return backendRequest(
        event,
        `/api/service/auditcomment?${new URLSearchParams({ auditReportId })}`
    );
}

export async function POST(event) {
    return backendRequest(event, '/api/service/auditcomment', {
        method: 'POST',
        headers: {
            'Content-Type': event.request.headers.get('content-type') ?? 'application/json'
        },
        body: await event.request.text()
    });
}
