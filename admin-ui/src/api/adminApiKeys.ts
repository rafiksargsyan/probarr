import type { User } from 'firebase/auth';
import { apiRequest } from './client';
import type { AdminApiKey, AdminProfile } from '../types';

export function signup(user: User): Promise<AdminProfile> {
  return apiRequest('/signup', user, { method: 'POST' });
}

export function listAdminApiKeys(user: User): Promise<AdminApiKey[]> {
  return apiRequest('/api-key', user);
}

export function createAdminApiKey(user: User, description: string): Promise<AdminApiKey> {
  return apiRequest('/api-key', user, { method: 'POST', body: JSON.stringify({ description }) });
}

export function disableAdminApiKey(user: User, keyId: string): Promise<void> {
  return apiRequest(`/api-key/${keyId}/disable`, user, { method: 'PUT' });
}

export function enableAdminApiKey(user: User, keyId: string): Promise<void> {
  return apiRequest(`/api-key/${keyId}/enable`, user, { method: 'PUT' });
}

export function deleteAdminApiKey(user: User, keyId: string): Promise<void> {
  return apiRequest(`/api-key/${keyId}`, user, { method: 'DELETE' });
}
