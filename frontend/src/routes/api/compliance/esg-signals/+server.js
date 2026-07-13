import { json } from '@sveltejs/kit';
import { getApiBaseUrl } from '$lib/server/env.js';

const API_BASE_URL = getApiBaseUrl();

export async function GET({ locals }) {
  const res = await fetch(`${API_BASE_URL}/api/compliance/esg-signals`, {
    headers: { Authorization: `Bearer ${locals.jwt_token}` }
  });
  return json(await res.json(), { status: res.status });
}
