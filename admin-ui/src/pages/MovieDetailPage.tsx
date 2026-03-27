import { useEffect, useState } from 'react';
import {
  Alert,
  Box,
  Button,
  CircularProgress,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  Divider,
  MenuItem,
  Paper,
  TextField,
  Typography,
} from '@mui/material';
import ArrowBackIcon from '@mui/icons-material/ArrowBack';
import EditIcon from '@mui/icons-material/Edit';
import { useNavigate, useParams } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';
import { getMovie, updateMovie } from '../api/movies';
import type { Locale, Movie } from '../types';
import { LOCALES } from '../types';

function DetailRow({ label, value }: { label: string; value: React.ReactNode }) {
  return (
    <Box sx={{ display: 'flex', py: 1.5, borderBottom: '1px solid', borderColor: 'divider' }}>
      <Typography sx={{ width: 160, flexShrink: 0, color: 'text.secondary', fontSize: 14 }}>
        {label}
      </Typography>
      <Typography sx={{ fontSize: 14 }}>{value ?? '—'}</Typography>
    </Box>
  );
}

export function MovieDetailPage() {
  const { id } = useParams<{ id: string }>();
  const { user } = useAuth();
  const navigate = useNavigate();

  const [movie, setMovie] = useState<Movie | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const [editOpen, setEditOpen] = useState(false);
  const [saving, setSaving] = useState(false);
  const [formError, setFormError] = useState<string | null>(null);
  const [form, setForm] = useState({
    originalTitle: '',
    originalLocale: 'EN_US' as Locale,
    releaseDate: '',
    runtimeMinutes: '',
    tmdbId: '',
    alternativeTitles: '',
  });

  useEffect(() => {
    if (!user || !id) return;
    setLoading(true);
    getMovie(user, id)
      .then((m) => {
        setMovie(m);
      })
      .catch((e: Error) => setError(e.message))
      .finally(() => setLoading(false));
  }, [user, id]);

  function openEdit() {
    if (!movie) return;
    setForm({
      originalTitle: movie.originalTitle,
      originalLocale: movie.originalLocale,
      releaseDate: movie.releaseDate ?? '',
      runtimeMinutes: movie.runtimeMinutes != null ? String(movie.runtimeMinutes) : '',
      tmdbId: movie.tmdbId != null ? String(movie.tmdbId) : '',
      alternativeTitles: movie.alternativeTitles.join('\n'),
    });
    setFormError(null);
    setEditOpen(true);
  }

  async function handleUpdate() {
    if (!user || !id || !form.originalTitle.trim()) return;
    setSaving(true);
    setFormError(null);
    try {
      const updated = await updateMovie(user, id, {
        originalTitle: form.originalTitle.trim(),
        originalLocale: form.originalLocale,
        releaseDate: form.releaseDate || null,
        runtimeMinutes: form.runtimeMinutes ? parseInt(form.runtimeMinutes) : null,
        tmdbId: form.tmdbId ? parseInt(form.tmdbId) : null,
        alternativeTitles: form.alternativeTitles ? form.alternativeTitles.split('\n').map(t => t.trim()).filter(Boolean) : [],
      });
      setMovie(updated);
      setEditOpen(false);
    } catch (e: unknown) {
      setFormError(e instanceof Error ? e.message : 'Failed to update movie');
    } finally {
      setSaving(false);
    }
  }

  if (loading) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', mt: 6 }}>
        <CircularProgress />
      </Box>
    );
  }

  if (error || !movie) {
    return <Alert severity="error">{error ?? 'Movie not found'}</Alert>;
  }

  return (
    <>
      <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 3 }}>
        <Button startIcon={<ArrowBackIcon />} onClick={() => navigate('/movies')}>
          Movies
        </Button>
        <Typography color="text.secondary">/</Typography>
        <Typography fontWeight="medium">{movie.originalTitle}</Typography>
      </Box>

      <Box sx={{ display: 'flex', gap: 3, flexWrap: 'wrap' }}>
        <Paper sx={{ flex: '1 1 400px', p: 3 }}>
          <Box sx={{ display: 'flex', alignItems: 'center', mb: 1 }}>
            <Typography variant="h6" fontWeight="bold" sx={{ flexGrow: 1 }}>
              {movie.originalTitle}
            </Typography>
            <Button size="small" startIcon={<EditIcon />} onClick={openEdit}>
              Edit
            </Button>
          </Box>
          <Divider sx={{ mb: 1 }} />
          <DetailRow label="ID" value={<Typography sx={{ fontSize: 13, fontFamily: 'monospace' }}>{movie.id}</Typography>} />
          <DetailRow label="Language" value={movie.originalLocale} />
          <DetailRow label="Release Date" value={movie.releaseDate} />
          <DetailRow label="Runtime" value={movie.runtimeMinutes != null ? `${movie.runtimeMinutes}m` : null} />
          <DetailRow label="TMDB ID" value={movie.tmdbId} />
          <DetailRow label="Last Scan" value={movie.lastScanAt} />
          <DetailRow label="Force Scan" value={String(movie.forceScan)} />
          <DetailRow label="Alternative Titles" value={movie.alternativeTitles.join(', ') || null} />
          <DetailRow label="Blacklist" value={movie.blackList.join(', ') || null} />
          <DetailRow label="Whitelist" value={movie.whiteList.join(', ') || null} />
          <DetailRow label="Created At" value={movie.createdAt} />
        </Paper>
      </Box>

      <Dialog open={editOpen} onClose={() => setEditOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>Edit Movie</DialogTitle>
        <DialogContent sx={{ display: 'flex', flexDirection: 'column', gap: 2, pt: 2 }}>
          {formError && <Alert severity="error">{formError}</Alert>}
          <TextField
            label="Original Title"
            value={form.originalTitle}
            onChange={(e) => setForm({ ...form, originalTitle: e.target.value })}
            required
            autoFocus
          />
          <TextField
            select
            label="Original Language"
            value={form.originalLocale}
            onChange={(e) => setForm({ ...form, originalLocale: e.target.value as Locale })}
          >
            {LOCALES.map((l) => (
              <MenuItem key={l.value} value={l.value}>{l.label}</MenuItem>
            ))}
          </TextField>
          <TextField
            label="Release Date"
            type="date"
            value={form.releaseDate}
            onChange={(e) => setForm({ ...form, releaseDate: e.target.value })}
            slotProps={{ inputLabel: { shrink: true } }}
          />
          <TextField
            label="Runtime (minutes)"
            type="number"
            value={form.runtimeMinutes}
            onChange={(e) => setForm({ ...form, runtimeMinutes: e.target.value })}
          />
          <TextField
            label="TMDB ID"
            type="number"
            value={form.tmdbId}
            onChange={(e) => setForm({ ...form, tmdbId: e.target.value })}
          />
          <TextField
            label="Alternative Titles (one per line)"
            multiline
            rows={3}
            value={form.alternativeTitles}
            onChange={(e) => setForm({ ...form, alternativeTitles: e.target.value })}
          />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setEditOpen(false)}>Cancel</Button>
          <Button
            variant="contained"
            onClick={handleUpdate}
            disabled={saving || !form.originalTitle.trim()}
          >
            {saving ? <CircularProgress size={20} /> : 'Save'}
          </Button>
        </DialogActions>
      </Dialog>
    </>
  );
}
