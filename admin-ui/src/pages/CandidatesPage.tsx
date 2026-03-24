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
import { createCandidate, listCandidates } from '../api/candidates';
import type { CandidateSource, ReleaseCandidate } from '../types';

const SOURCES: CandidateSource[] = ['RADARR', 'SONARR', 'MANUAL'];

function formatBytes(bytes: number | null): string {
  if (bytes == null) return '—';
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  if (bytes < 1024 * 1024 * 1024) return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
  return `${(bytes / (1024 * 1024 * 1024)).toFixed(2)} GB`;
}

export function CandidatesPage() {
  const { user } = useAuth();
  const navigate = useNavigate();

  const [candidates, setCandidates] = useState<ReleaseCandidate[]>([]);
  const [totalElements, setTotalElements] = useState(0);
  const [page, setPage] = useState(0);
  const [rowsPerPage, setRowsPerPage] = useState(20);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const [dialogOpen, setDialogOpen] = useState(false);
  const [saving, setSaving] = useState(false);
  const [formError, setFormError] = useState<string | null>(null);
  const [form, setForm] = useState({
    name: '',
    source: 'MANUAL' as CandidateSource,
    movieId: '',
    episodeId: '',
    tracker: '',
  });

  useEffect(() => {
    if (!user) return;
    setLoading(true);
    listCandidates(user, page, rowsPerPage)
      .then((p) => {
        setCandidates(p.content);
        setTotalElements(p.totalElements);
      })
      .catch((e: Error) => setError(e.message))
      .finally(() => setLoading(false));
  }, [user, page, rowsPerPage]);

  function resetForm() {
    setForm({ name: '', source: 'MANUAL', movieId: '', episodeId: '', tracker: '' });
    setFormError(null);
  }

  async function handleCreate() {
    if (!user || !form.name.trim()) return;
    setSaving(true);
    setFormError(null);
    try {
      await createCandidate(user, {
        name: form.name.trim(),
        source: form.source,
        movieId: form.movieId.trim() || null,
        episodeId: form.episodeId.trim() || null,
        tracker: form.tracker.trim() || null,
      });
      setDialogOpen(false);
      resetForm();
      setPage(0);
      const p = await listCandidates(user, 0, rowsPerPage);
      setCandidates(p.content);
      setTotalElements(p.totalElements);
    } catch (e: unknown) {
      setFormError(e instanceof Error ? e.message : 'Failed to create candidate');
    } finally {
      setSaving(false);
    }
  }

  return (
    <>
      <Box sx={{ display: 'flex', alignItems: 'center', mb: 3 }}>
        <Typography variant="h5" fontWeight="bold" sx={{ flexGrow: 1 }}>
          Candidates
        </Typography>
        <Button variant="contained" startIcon={<AddIcon />} onClick={() => setDialogOpen(true)}>
          Add Candidate
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
                  <TableCell>Name</TableCell>
                  <TableCell>Source</TableCell>
                  <TableCell>Status</TableCell>
                  <TableCell>Movie ID</TableCell>
                  <TableCell>Episode ID</TableCell>
                  <TableCell>Size</TableCell>
                  <TableCell>Tracker</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {candidates.map((c) => (
                  <TableRow
                    key={c.id}
                    hover
                    sx={{ cursor: 'pointer' }}
                    onClick={() => navigate(`/candidates/${c.id}`)}
                  >
                    <TableCell sx={{ maxWidth: 300, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                      {c.name}
                    </TableCell>
                    <TableCell>{c.source}</TableCell>
                    <TableCell>{c.status}</TableCell>
                    <TableCell>{c.movieId ?? '—'}</TableCell>
                    <TableCell>{c.episodeId ?? '—'}</TableCell>
                    <TableCell>{formatBytes(c.sizeBytes)}</TableCell>
                    <TableCell>{c.tracker ?? '—'}</TableCell>
                  </TableRow>
                ))}
                {candidates.length === 0 && (
                  <TableRow>
                    <TableCell colSpan={7} align="center" sx={{ color: 'text.secondary' }}>
                      No candidates yet
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
        <DialogTitle>Add Candidate</DialogTitle>
        <DialogContent sx={{ display: 'flex', flexDirection: 'column', gap: 2, pt: 2 }}>
          {formError && <Alert severity="error">{formError}</Alert>}
          <TextField
            label="Name"
            value={form.name}
            onChange={(e) => setForm({ ...form, name: e.target.value })}
            required
            autoFocus
          />
          <TextField
            select
            label="Source"
            value={form.source}
            onChange={(e) => setForm({ ...form, source: e.target.value as CandidateSource })}
          >
            {SOURCES.map((s) => (
              <MenuItem key={s} value={s}>{s}</MenuItem>
            ))}
          </TextField>
          <TextField
            label="Movie ID (optional)"
            value={form.movieId}
            onChange={(e) => setForm({ ...form, movieId: e.target.value })}
          />
          <TextField
            label="Episode ID (optional)"
            value={form.episodeId}
            onChange={(e) => setForm({ ...form, episodeId: e.target.value })}
          />
          <TextField
            label="Tracker (optional)"
            value={form.tracker}
            onChange={(e) => setForm({ ...form, tracker: e.target.value })}
          />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => { setDialogOpen(false); resetForm(); }}>Cancel</Button>
          <Button
            variant="contained"
            onClick={handleCreate}
            disabled={saving || !form.name.trim()}
          >
            {saving ? <CircularProgress size={20} /> : 'Create'}
          </Button>
        </DialogActions>
      </Dialog>
    </>
  );
}
