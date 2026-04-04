import { useEffect, useState } from 'react';
import {
  Alert,
  Box,
  Button,
  Checkbox,
  CircularProgress,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  FormControlLabel,
  MenuItem,
  Paper,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TablePagination,
  TableRow,
  TextField,
  Typography,
} from '@mui/material';
import AddIcon from '@mui/icons-material/Add';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';
import { createTVShow, listTVShows } from '../api/tvshows';
import type { Locale, TVShow } from '../types';
import { LOCALES } from '../types';

export function TVShowsPage() {
  const { user } = useAuth();
  const navigate = useNavigate();

  const [tvShows, setTVShows] = useState<TVShow[]>([]);
  const [totalElements, setTotalElements] = useState(0);
  const [page, setPage] = useState(0);
  const [rowsPerPage, setRowsPerPage] = useState(20);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const [dialogOpen, setDialogOpen] = useState(false);
  const [saving, setSaving] = useState(false);
  const [formError, setFormError] = useState<string | null>(null);
  const [form, setForm] = useState({
    originalTitle: '',
    originalLocale: 'EN_US' as Locale,
    tmdbId: '',
    imdbId: '',
    tvdbId: '',
    releaseDate: '',
    useTvdb: false,
  });

  useEffect(() => {
    if (!user) return;
    setLoading(true);
    listTVShows(user, page, rowsPerPage)
      .then((p) => {
        setTVShows(p.content);
        setTotalElements(p.totalElements);
      })
      .catch((e: Error) => setError(e.message))
      .finally(() => setLoading(false));
  }, [user, page, rowsPerPage]);

  function resetForm() {
    setForm({
      originalTitle: '',
      originalLocale: 'EN_US',
      tmdbId: '',
      imdbId: '',
      tvdbId: '',
      releaseDate: '',
      useTvdb: false,
    });
    setFormError(null);
  }

  async function handleCreate() {
    if (!user || !form.originalTitle.trim()) return;
    setSaving(true);
    setFormError(null);
    try {
      await createTVShow(user, {
        originalTitle: form.originalTitle.trim(),
        originalLocale: form.originalLocale,
        tmdbId: form.tmdbId ? parseInt(form.tmdbId) : null,
        imdbId: form.imdbId.trim() || null,
        tvdbId: form.tvdbId ? parseInt(form.tvdbId) : null,
        releaseDate: form.releaseDate || null,
        useTvdb: form.useTvdb,
      });
      setDialogOpen(false);
      resetForm();
      setPage(0);
      const p = await listTVShows(user, 0, rowsPerPage);
      setTVShows(p.content);
      setTotalElements(p.totalElements);
    } catch (e: unknown) {
      setFormError(e instanceof Error ? e.message : 'Failed to create TV show');
    } finally {
      setSaving(false);
    }
  }

  return (
    <>
      <Box sx={{ display: 'flex', alignItems: 'center', mb: 3 }}>
        <Typography variant="h5" fontWeight="bold" sx={{ flexGrow: 1 }}>
          TV Shows
        </Typography>
        <Button variant="contained" startIcon={<AddIcon />} onClick={() => setDialogOpen(true)}>
          Add TV Show
        </Button>
      </Box>

      {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}

      {loading ? (
        <Box sx={{ display: 'flex', justifyContent: 'center', mt: 6 }}>
          <CircularProgress />
        </Box>
      ) : (
        <Paper>
          <TableContainer>
            <Table>
              <TableHead>
                <TableRow>
                  <TableCell>Title</TableCell>
                  <TableCell>Language</TableCell>
                  <TableCell>Release Date</TableCell>
                  <TableCell>TMDB ID</TableCell>
                  <TableCell>TVDB ID</TableCell>
                  <TableCell>Use TVDB</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {tvShows.map((s) => (
                  <TableRow
                    key={s.id}
                    hover
                    sx={{ cursor: 'pointer' }}
                    onClick={() => navigate(`/tvshows/${s.id}`)}
                  >
                    <TableCell>{s.originalTitle}</TableCell>
                    <TableCell>{s.originalLocale}</TableCell>
                    <TableCell>{s.releaseDate ?? '—'}</TableCell>
                    <TableCell>{s.tmdbId ?? '—'}</TableCell>
                    <TableCell>{s.tvdbId ?? '—'}</TableCell>
                    <TableCell>{s.useTvdb ? 'Yes' : 'No'}</TableCell>
                  </TableRow>
                ))}
                {tvShows.length === 0 && (
                  <TableRow>
                    <TableCell colSpan={6} align="center" sx={{ color: 'text.secondary' }}>
                      No TV shows yet
                    </TableCell>
                  </TableRow>
                )}
              </TableBody>
            </Table>
          </TableContainer>
          <TablePagination
            component="div"
            count={totalElements}
            page={page}
            rowsPerPage={rowsPerPage}
            rowsPerPageOptions={[10, 20, 50]}
            onPageChange={(_, p) => setPage(p)}
            onRowsPerPageChange={(e) => { setRowsPerPage(parseInt(e.target.value)); setPage(0); }}
          />
        </Paper>
      )}

      <Dialog open={dialogOpen} onClose={() => { setDialogOpen(false); resetForm(); }} maxWidth="sm" fullWidth>
        <DialogTitle>Add TV Show</DialogTitle>
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
            label="TMDB ID"
            type="number"
            value={form.tmdbId}
            onChange={(e) => setForm({ ...form, tmdbId: e.target.value })}
          />
          <TextField
            label="IMDB ID"
            value={form.imdbId}
            onChange={(e) => setForm({ ...form, imdbId: e.target.value })}
          />
          <TextField
            label="TVDB ID"
            type="number"
            value={form.tvdbId}
            onChange={(e) => setForm({ ...form, tvdbId: e.target.value })}
          />
          <FormControlLabel
            control={
              <Checkbox
                checked={form.useTvdb}
                onChange={(e) => setForm({ ...form, useTvdb: e.target.checked })}
              />
            }
            label="Use TVDB for season/episode data (anime)"
          />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => { setDialogOpen(false); resetForm(); }}>Cancel</Button>
          <Button
            variant="contained"
            onClick={handleCreate}
            disabled={saving || !form.originalTitle.trim()}
          >
            {saving ? <CircularProgress size={20} /> : 'Create'}
          </Button>
        </DialogActions>
      </Dialog>
    </>
  );
}
