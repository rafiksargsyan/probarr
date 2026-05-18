import type { User } from 'firebase/auth';
import { apiRequest } from './client';
import { env } from '../lib/env';
import type { ClientMovieDTO, PageDTO } from '../types/api.types';

export function listMovies(user: User, page = 0, size = 20): Promise<PageDTO<ClientMovieDTO>> {
  return apiRequest<PageDTO<ClientMovieDTO>>(`/movie?page=${page}&size=${size}`, user);
}

export function getMovie(id: string, user: User): Promise<ClientMovieDTO> {
  return apiRequest<ClientMovieDTO>(`/movie/${id}`, user);
}

export async function downloadTorrent(infoHash: string, user: User): Promise<void> {
  const token = await user.getIdToken();
  const response = await fetch(`${env.VITE_API_BASE_URL}/movie/torrent/${infoHash}`, {
    headers: { Authorization: `Bearer ${token}` },
  });
  if (!response.ok) throw new Error(`Download failed: ${response.status}`);
  const blob = await response.blob();
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = `${infoHash}.torrent`;
  a.click();
  URL.revokeObjectURL(url);
}
