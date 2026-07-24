import { backendRequest } from '$lib/server/backend.js';

export async function GET(event) {
    const portfolioId = event.url.searchParams.get('portfolioId') ?? '';
    return backendRequest(
        event,
        `/api/service/auditreport/latest?${new URLSearchParams({ portfolioId })}`
    );
}
