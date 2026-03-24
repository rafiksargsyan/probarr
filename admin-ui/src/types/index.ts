export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

export interface Movie {
  id: string;
  originalTitle: string;
  year: number | null;
  imdbId: string | null;
  tmdbId: number | null;
  radarrId: number | null;
  createdAt: string;
}

export interface TVShow {
  id: string;
  originalTitle: string;
  imdbId: string | null;
  tvdbId: number | null;
  sonarrId: number | null;
  createdAt: string;
}

export interface Season {
  id: string;
  tvShowId: string;
  seasonNumber: number;
  originalName: string | null;
  airDate: string | null;
}

export interface Episode {
  id: string;
  tvShowId: string;
  seasonNumber: number | null;
  episodeNumber: number | null;
  absoluteNumber: number | null;
  airDate: string | null;
  runtime: number | null;
}

export type CandidateSource = 'RADARR' | 'SONARR' | 'MANUAL';
export type CandidateStatus = 'PENDING' | 'DOWNLOADING' | 'DOWNLOADED' | 'INDEXED' | 'FAILED';
export type Resolution = 'SD' | 'HD_720P' | 'FHD_1080P' | 'UHD_4K' | 'UHD_8K';
export type RipType = 'BLURAY' | 'WEBRIP' | 'WEBDL' | 'HDTV' | 'DVDRIP' | 'CAM' | 'UNKNOWN';

export interface ReleaseCandidate {
  id: string;
  movieId: string | null;
  episodeId: string | null;
  name: string;
  infoHash: string | null;
  sizeBytes: number | null;
  source: CandidateSource;
  status: CandidateStatus;
  tracker: string | null;
  createdAt: string;
}

export interface AudioTrack {
  language: string | null;
  codec: string;
  channels: number;
  isDefault: boolean;
}

export interface SubtitleTrack {
  language: string | null;
  format: string;
  isDefault: boolean;
  isForced: boolean;
}

export interface Release {
  id: string;
  candidateId: string;
  filePath: string;
  fileSizeBytes: number;
  videoCodec: string;
  resolution: Resolution;
  ripType: RipType;
  runtimeSeconds: number;
  audioTracks: AudioTrack[];
  subtitleTracks: SubtitleTrack[];
  createdAt: string;
}
