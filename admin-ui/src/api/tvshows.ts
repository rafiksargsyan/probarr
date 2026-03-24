import type { User } from 'firebase/auth';
import { apiRequest } from './client';
import type { TVShow, Season, Episode, Page } from '../types';

export function listTVShows(user: User, page: number, size: number): Promise<Page<TVShow>> {
  return apiRequest(`/tvshow?page=${page}&size=${size}`, user);
}
export function getTVShow(user: User, id: string): Promise<TVShow> {
  return apiRequest(`/tvshow/${id}`, user);
}
export function createTVShow(user: User, body: { originalTitle: string; imdbId?: string | null; tvdbId?: number | null; sonarrId?: number | null }): Promise<TVShow> {
  return apiRequest('/tvshow', user, { method: 'POST', body: JSON.stringify(body) });
}
export function listSeasons(user: User, tvShowId: string): Promise<Season[]> {
  return apiRequest(`/tvshow/${tvShowId}/season`, user);
}
export function listEpisodes(user: User, tvShowId: string, seasonNumber?: number): Promise<Episode[]> {
  const q = seasonNumber != null ? `?seasonNumber=${seasonNumber}` : '';
  return apiRequest(`/tvshow/${tvShowId}/episode${q}`, user);
}
