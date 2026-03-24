import type { User } from 'firebase/auth';
import { apiRequest } from './client';
import type { Movie, Page } from '../types';

export function listMovies(user: User, page: number, size: number): Promise<Page<Movie>> {
  return apiRequest(`/movie?page=${page}&size=${size}`, user);
}
export function getMovie(user: User, id: string): Promise<Movie> {
  return apiRequest(`/movie/${id}`, user);
}
export function createMovie(user: User, body: { originalTitle: string; year?: number | null; imdbId?: string | null; tmdbId?: number | null; radarrId?: number | null }): Promise<Movie> {
  return apiRequest('/movie', user, { method: 'POST', body: JSON.stringify(body) });
}
export function updateMovie(user: User, id: string, body: { originalTitle: string; year?: number | null; imdbId?: string | null; tmdbId?: number | null; radarrId?: number | null }): Promise<Movie> {
  return apiRequest(`/movie/${id}`, user, { method: 'PUT', body: JSON.stringify(body) });
}
