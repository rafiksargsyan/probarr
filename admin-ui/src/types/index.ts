export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

export type Locale =
  | 'en' | 'en-US' | 'en-GB' | 'en-AU'
  | 'ru' | 'fr' | 'fr-FR' | 'fr-CA'
  | 'de' | 'es' | 'es-ES' | 'es-419'
  | 'it' | 'pt' | 'pt-BR' | 'pt-PT'
  | 'ja' | 'ko' | 'zh' | 'zh-Hans-CN' | 'zh-Hant-TW' | 'zh-Hant-HK'
  | 'hi' | 'ta' | 'te' | 'ml' | 'ur' | 'bn'
  | 'tr' | 'el' | 'th' | 'id' | 'ms' | 'vi' | 'tl'
  | 'sq' | 'az' | 'ka'
  | 'da' | 'sv' | 'nb' | 'nl' | 'nl-NL' | 'nl-BE' | 'fi'
  | 'pl' | 'uk' | 'be' | 'bg' | 'cs' | 'sk' | 'sl' | 'hr' | 'sr' | 'bs' | 'mk' | 'hu' | 'ro'
  | 'et' | 'lv' | 'lt'
  | 'hy' | 'fa' | 'ar' | 'he'
  | 'myn';

export const LOCALES: { value: Locale; label: string }[] = [
  { value: 'en',        label: 'English' },
  { value: 'en-US',     label: 'English (US)' },
  { value: 'en-GB',     label: 'English (UK)' },
  { value: 'en-AU',     label: 'English (AU)' },
  { value: 'ru',        label: 'Russian' },
  { value: 'fr',        label: 'French' },
  { value: 'fr-FR',     label: 'French (France)' },
  { value: 'fr-CA',     label: 'French (Canada)' },
  { value: 'de',        label: 'German' },
  { value: 'es',        label: 'Spanish' },
  { value: 'es-ES',     label: 'Spanish (Spain)' },
  { value: 'es-419',    label: 'Spanish (Latin America)' },
  { value: 'it',        label: 'Italian' },
  { value: 'pt',        label: 'Portuguese' },
  { value: 'pt-BR',     label: 'Portuguese (Brazil)' },
  { value: 'pt-PT',     label: 'Portuguese (Portugal)' },
  { value: 'ja',        label: 'Japanese' },
  { value: 'ko',        label: 'Korean' },
  { value: 'zh',        label: 'Chinese' },
  { value: 'zh-Hans-CN', label: 'Chinese (Simplified)' },
  { value: 'zh-Hant-TW', label: 'Chinese (Traditional, TW)' },
  { value: 'zh-Hant-HK', label: 'Chinese (Traditional, HK)' },
  { value: 'hi',        label: 'Hindi' },
  { value: 'ta',        label: 'Tamil' },
  { value: 'te',        label: 'Telugu' },
  { value: 'ml',        label: 'Malayalam' },
  { value: 'ur',        label: 'Urdu' },
  { value: 'bn',        label: 'Bengali' },
  { value: 'tr',        label: 'Turkish' },
  { value: 'el',        label: 'Greek' },
  { value: 'th',        label: 'Thai' },
  { value: 'id',        label: 'Indonesian' },
  { value: 'ms',        label: 'Malay' },
  { value: 'vi',        label: 'Vietnamese' },
  { value: 'tl',        label: 'Filipino' },
  { value: 'sq',        label: 'Albanian' },
  { value: 'az',        label: 'Azerbaijani' },
  { value: 'ka',        label: 'Georgian' },
  { value: 'da',        label: 'Danish' },
  { value: 'sv',        label: 'Swedish' },
  { value: 'nb',        label: 'Norwegian' },
  { value: 'nl',        label: 'Dutch' },
  { value: 'nl-NL',     label: 'Dutch (Netherlands)' },
  { value: 'nl-BE',     label: 'Dutch (Belgium)' },
  { value: 'fi',        label: 'Finnish' },
  { value: 'pl',        label: 'Polish' },
  { value: 'uk',        label: 'Ukrainian' },
  { value: 'be',        label: 'Belarusian' },
  { value: 'bg',        label: 'Bulgarian' },
  { value: 'cs',        label: 'Czech' },
  { value: 'sk',        label: 'Slovak' },
  { value: 'sl',        label: 'Slovenian' },
  { value: 'hr',        label: 'Croatian' },
  { value: 'sr',        label: 'Serbian' },
  { value: 'bs',        label: 'Bosnian' },
  { value: 'mk',        label: 'Macedonian' },
  { value: 'hu',        label: 'Hungarian' },
  { value: 'ro',        label: 'Romanian' },
  { value: 'et',        label: 'Estonian' },
  { value: 'lv',        label: 'Latvian' },
  { value: 'lt',        label: 'Lithuanian' },
  { value: 'hy',        label: 'Armenian' },
  { value: 'fa',        label: 'Persian' },
  { value: 'ar',        label: 'Arabic' },
  { value: 'he',        label: 'Hebrew' },
  { value: 'myn',       label: 'Mayan' },
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
  title: string | null;
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
  releases: Release[];
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
  originalLocale: Locale;
  tmdbId: number | null;
  imdbId: string | null;
  tvdbId: number | null;
  releaseDate: string | null;
  useTvdb: boolean;
  lastEnrichedAt: string | null;
  createdAt: string;
}

export interface Season {
  id: string;
  tvShowId: string;
  seasonNumber: number;
  originalName: string | null;
  airDate: string | null;
  tmdbSeasonNumber: number | null;
  tvdbSeasonNumber: number | null;
}

export interface Episode {
  id: string;
  tvShowId: string;
  seasonNumber: number | null;
  episodeNumber: number | null;
  absoluteNumber: number | null;
  airDate: string | null;
  runtimeSeconds: number | null;
  releaseCandidates: MovieReleaseCandidate[];
  releases: Release[];
  blackList: { infoHash: string; reason: string }[];
  whiteList: string[];
  coolDownList: string[];
  lastScanAt: string | null;
  scanning: boolean;
  scanStartedAt: string | null;
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

export type Resolution = 'SD' | 'HD_720P' | 'FHD_1080P' | 'UHD_4K' | 'UHD_8K';
export type RipType = 'CAM' | 'TELESYNC' | 'DVD' | 'HDTV' | 'WEB' | 'BR';

export interface Release {
  infoHash: string;
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
