import type { User } from 'firebase/auth';
import { apiRequest } from './client';
import type { TVShow, Season, Episode, Locale, Page } from '../types';

export function listTVShows(user: User, page: number, size: number): Promise<Page<TVShow>> {
  return apiRequest(`/tvshow?page=${page}&size=${size}`, user);
}
export function getTVShow(user: User, id: string): Promise<TVShow> {
  return apiRequest(`/tvshow/${id}`, user);
}
export function createTVShow(
  user: User,
  body: {
    originalTitle: string;
    originalLocale?: Locale | null;
    tmdbId?: number | null;
    imdbId?: string | null;
    tvdbId?: number | null;
    releaseDate?: string | null;
    useTvdb?: boolean;
  },
): Promise<TVShow> {
  return apiRequest('/tvshow', user, { method: 'POST', body: JSON.stringify(body) });
}
export function listSeasons(user: User, tvShowId: string): Promise<Season[]> {
  return apiRequest(`/tvshow/${tvShowId}/season`, user);
}
export function listEpisodes(user: User, tvShowId: string, seasonNumber?: number): Promise<Episode[]> {
  const q = seasonNumber != null ? `?seasonNumber=${seasonNumber}` : '';
  return apiRequest(`/tvshow/${tvShowId}/episode${q}`, user);
}
export function getEpisode(user: User, tvShowId: string, episodeId: string): Promise<Episode> {
  return apiRequest(`/tvshow/${tvShowId}/episode/${episodeId}`, user);
}
export function updateEpisode(
  user: User,
  tvShowId: string,
  episodeId: string,
  body: {
    seasonNumber?: number | null;
    episodeNumber?: number | null;
    absoluteNumber?: number | null;
    airDate?: string | null;
    runtimeSeconds?: number | null;
  },
): Promise<Episode> {
  return apiRequest(`/tvshow/${tvShowId}/episode/${episodeId}`, user, { method: 'PUT', body: JSON.stringify(body) });
}
export function triggerEpisodeScan(user: User, tvShowId: string, episodeId: string): Promise<Episode> {
  return apiRequest(`/tvshow/${tvShowId}/episode/${episodeId}/scan`, user, { method: 'POST' });
}
export function addToEpisodeBlackList(user: User, tvShowId: string, episodeId: string, infoHash: string): Promise<Episode> {
  return apiRequest(`/tvshow/${tvShowId}/episode/${episodeId}/blacklist/${infoHash}`, user, { method: 'POST' });
}
export function removeFromEpisodeBlackList(user: User, tvShowId: string, episodeId: string, infoHash: string): Promise<Episode> {
  return apiRequest(`/tvshow/${tvShowId}/episode/${episodeId}/blacklist/${infoHash}`, user, { method: 'DELETE' });
}
export function addToEpisodeWhiteList(user: User, tvShowId: string, episodeId: string, infoHash: string): Promise<Episode> {
  return apiRequest(`/tvshow/${tvShowId}/episode/${episodeId}/whitelist/${infoHash}`, user, { method: 'POST' });
}
export function removeFromEpisodeWhiteList(user: User, tvShowId: string, episodeId: string, infoHash: string): Promise<Episode> {
  return apiRequest(`/tvshow/${tvShowId}/episode/${episodeId}/whitelist/${infoHash}`, user, { method: 'DELETE' });
}
export function addToEpisodeCoolDown(user: User, tvShowId: string, episodeId: string, infoHash: string): Promise<Episode> {
  return apiRequest(`/tvshow/${tvShowId}/episode/${episodeId}/cooldown/${infoHash}`, user, { method: 'POST' });
}
export function removeFromEpisodeCoolDown(user: User, tvShowId: string, episodeId: string, infoHash: string): Promise<Episode> {
  return apiRequest(`/tvshow/${tvShowId}/episode/${episodeId}/cooldown/${infoHash}`, user, { method: 'DELETE' });
}
