import { useEffect, useState } from 'react';
import {
  Alert,
  Box,
  Button,
  CircularProgress,
  Divider,
  Paper,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Typography,
} from '@mui/material';
import ArrowBackIcon from '@mui/icons-material/ArrowBack';
import { useNavigate, useParams } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';
import { listSeasons, listEpisodes } from '../api/tvshows';
import type { Season, Episode } from '../types';

function formatRuntime(seconds: number | null): string {
  if (seconds == null) return '—';
  const mins = Math.round(seconds / 60);
  return `${mins}m`;
}

export function SeasonDetailPage() {
  const { id: tvShowId, seasonId } = useParams<{ id: string; seasonId: string }>();
  const { user } = useAuth();
  const navigate = useNavigate();

  const [season, setSeason] = useState<Season | null>(null);
  const [episodes, setEpisodes] = useState<Episode[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!user || !tvShowId || !seasonId) return;
    setLoading(true);
    listSeasons(user, tvShowId)
      .then((seasons) => {
        const found = seasons.find((s) => s.id === seasonId) ?? null;
        setSeason(found);
        if (found) {
          return listEpisodes(user, tvShowId, found.seasonNumber);
        }
        return [];
      })
      .then((eps) => setEpisodes(eps))
      .catch((e: Error) => setError(e.message))
      .finally(() => setLoading(false));
  }, [user, tvShowId, seasonId]);

  if (loading) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', mt: 6 }}>
        <CircularProgress />
      </Box>
    );
  }

  if (error || !season) {
    return <Alert severity="error">{error ?? 'Season not found'}</Alert>;
  }

  const title = season.originalName ?? `Season ${season.seasonNumber}`;

  return (
    <>
      <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 3 }}>
        <Button startIcon={<ArrowBackIcon />} onClick={() => navigate(`/tvshows/${tvShowId}`)}>
          TV Show
        </Button>
        <Typography color="text.secondary">/</Typography>
        <Typography fontWeight="medium">{title}</Typography>
      </Box>

      <Paper sx={{ p: 3, mb: 3, maxWidth: 500 }}>
        <Typography variant="h6" fontWeight="bold" gutterBottom>
          {title}
        </Typography>
        <Divider sx={{ mb: 1 }} />
        {[
          { label: 'Season #', value: season.seasonNumber },
          { label: 'Air Date', value: season.airDate ?? '—' },
          { label: 'TMDB Season #', value: season.tmdbSeasonNumber ?? '—' },
          { label: 'TVDB Season #', value: season.tvdbSeasonNumber ?? '—' },
          { label: 'Episodes', value: episodes.length },
        ].map(({ label, value }, i, arr) => (
          <Box
            key={label}
            sx={{
              display: 'flex',
              py: 1.5,
              borderBottom: i < arr.length - 1 ? '1px solid' : 'none',
              borderColor: 'divider',
            }}
          >
            <Typography sx={{ width: 140, flexShrink: 0, color: 'text.secondary', fontSize: 14 }}>
              {label}
            </Typography>
            <Typography sx={{ fontSize: 14 }}>{value}</Typography>
          </Box>
        ))}
      </Paper>

      <Typography variant="h6" fontWeight="bold" gutterBottom>
        Episodes
      </Typography>
      <Paper>
        <TableContainer>
          <Table>
            <TableHead>
              <TableRow>
                <TableCell>Season/Ep</TableCell>
                <TableCell>Absolute #</TableCell>
                <TableCell>Air Date</TableCell>
                <TableCell>Runtime</TableCell>
                <TableCell>Candidates</TableCell>
                <TableCell>Releases</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {episodes.map((e) => (
                <TableRow key={e.id} hover sx={{ cursor: 'pointer' }} onClick={() => navigate(`/tvshows/${tvShowId}/episodes/${e.id}`)}>
                  <TableCell>
                    {e.seasonNumber != null && e.episodeNumber != null
                      ? `S${String(e.seasonNumber).padStart(2, '0')}E${String(e.episodeNumber).padStart(2, '0')}`
                      : '—'}
                  </TableCell>
                  <TableCell>{e.absoluteNumber ?? '—'}</TableCell>
                  <TableCell>{e.airDate ?? '—'}</TableCell>
                  <TableCell>{formatRuntime(e.runtimeSeconds)}</TableCell>
                  <TableCell>{e.releaseCandidates.length || '—'}</TableCell>
                  <TableCell>{e.releases.length || '—'}</TableCell>
                </TableRow>
              ))}
              {episodes.length === 0 && (
                <TableRow>
                  <TableCell colSpan={6} align="center" sx={{ color: 'text.secondary' }}>
                    No episodes yet
                  </TableCell>
                </TableRow>
              )}
            </TableBody>
          </Table>
        </TableContainer>
      </Paper>
    </>
  );
}
