export type ApiErrorCode =
  | 'SESSION_EXPIRED'
  | 'BACKEND_TIMEOUT'
  | 'BACKEND_UNAVAILABLE'
  | 'REQUEST_FAILED';

export class ApiError extends Error {
  constructor(
    message: string,
    public readonly status: number,
    public readonly code: ApiErrorCode,
    public readonly requestId?: string
  ) {
    super(message);
    this.name = 'ApiError';
  }
}

export async function apiRequest<T>(input: RequestInfo | URL, init?: RequestInit): Promise<T> {
  const response = await fetch(input, {
    cache: 'no-store',
    ...init
  });

  const contentType = response.headers.get('content-type') ?? '';
  const payload = contentType.includes('application/json')
    ? await response.json()
    : await response.text();

  if (!response.ok) {
    const structuredError =
      payload && typeof payload === 'object' && 'error' in payload && typeof payload.error === 'object'
        ? payload.error
        : null;
    const message =
      structuredError?.message ??
      (typeof payload === 'string' && payload.trim() ? payload : 'The request could not be completed.');
    const code = (structuredError?.code ?? 'REQUEST_FAILED') as ApiErrorCode;

    throw new ApiError(message, response.status, code, structuredError?.requestId);
  }

  return payload as T;
}
