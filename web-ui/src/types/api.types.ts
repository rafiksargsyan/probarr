export interface UserDTO {
  id: string;
  accountId: string;
}

export interface ApiKeyDTO {
  id: string;
  key: string | null;
  lastAccessTime: string | null;
  description: string;
  disabled: boolean;
}

export interface ClientAudioTrackDTO {
  language: string | null;
  channels: number | null;
}

export interface ClientSubtitleTrackDTO {
  language: string | null;
}

export interface ClientReleaseDTO {
  infoHash: string;
  resolution: string | null;
  ripType: string | null;
  torrentSource: string | null;
  fileIndex: number | null;
  magnetUri: string | null;
  hasTorrentFile: boolean;
  audioTracks: ClientAudioTrackDTO[];
  subtitleTracks: ClientSubtitleTrackDTO[];
}

export interface ClientMovieDTO {
  id: string;
  originalTitle: string;
  releaseDate: string | null;
  runtimeMinutes: number | null;
  tmdbId: number | null;
  imdbId: string | null;
  releases: ClientReleaseDTO[];
}

export interface PageDTO<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}
