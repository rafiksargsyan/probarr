import { useEffect, useState } from 'react';
import {
  Alert,
  Box,
  Button,
  Chip,
  CircularProgress,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  Divider,
  IconButton,
  MenuItem,
  Paper,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableRow,
  TextField,
  Typography,
} from '@mui/material';
import ArrowBackIcon from '@mui/icons-material/ArrowBack';
import EditIcon from '@mui/icons-material/Edit';
import AddIcon from '@mui/icons-material/Add';
import SearchIcon from '@mui/icons-material/Search';
import { useNavigate, useParams } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';
import {
  getMovie, updateMovie, triggerScan,
  addToBlackList, removeFromBlackList,
  addToWhiteList, removeFromWhiteList,
  addToCoolDown, removeFromCoolDown,
} from '../api/movies';
import type { Locale, Movie } from '../types';
import { LOCALES } from '../types';

function formatBytes(bytes: number | null): string {
  if (bytes == null) return '—';
  if (bytes >= 1e9) return `${(bytes / 1e9).toFixed(1)} GB`;
  if (bytes >= 1e6) return `${(bytes / 1e6).toFixed(0)} MB`;
  return `${bytes} B`;
}

function formatDate(value: string | null | undefined): string | null {
  if (!value) return null;
  return new Date(value).toLocaleString();
}

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

  const [blacklistInput, setBlacklistInput] = useState('');
  const [whitelistInput, setWhitelistInput] = useState('');
  const [cooldownInput, setCooldownInput] = useState('');
  const [listError, setListError] = useState<string | null>(null);
  const [scanError, setScanError] = useState<string | null>(null);
  const [selectedCandidate, setSelectedCandidate] = useState<object | null>(null);
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
      .then((m) => setMovie(m))
      .catch((e: Error) => setError(e.message))
      .finally(() => setLoading(false));
  }, [user, id]);

  useEffect(() => {
    if (!movie?.scanning) return;
    const STALE_MS = 10 * 60 * 1000;
    const isStale = movie.scanStartedAt && Date.now() - new Date(movie.scanStartedAt).getTime() > STALE_MS;
    if (isStale) return;
    const interval = setInterval(() => {
      if (!user || !id) return;
      getMovie(user, id).then((m) => setMovie(m)).catch(() => {});
    }, 3000);
    return () => clearInterval(interval);
  }, [movie?.scanning, movie?.scanStartedAt, user, id]);

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

  async function handleScan() {
    if (!user || !id) return;
    setScanError(null);
    try {
      setMovie(await triggerScan(user, id));
    } catch (e: unknown) {
      setScanError(e instanceof Error ? e.message : 'Scan failed');
    }
  }

  async function handleListAction(action: () => Promise<Movie>) {
    if (!user || !id) return;
    setListError(null);
    try {
      setMovie(await action());
    } catch (e: unknown) {
      setListError(e instanceof Error ? e.message : 'Operation failed');
    }
  }

  const STALE_MS = 10 * 60 * 1000;
  const isScanningActive = !!movie?.scanning &&
    !(movie.scanStartedAt && Date.now() - new Date(movie.scanStartedAt).getTime() > STALE_MS);

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
            <Button size="small" startIcon={isScanningActive ? <CircularProgress size={14} /> : <SearchIcon />} onClick={handleScan} disabled={isScanningActive} sx={{ mr: 1 }}>
              {isScanningActive ? 'Scanning…' : 'Scan'}
            </Button>
            <Button size="small" startIcon={<EditIcon />} onClick={openEdit}>
              Edit
            </Button>
          </Box>
          {scanError && <Alert severity="error" sx={{ mb: 1 }}>{scanError}</Alert>}
          <Divider sx={{ mb: 1 }} />
          <DetailRow label="ID" value={<Typography sx={{ fontSize: 13, fontFamily: 'monospace' }}>{movie.id}</Typography>} />
          <DetailRow label="Language" value={movie.originalLocale} />
          <DetailRow label="Release Date" value={movie.releaseDate} />
          <DetailRow label="Runtime" value={movie.runtimeMinutes != null ? `${movie.runtimeMinutes}m` : null} />
          <DetailRow label="TMDB ID" value={movie.tmdbId} />
          <DetailRow label="Last Scan" value={formatDate(movie.lastScanAt)} />
          <DetailRow label="Force Scan" value={String(movie.forceScan)} />
          <DetailRow label="Alternative Titles" value={movie.alternativeTitles?.join(', ') || null} />

          {listError && <Alert severity="error" sx={{ my: 1 }}>{listError}</Alert>}

          <Box sx={{ py: 1.5, borderBottom: '1px solid', borderColor: 'divider' }}>
            <Typography sx={{ color: 'text.secondary', fontSize: 14, mb: 0.5 }}>Blacklist</Typography>
            <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 0.5, mb: 1 }}>
              {movie.blackList?.map((e) => (
                <Chip
                  key={e.infoHash}
                  label={`${e.infoHash} (${e.reason})`}
                  size="small"
                  onDelete={() => handleListAction(() => removeFromBlackList(user!, id!, e.infoHash))}
                />
              ))}
            </Box>
            <Box sx={{ display: 'flex', gap: 1 }}>
              <TextField size="small" placeholder="info hash" value={blacklistInput} onChange={(e) => setBlacklistInput(e.target.value)} sx={{ flex: 1 }} />
              <IconButton size="small" disabled={!blacklistInput.trim()} onClick={() => { handleListAction(() => addToBlackList(user!, id!, blacklistInput.trim())); setBlacklistInput(''); }}>
                <AddIcon fontSize="small" />
              </IconButton>
            </Box>
          </Box>

          <Box sx={{ py: 1.5, borderBottom: '1px solid', borderColor: 'divider' }}>
            <Typography sx={{ color: 'text.secondary', fontSize: 14, mb: 0.5 }}>Whitelist</Typography>
            <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 0.5, mb: 1 }}>
              {movie.whiteList?.map((h) => (
                <Chip
                  key={h}
                  label={h}
                  size="small"
                  onDelete={() => handleListAction(() => removeFromWhiteList(user!, id!, h))}
                />
              ))}
            </Box>
            <Box sx={{ display: 'flex', gap: 1 }}>
              <TextField size="small" placeholder="info hash" value={whitelistInput} onChange={(e) => setWhitelistInput(e.target.value)} sx={{ flex: 1 }} />
              <IconButton size="small" disabled={!whitelistInput.trim()} onClick={() => { handleListAction(() => addToWhiteList(user!, id!, whitelistInput.trim())); setWhitelistInput(''); }}>
                <AddIcon fontSize="small" />
              </IconButton>
            </Box>
          </Box>

          <Box sx={{ py: 1.5, borderBottom: '1px solid', borderColor: 'divider' }}>
            <Typography sx={{ color: 'text.secondary', fontSize: 14, mb: 0.5 }}>Cooldown</Typography>
            <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 0.5, mb: 1 }}>
              {movie.coolDownList?.map((h) => (
                <Chip
                  key={h}
                  label={h}
                  size="small"
                  onDelete={() => handleListAction(() => removeFromCoolDown(user!, id!, h))}
                />
              ))}
            </Box>
            <Box sx={{ display: 'flex', gap: 1 }}>
              <TextField size="small" placeholder="info hash" value={cooldownInput} onChange={(e) => setCooldownInput(e.target.value)} sx={{ flex: 1 }} />
              <IconButton size="small" disabled={!cooldownInput.trim()} onClick={() => { handleListAction(() => addToCoolDown(user!, id!, cooldownInput.trim())); setCooldownInput(''); }}>
                <AddIcon fontSize="small" />
              </IconButton>
            </Box>
          </Box>

          <DetailRow label="Created At" value={formatDate(movie.createdAt)} />
        </Paper>
        <Paper sx={{ flex: '1 1 600px', p: 3 }}>
          <Typography variant="h6" fontWeight="bold" sx={{ mb: 1 }}>
            Release Candidates ({movie.releaseCandidates?.length ?? 0})
          </Typography>
          <Divider sx={{ mb: 1 }} />
          {!movie.releaseCandidates?.length ? (
            <Typography sx={{ color: 'text.secondary', fontSize: 14 }}>No candidates found.</Typography>
          ) : (
            <Table size="small">
              <TableHead>
                <TableRow>
                  <TableCell>Title</TableCell>
                  <TableCell>Info Hash</TableCell>
                  <TableCell>Tracker</TableCell>
                  <TableCell>Size</TableCell>
                  <TableCell>Seeders</TableCell>
                  <TableCell>Resolution</TableCell>
                  <TableCell>Rip</TableCell>
                  <TableCell>Edition</TableCell>
                  <TableCell>Languages</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {movie.releaseCandidates.map((rc) => (
                  <TableRow key={rc.infoHash} hover sx={{ cursor: 'pointer' }} onClick={() => setSelectedCandidate(rc)}>
                    <TableCell sx={{ fontSize: 12, maxWidth: 300, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{rc.title ?? '—'}</TableCell>
                    <TableCell>
                      <Typography sx={{ fontFamily: 'monospace', fontSize: 12 }}>
                        {rc.infoHash.slice(0, 8)}…
                      </Typography>
                    </TableCell>
                    <TableCell sx={{ fontSize: 12 }}>{rc.tracker}</TableCell>
                    <TableCell sx={{ fontSize: 12 }}>{formatBytes(rc.sizeInBytes)}</TableCell>
                    <TableCell sx={{ fontSize: 12 }}>{rc.seeders ?? '—'}</TableCell>
                    <TableCell sx={{ fontSize: 12 }}>{rc.resolution}</TableCell>
                    <TableCell sx={{ fontSize: 12 }}>{rc.ripType}</TableCell>
                    <TableCell sx={{ fontSize: 12 }}>{rc.edition ?? '—'}</TableCell>
                    <TableCell sx={{ fontSize: 12 }}>{rc.languages?.join(', ') || '—'}</TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          )}
        </Paper>
      </Box>

      <Dialog open={!!selectedCandidate} onClose={() => setSelectedCandidate(null)} maxWidth="md" fullWidth>
        <DialogTitle>Release Candidate</DialogTitle>
        <DialogContent>
          <Box component="pre" sx={{ fontSize: 12, fontFamily: 'monospace', whiteSpace: 'pre-wrap', wordBreak: 'break-all', m: 0 }}>
            {JSON.stringify(selectedCandidate, null, 2)}
          </Box>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setSelectedCandidate(null)}>Close</Button>
        </DialogActions>
      </Dialog>

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
