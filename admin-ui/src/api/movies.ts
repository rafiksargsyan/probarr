import type { User } from 'firebase/auth';
import { apiRequest } from './client';
import type { Locale, Movie, Page } from '../types';

type MovieBody = {
  originalTitle: string;
  originalLocale: Locale;
  releaseDate?: string | null;
  runtimeMinutes?: number | null;
  tmdbId?: number | null;
  alternativeTitles?: string[];
};

export function listMovies(user: User, page: number, size: number): Promise<Page<Movie>> {
  return apiRequest(`/movie?page=${page}&size=${size}`, user);
}
export function getMovie(user: User, id: string): Promise<Movie> {
  return apiRequest(`/movie/${id}`, user);
}
export function createMovie(user: User, body: MovieBody): Promise<Movie> {
  return apiRequest('/movie', user, { method: 'POST', body: JSON.stringify(body) });
}
export function updateMovie(user: User, id: string, body: MovieBody): Promise<Movie> {
  return apiRequest(`/movie/${id}`, user, { method: 'PUT', body: JSON.stringify(body) });
}
export function setForceScan(user: User, id: string, value: boolean): Promise<Movie> {
  return apiRequest(`/movie/${id}/force-scan?value=${value}`, user, { method: 'PATCH' });
}
export function addToBlackList(user: User, id: string, candidateId: string): Promise<Movie> {
  return apiRequest(`/movie/${id}/blacklist/${candidateId}`, user, { method: 'POST' });
}
export function removeFromBlackList(user: User, id: string, candidateId: string): Promise<Movie> {
  return apiRequest(`/movie/${id}/blacklist/${candidateId}`, user, { method: 'DELETE' });
}
export function addToWhiteList(user: User, id: string, candidateId: string): Promise<Movie> {
  return apiRequest(`/movie/${id}/whitelist/${candidateId}`, user, { method: 'POST' });
}
export function removeFromWhiteList(user: User, id: string, candidateId: string): Promise<Movie> {
  return apiRequest(`/movie/${id}/whitelist/${candidateId}`, user, { method: 'DELETE' });
}
