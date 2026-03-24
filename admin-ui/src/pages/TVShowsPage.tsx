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
import type { TVShow } from '../types';

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
    imdbId: '',
    tvdbId: '',
    sonarrId: '',
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
    setForm({ originalTitle: '', imdbId: '', tvdbId: '', sonarrId: '' });
    setFormError(null);
  }

  async function handleCreate() {
    if (!user || !form.originalTitle.trim()) return;
    setSaving(true);
    setFormError(null);
    try {
      await createTVShow(user, {
        originalTitle: form.originalTitle.trim(),
        imdbId: form.imdbId.trim() || null,
        tvdbId: form.tvdbId ? parseInt(form.tvdbId) : null,
        sonarrId: form.sonarrId ? parseInt(form.sonarrId) : null,
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
                  <TableCell>IMDB ID</TableCell>
                  <TableCell>TVDB ID</TableCell>
                  <TableCell>Sonarr ID</TableCell>
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
                    <TableCell>{s.imdbId ?? '—'}</TableCell>
                    <TableCell>{s.tvdbId ?? '—'}</TableCell>
                    <TableCell>{s.sonarrId ?? '—'}</TableCell>
                  </TableRow>
                ))}
                {tvShows.length === 0 && (
                  <TableRow>
                    <TableCell colSpan={4} align="center" sx={{ color: 'text.secondary' }}>
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
          <TextField
            label="Sonarr ID"
            type="number"
            value={form.sonarrId}
            onChange={(e) => setForm({ ...form, sonarrId: e.target.value })}
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
