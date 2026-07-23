import { useEffect, useState } from 'react';
import {
  Alert,
  Box,
  Button,
  Chip,
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
import { getTVShow, listSeasons, clearEpisodeBlackList, clearEpisodeCoolDown } from '../api/tvshows';
import type { Season, TVShow } from '../types';

function DetailRow({ label, value }: { label: string; value: React.ReactNode }) {
  return (
    <Box sx={{ display: 'flex', py: 1.5, borderBottom: '1px solid', borderColor: 'divider' }}>
      <Typography sx={{ width: 160, flexShrink: 0, color: 'text.secondary', fontSize: 14 }}>
        {label}
      </Typography>
      <Box sx={{ fontSize: 14 }}>{value ?? <Typography sx={{ fontSize: 14 }}>—</Typography>}</Box>
    </Box>
  );
}

function formatDate(value: string | null | undefined): string | null {
  if (!value) return null;
  return new Date(value).toLocaleString();
}

export function TVShowDetailPage() {
  const { id } = useParams<{ id: string }>();
  const { user } = useAuth();
  const navigate = useNavigate();

  const [tvShow, setTVShow] = useState<TVShow | null>(null);
  const [seasons, setSeasons] = useState<Season[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [clearError, setClearError] = useState<string | null>(null);
  const [clearing, setClearing] = useState<'blacklist' | 'cooldown' | null>(null);

  useEffect(() => {
    if (!user || !id) return;
    setLoading(true);
    Promise.all([getTVShow(user, id), listSeasons(user, id)])
      .then(([show, seas]) => {
        setTVShow(show);
        setSeasons(seas);
      })
      .catch((e: Error) => setError(e.message))
      .finally(() => setLoading(false));
  }, [user, id]);

  async function handleClear(type: 'blacklist' | 'cooldown') {
    if (!user || !id) return;
    setClearing(type);
    setClearError(null);
    try {
      if (type === 'blacklist') await clearEpisodeBlackList(user, id);
      else await clearEpisodeCoolDown(user, id);
    } catch (e: unknown) {
      setClearError(e instanceof Error ? e.message : 'Operation failed');
    } finally {
      setClearing(null);
    }
  }

  if (loading) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', mt: 6 }}>
        <CircularProgress />
      </Box>
    );
  }

  if (error || !tvShow) {
    return <Alert severity="error">{error ?? 'TV Show not found'}</Alert>;
  }

  return (
    <>
      <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 3 }}>
        <Button startIcon={<ArrowBackIcon />} onClick={() => navigate('/tvshows')}>
          TV Shows
        </Button>
        <Typography color="text.secondary">/</Typography>
        <Typography fontWeight="medium">{tvShow.originalTitle}</Typography>
      </Box>

      <Box sx={{ display: 'flex', gap: 3, flexWrap: 'wrap', mb: 3 }}>
        <Paper sx={{ flex: '1 1 400px', p: 3 }}>
          <Typography variant="h6" fontWeight="bold" gutterBottom>
            {tvShow.originalTitle}
          </Typography>
          <Divider sx={{ mb: 1 }} />
          <DetailRow
            label="ID"
            value={<Typography sx={{ fontSize: 13, fontFamily: 'monospace' }}>{tvShow.id}</Typography>}
          />
          <DetailRow label="Language" value={tvShow.originalLocale} />
          <DetailRow label="Release Date" value={tvShow.releaseDate} />
          <DetailRow label="TMDB ID" value={tvShow.tmdbId} />
          <DetailRow label="IMDB ID" value={tvShow.imdbId} />
          <DetailRow label="TVDB ID" value={tvShow.tvdbId} />
          <DetailRow
            label="Use TVDB"
            value={
              <Chip
                label={tvShow.useTvdb ? 'Yes' : 'No'}
                size="small"
                color={tvShow.useTvdb ? 'primary' : 'default'}
              />
            }
          />
          <DetailRow label="Last Enriched" value={formatDate(tvShow.lastEnrichedAt)} />
          <DetailRow label="Created At" value={formatDate(tvShow.createdAt)} />
          {clearError && <Alert severity="error" sx={{ mt: 2 }}>{clearError}</Alert>}
          <Box sx={{ display: 'flex', gap: 1, mt: 2 }}>
            <Button
              size="small"
              color="warning"
              variant="outlined"
              disabled={!!clearing}
              onClick={() => handleClear('blacklist')}
            >
              {clearing === 'blacklist' ? <CircularProgress size={16} /> : 'Clear Blacklist'}
            </Button>
            <Button
              size="small"
              color="warning"
              variant="outlined"
              disabled={!!clearing}
              onClick={() => handleClear('cooldown')}
            >
              {clearing === 'cooldown' ? <CircularProgress size={16} /> : 'Clear Cooldown'}
            </Button>
          </Box>
        </Paper>
      </Box>

      <Typography variant="h6" fontWeight="bold" gutterBottom>
        Seasons
      </Typography>
      <Paper>
        <TableContainer>
          <Table>
            <TableHead>
              <TableRow>
                <TableCell>Season #</TableCell>
                <TableCell>Name</TableCell>
                <TableCell>Air Date</TableCell>
                <TableCell>TMDB Season #</TableCell>
                <TableCell>TVDB Season #</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {seasons.map((s) => (
                <TableRow
                  key={s.id}
                  hover
                  sx={{ cursor: 'pointer' }}
                  onClick={() => navigate(`/tvshows/${tvShow.id}/seasons/${s.id}`)}
                >
                  <TableCell>{s.seasonNumber}</TableCell>
                  <TableCell>{s.originalName ?? `Season ${s.seasonNumber}`}</TableCell>
                  <TableCell>{s.airDate ?? '—'}</TableCell>
                  <TableCell>{s.tmdbSeasonNumber ?? '—'}</TableCell>
                  <TableCell>{s.tvdbSeasonNumber ?? '—'}</TableCell>
                </TableRow>
              ))}
              {seasons.length === 0 && (
                <TableRow>
                  <TableCell colSpan={5} align="center" sx={{ color: 'text.secondary' }}>
                    No seasons yet
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
