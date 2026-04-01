export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

export type Locale =
  | 'EN_US' | 'EN_GB' | 'EN_AU'
  | 'HY_AM' | 'RU_RU' | 'FR_FR' | 'DE_DE'
  | 'ES_ES' | 'ES_MX' | 'IT_IT' | 'PT_BR'
  | 'ZH_HANS_CN' | 'ZH_HANT_TW' | 'ZH_HANT_HK';

export const LOCALES: { value: Locale; label: string }[] = [
  { value: 'EN_US', label: 'English (US)' },
  { value: 'EN_GB', label: 'English (UK)' },
  { value: 'EN_AU', label: 'English (AU)' },
  { value: 'HY_AM', label: 'Armenian' },
  { value: 'RU_RU', label: 'Russian' },
  { value: 'FR_FR', label: 'French' },
  { value: 'DE_DE', label: 'German' },
  { value: 'ES_ES', label: 'Spanish (Spain)' },
  { value: 'ES_MX', label: 'Spanish (Mexico)' },
  { value: 'IT_IT', label: 'Italian' },
  { value: 'PT_BR', label: 'Portuguese (Brazil)' },
  { value: 'ZH_HANS_CN', label: 'Chinese (Simplified)' },
  { value: 'ZH_HANT_TW', label: 'Chinese (Traditional, TW)' },
  { value: 'ZH_HANT_HK', label: 'Chinese (Traditional, HK)' },
];

export interface MovieReleaseCandidate {
  infoHash: string;
  downloadUrl: string | null;
  infoUrl: string | null;
  tracker: string;
  sizeInBytes: number | null;
  seeders: number | null;
  resolution: string;
  ripType: string;
  edition: string | null;
  releaseAt: string | null;
  languages: string[];
}

export interface Movie {
  id: string;
  originalTitle: string;
  originalLocale: Locale;
  releaseDate: string | null;
  runtimeMinutes: number | null;
  tmdbId: number | null;
  alternativeTitles: string[];
  releaseCandidates: MovieReleaseCandidate[];
  blackList: { infoHash: string; reason: string }[];
  whiteList: string[];
  coolDownList: string[];
  lastScanAt: string | null;
  forceScan: boolean;
  scanning: boolean;
  scanStartedAt: string | null;
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
