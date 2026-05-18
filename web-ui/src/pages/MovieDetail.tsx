import { useEffect, useState } from 'react';
import {
  Box, Typography, CircularProgress, Alert, Chip, Divider,
  Paper, Stack, Button, Tooltip,
} from '@mui/material';
import ArrowBackIcon from '@mui/icons-material/ArrowBack';
import OpenInNewIcon from '@mui/icons-material/OpenInNew';
import MagnetIcon from '@mui/icons-material/Link';
import DownloadIcon from '@mui/icons-material/Download';
import { useParams, useNavigate } from 'react-router-dom';
import type { User } from 'firebase/auth';
import { useAuth } from '../hooks/useAuth';
import { getMovie, downloadTorrent } from '../api/movies';
import type { ClientMovieDTO, ClientReleaseDTO } from '../types/api.types';

function resolutionColor(res: string | null): 'default' | 'primary' | 'secondary' | 'success' {
  if (!res) return 'default';
  if (res.includes('2160') || res === 'UHD_4K') return 'secondary';
  if (res.includes('1080') || res === 'FHD_1080P') return 'primary';
  if (res.includes('720') || res === 'HD_720P') return 'success';
  return 'default';
}

function ReleaseCard({ release, user }: { release: ClientReleaseDTO; user: User }) {
  const [copied, setCopied] = useState(false);
  const [downloading, setDownloading] = useState(false);

  function copyMagnet() {
    const text = release.magnetUri!;
    if (navigator.clipboard) {
      navigator.clipboard.writeText(text).then(() => {
        setCopied(true);
        setTimeout(() => setCopied(false), 2000);
      });
    } else {
      const ta = document.createElement('textarea');
      ta.value = text;
      ta.style.position = 'fixed';
      ta.style.opacity = '0';
      document.body.appendChild(ta);
      ta.focus();
      ta.select();
      document.execCommand('copy');
      document.body.removeChild(ta);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    }
  }

  function handleDownload() {
    setDownloading(true);
    downloadTorrent(release.infoHash, user).finally(() => setDownloading(false));
  }

  return (
    <Paper variant="outlined" sx={{ p: 2 }}>
      <Stack direction="row" spacing={1} flexWrap="wrap" sx={{ mb: 1 }}>
        {release.resolution && (
          <Chip label={release.resolution} size="small" color={resolutionColor(release.resolution)} />
        )}
        {release.ripType && (
          <Chip label={release.ripType} size="small" variant="outlined" />
        )}
      </Stack>

      {release.audioTracks.length > 0 && (
        <Box sx={{ mb: 0.5 }}>
          <Typography variant="caption" color="text.secondary" fontWeight="bold">
            Audio
          </Typography>
          <Stack direction="row" spacing={0.5} flexWrap="wrap" sx={{ mt: 0.5 }}>
            {release.audioTracks.map((t, i) => (
              <Chip
                key={i}
                label={[t.language ?? '?', t.channels ? `${t.channels}ch` : null].filter(Boolean).join(' ')}
                size="small"
                variant="outlined"
              />
            ))}
          </Stack>
        </Box>
      )}

      {release.subtitleTracks.length > 0 && (
        <Box sx={{ mb: 0.5 }}>
          <Typography variant="caption" color="text.secondary" fontWeight="bold">
            Subtitles
          </Typography>
          <Stack direction="row" spacing={0.5} flexWrap="wrap" sx={{ mt: 0.5 }}>
            {release.subtitleTracks.map((t, i) => (
              <Chip key={i} label={t.language ?? '?'} size="small" variant="outlined" />
            ))}
          </Stack>
        </Box>
      )}

      {(release.magnetUri || release.hasTorrentFile) && (
        <Stack direction="row" spacing={1} sx={{ mt: 1.5 }}>
          {release.magnetUri && (
            <Tooltip title={copied ? 'Copied!' : 'Copy magnet link'}>
              <Button
                size="small"
                variant="outlined"
                startIcon={<MagnetIcon />}
                onClick={copyMagnet}
                color={copied ? 'success' : 'primary'}
              >
                {copied ? 'Copied' : 'Copy Magnet'}
              </Button>
            </Tooltip>
          )}
          {release.hasTorrentFile && (
            <Button
              size="small"
              variant="outlined"
              startIcon={<DownloadIcon />}
              onClick={handleDownload}
              disabled={downloading}
            >
              {downloading ? 'Downloading…' : 'Download .torrent'}
            </Button>
          )}
        </Stack>
      )}
    </Paper>
  );
}

export function MovieDetail() {
  const { id } = useParams<{ id: string }>();
  const { user } = useAuth();
  const navigate = useNavigate();
  const [movie, setMovie] = useState<ClientMovieDTO | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!user || !id) return;
    setLoading(true);
    setError(null);
    getMovie(id, user)
      .then(setMovie)
      .catch((e: Error) => setError(e.message))
      .finally(() => setLoading(false));
  }, [user, id]);

  return (
    <Box>
      <Button
        startIcon={<ArrowBackIcon />}
        onClick={() => navigate('/movies')}
        sx={{ mb: 2 }}
      >
        Movies
      </Button>

      {loading && (
        <Box sx={{ display: 'flex', justifyContent: 'center', mt: 6 }}>
          <CircularProgress />
        </Box>
      )}

      {error && <Alert severity="error">{error}</Alert>}

      {movie && (
        <Box>
          <Typography variant="h5" fontWeight="bold" gutterBottom>
            {movie.originalTitle}
          </Typography>

          <Stack direction="row" spacing={1} flexWrap="wrap" sx={{ mb: 2 }}>
            {movie.releaseDate && (
              <Chip label={movie.releaseDate.substring(0, 4)} size="small" />
            )}
            {movie.runtimeMinutes && (
              <Chip label={`${movie.runtimeMinutes} min`} size="small" />
            )}
            {movie.imdbId && (
              <Chip
                label="IMDb"
                size="small"
                variant="outlined"
                onClick={() => window.open(`https://www.imdb.com/title/${movie.imdbId}`, '_blank')}
                onDelete={() => window.open(`https://www.imdb.com/title/${movie.imdbId}`, '_blank')}
                deleteIcon={<Tooltip title="Open IMDb"><OpenInNewIcon /></Tooltip>}
              />
            )}
            {movie.tmdbId && (
              <Chip
                label="TMDB"
                size="small"
                variant="outlined"
                onClick={() => window.open(`https://www.themoviedb.org/movie/${movie.tmdbId}`, '_blank')}
                onDelete={() => window.open(`https://www.themoviedb.org/movie/${movie.tmdbId}`, '_blank')}
                deleteIcon={<Tooltip title="Open TMDB"><OpenInNewIcon /></Tooltip>}
              />
            )}
          </Stack>

          <Divider sx={{ mb: 3 }} />

          <Typography variant="h6" fontWeight="bold" gutterBottom>
            Releases{movie.releases.length > 0 ? ` (${movie.releases.length})` : ''}
          </Typography>

          {movie.releases.length === 0 ? (
            <Typography color="text.secondary">No releases available yet.</Typography>
          ) : (
            <Stack spacing={2}>
              {movie.releases.map((r) => (
                <ReleaseCard key={r.infoHash} release={r} user={user!} />
              ))}
            </Stack>
          )}
        </Box>
      )}
    </Box>
  );
}
