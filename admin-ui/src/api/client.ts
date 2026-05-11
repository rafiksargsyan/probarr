import type { User } from 'firebase/auth';
import { env } from '../lib/env';

const BASE_URL = env.VITE_API_BASE_URL;

export async function apiRequest<T>(
  path: string,
  user: User,
  options: RequestInit = {},
): Promise<T> {
  const token = await user.getIdToken();
  const { headers: extraHeaders, ...fetchOptions } = options;

  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    Authorization: `Bearer ${token}`,
    ...(extraHeaders as Record<string, string> | undefined),
  };

  const response = await fetch(`${BASE_URL}/admin${path}`, { ...fetchOptions, headers });

  if (!response.ok) {
    const body = await response.json().catch(() => null);
    throw new Error(body?.message ?? `Request failed: ${response.status}`);
  }

  if (response.status === 204) return undefined as T;
  return response.json() as Promise<T>;
}
