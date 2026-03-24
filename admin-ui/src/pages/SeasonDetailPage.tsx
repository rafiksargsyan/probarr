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
        <Box sx={{ display: 'flex', py: 1.5, borderBottom: '1px solid', borderColor: 'divider' }}>
          <Typography sx={{ width: 140, flexShrink: 0, color: 'text.secondary', fontSize: 14 }}>Season #</Typography>
          <Typography sx={{ fontSize: 14 }}>{season.seasonNumber}</Typography>
        </Box>
        <Box sx={{ display: 'flex', py: 1.5, borderBottom: '1px solid', borderColor: 'divider' }}>
          <Typography sx={{ width: 140, flexShrink: 0, color: 'text.secondary', fontSize: 14 }}>Air Date</Typography>
          <Typography sx={{ fontSize: 14 }}>{season.airDate ?? '—'}</Typography>
        </Box>
        <Box sx={{ display: 'flex', py: 1.5 }}>
          <Typography sx={{ width: 140, flexShrink: 0, color: 'text.secondary', fontSize: 14 }}>Episodes</Typography>
          <Typography sx={{ fontSize: 14 }}>{episodes.length}</Typography>
        </Box>
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
              </TableRow>
            </TableHead>
            <TableBody>
              {episodes.map((e) => (
                <TableRow key={e.id}>
                  <TableCell>
                    {e.seasonNumber != null && e.episodeNumber != null
                      ? `S${String(e.seasonNumber).padStart(2, '0')}E${String(e.episodeNumber).padStart(2, '0')}`
                      : '—'}
                  </TableCell>
                  <TableCell>{e.absoluteNumber ?? '—'}</TableCell>
                  <TableCell>{e.airDate ?? '—'}</TableCell>
                  <TableCell>{e.runtime != null ? `${e.runtime} min` : '—'}</TableCell>
                </TableRow>
              ))}
              {episodes.length === 0 && (
                <TableRow>
                  <TableCell colSpan={4} align="center" sx={{ color: 'text.secondary' }}>
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
