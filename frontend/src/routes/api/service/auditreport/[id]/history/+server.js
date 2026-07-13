import { json } from '@sveltejs/kit';
import { getApiBaseUrl } from '$lib/server/env.js';

const API_BASE_URL = getApiBaseUrl();

export async function GET({ params, locals }) {
  const response = await fetch(`${API_BASE_URL}/api/service/auditreport/${params.id}/history`, {
    headers: { Authorization: `Bearer ${locals.jwt_token}` }
  });
  return json(await response.json(), { status: response.status });
}
