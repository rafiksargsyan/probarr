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
  Paper,
  TextField,
  Typography,
} from '@mui/material';
import ArrowBackIcon from '@mui/icons-material/ArrowBack';
import EditIcon from '@mui/icons-material/Edit';
import { useNavigate, useParams } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';
import { getMovie, updateMovie } from '../api/movies';
import type { Movie } from '../types';

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
    year: '',
    imdbId: '',
    tmdbId: '',
    radarrId: '',
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
      year: movie.year != null ? String(movie.year) : '',
      imdbId: movie.imdbId ?? '',
      tmdbId: movie.tmdbId != null ? String(movie.tmdbId) : '',
      radarrId: movie.radarrId != null ? String(movie.radarrId) : '',
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
        year: form.year ? parseInt(form.year) : null,
        imdbId: form.imdbId.trim() || null,
        tmdbId: form.tmdbId ? parseInt(form.tmdbId) : null,
        radarrId: form.radarrId ? parseInt(form.radarrId) : null,
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
          <DetailRow label="Year" value={movie.year} />
          <DetailRow label="IMDB ID" value={movie.imdbId} />
          <DetailRow label="TMDB ID" value={movie.tmdbId} />
          <DetailRow label="Radarr ID" value={movie.radarrId} />
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
            label="Year"
            type="number"
            value={form.year}
            onChange={(e) => setForm({ ...form, year: e.target.value })}
          />
          <TextField
            label="IMDB ID"
            value={form.imdbId}
            onChange={(e) => setForm({ ...form, imdbId: e.target.value })}
          />
          <TextField
            label="TMDB ID"
            type="number"
            value={form.tmdbId}
            onChange={(e) => setForm({ ...form, tmdbId: e.target.value })}
          />
          <TextField
            label="Radarr ID"
            type="number"
            value={form.radarrId}
            onChange={(e) => setForm({ ...form, radarrId: e.target.value })}
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
