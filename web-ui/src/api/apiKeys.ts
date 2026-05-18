import type { User } from 'firebase/auth';
import { apiRequest } from './client';
import type { ApiKeyDTO } from '../types/api.types';

export function listApiKeys(userId: string, user: User): Promise<ApiKeyDTO[]> {
  return apiRequest<ApiKeyDTO[]>(`/user/${userId}/api-key`, user);
}

export function createApiKey(userId: string, description: string, user: User): Promise<ApiKeyDTO> {
  return apiRequest<ApiKeyDTO>(`/user/${userId}/api-key`, user, {
    method: 'POST',
    body: JSON.stringify({ description }),
  });
}

export function disableApiKey(userId: string, keyId: string, user: User): Promise<ApiKeyDTO> {
  return apiRequest<ApiKeyDTO>(`/user/${userId}/api-key/${keyId}/disable`, user, { method: 'PUT' });
}

export function enableApiKey(userId: string, keyId: string, user: User): Promise<ApiKeyDTO> {
  return apiRequest<ApiKeyDTO>(`/user/${userId}/api-key/${keyId}/enable`, user, { method: 'PUT' });
}

export function deleteApiKey(userId: string, keyId: string, user: User): Promise<void> {
  return apiRequest<void>(`/user/${userId}/api-key/${keyId}`, user, { method: 'DELETE' });
}
