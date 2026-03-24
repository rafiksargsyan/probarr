import type { User } from 'firebase/auth';
import { apiRequest } from './client';
import type { ReleaseCandidate, Release, CandidateStatus, Page } from '../types';

export function listCandidates(user: User, page: number, size: number, movieId?: string, episodeId?: string): Promise<Page<ReleaseCandidate>> {
  const params = new URLSearchParams({ page: String(page), size: String(size) });
  if (movieId) params.set('movieId', movieId);
  if (episodeId) params.set('episodeId', episodeId);
  return apiRequest(`/release-candidate?${params}`, user);
}
export function getCandidate(user: User, id: string): Promise<ReleaseCandidate> {
  return apiRequest(`/release-candidate/${id}`, user);
}
export function createCandidate(user: User, body: { movieId?: string | null; episodeId?: string | null; name: string; source: CandidateStatus; tracker?: string | null }): Promise<ReleaseCandidate> {
  return apiRequest('/release-candidate', user, { method: 'POST', body: JSON.stringify(body) });
}
export function updateCandidateStatus(user: User, id: string, status: CandidateStatus): Promise<ReleaseCandidate> {
  return apiRequest(`/release-candidate/${id}/status`, user, { method: 'PATCH', body: JSON.stringify({ status }) });
}
export function getRelease(user: User, candidateId: string): Promise<Release> {
  return apiRequest(`/release/${candidateId}`, user);
}
