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
  getEpisode, updateEpisode, triggerEpisodeScan,
  addToEpisodeBlackList, removeFromEpisodeBlackList,
  addToEpisodeWhiteList, removeFromEpisodeWhiteList,
  addToEpisodeCoolDown, removeFromEpisodeCoolDown,
} from '../api/tvshows';
import type { Episode } from '../types';

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

function formatRuntime(seconds: number | null): string | null {
  if (seconds == null) return null;
  const mins = Math.round(seconds / 60);
  return `${mins}m`;
}

function episodeLabel(ep: Episode): string {
  if (ep.seasonNumber != null && ep.episodeNumber != null) {
    return `S${String(ep.seasonNumber).padStart(2, '0')}E${String(ep.episodeNumber).padStart(2, '0')}`;
  }
  if (ep.absoluteNumber != null) return `#${ep.absoluteNumber}`;
  return 'Episode';
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

export function EpisodeDetailPage() {
  const { id: tvShowId, episodeId } = useParams<{ id: string; episodeId: string }>();
  const { user } = useAuth();
  const navigate = useNavigate();

  const [episode, setEpisode] = useState<Episode | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const [editOpen, setEditOpen] = useState(false);
  const [saving, setSaving] = useState(false);
  const [formError, setFormError] = useState<string | null>(null);
  const [form, setForm] = useState({
    seasonNumber: '',
    episodeNumber: '',
    absoluteNumber: '',
    airDate: '',
    runtimeSeconds: '',
  });

  const [blacklistInput, setBlacklistInput] = useState('');
  const [whitelistInput, setWhitelistInput] = useState('');
  const [cooldownInput, setCooldownInput] = useState('');
  const [listError, setListError] = useState<string | null>(null);
  const [scanError, setScanError] = useState<string | null>(null);
  const [selectedCandidate, setSelectedCandidate] = useState<object | null>(null);

  useEffect(() => {
    if (!user || !tvShowId || !episodeId) return;
    setLoading(true);
    getEpisode(user, tvShowId, episodeId)
      .then((e) => setEpisode(e))
      .catch((e: Error) => setError(e.message))
      .finally(() => setLoading(false));
  }, [user, tvShowId, episodeId]);

  useEffect(() => {
    if (!episode?.scanning) return;
    const STALE_MS = 10 * 60 * 1000;
    const isStale = episode.scanStartedAt && Date.now() - new Date(episode.scanStartedAt).getTime() > STALE_MS;
    if (isStale) return;
    const interval = setInterval(() => {
      if (!user || !tvShowId || !episodeId) return;
      getEpisode(user, tvShowId, episodeId).then((e) => setEpisode(e)).catch(() => {});
    }, 3000);
    return () => clearInterval(interval);
  }, [episode?.scanning, episode?.scanStartedAt, user, tvShowId, episodeId]);

  function openEdit() {
    if (!episode) return;
    setForm({
      seasonNumber: episode.seasonNumber != null ? String(episode.seasonNumber) : '',
      episodeNumber: episode.episodeNumber != null ? String(episode.episodeNumber) : '',
      absoluteNumber: episode.absoluteNumber != null ? String(episode.absoluteNumber) : '',
      airDate: episode.airDate ?? '',
      runtimeSeconds: episode.runtimeSeconds != null ? String(episode.runtimeSeconds) : '',
    });
    setFormError(null);
    setEditOpen(true);
  }

  async function handleUpdate() {
    if (!user || !tvShowId || !episodeId) return;
    setSaving(true);
    setFormError(null);
    try {
      const updated = await updateEpisode(user, tvShowId, episodeId, {
        seasonNumber: form.seasonNumber ? parseInt(form.seasonNumber) : null,
        episodeNumber: form.episodeNumber ? parseInt(form.episodeNumber) : null,
        absoluteNumber: form.absoluteNumber ? parseInt(form.absoluteNumber) : null,
        airDate: form.airDate || null,
        runtimeSeconds: form.runtimeSeconds ? parseInt(form.runtimeSeconds) : null,
      });
      setEpisode(updated);
      setEditOpen(false);
    } catch (e: unknown) {
      setFormError(e instanceof Error ? e.message : 'Failed to update episode');
    } finally {
      setSaving(false);
    }
  }

  async function handleScan() {
    if (!user || !tvShowId || !episodeId) return;
    setScanError(null);
    try {
      setEpisode(await triggerEpisodeScan(user, tvShowId, episodeId));
    } catch (e: unknown) {
      setScanError(e instanceof Error ? e.message : 'Scan failed');
    }
  }

  async function handleListAction(action: () => Promise<Episode>) {
    if (!user || !tvShowId || !episodeId) return;
    setListError(null);
    try {
      setEpisode(await action());
    } catch (e: unknown) {
      setListError(e instanceof Error ? e.message : 'Operation failed');
    }
  }

  const STALE_MS = 10 * 60 * 1000;
  const isScanningActive = !!episode?.scanning &&
    !(episode.scanStartedAt && Date.now() - new Date(episode.scanStartedAt).getTime() > STALE_MS);

  if (loading) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', mt: 6 }}>
        <CircularProgress />
      </Box>
    );
  }

  if (error || !episode) {
    return <Alert severity="error">{error ?? 'Episode not found'}</Alert>;
  }

  const label = episodeLabel(episode);

  return (
    <>
      <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 3 }}>
        <Button startIcon={<ArrowBackIcon />} onClick={() => navigate(`/tvshows/${tvShowId}`)}>
          TV Show
        </Button>
        <Typography color="text.secondary">/</Typography>
        <Typography fontWeight="medium">{label}</Typography>
      </Box>

      <Box sx={{ display: 'flex', gap: 3, flexWrap: 'wrap' }}>
        <Paper sx={{ flex: '1 1 400px', p: 3 }}>
          <Box sx={{ display: 'flex', alignItems: 'center', mb: 1 }}>
            <Typography variant="h6" fontWeight="bold" sx={{ flexGrow: 1 }}>
              {label}
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
          <DetailRow label="ID" value={<Typography sx={{ fontSize: 13, fontFamily: 'monospace' }}>{episode.id}</Typography>} />
          <DetailRow label="Season" value={episode.seasonNumber} />
          <DetailRow label="Episode" value={episode.episodeNumber} />
          <DetailRow label="Absolute #" value={episode.absoluteNumber} />
          <DetailRow label="Air Date" value={episode.airDate} />
          <DetailRow label="Runtime" value={formatRuntime(episode.runtimeSeconds)} />
          <DetailRow label="Last Scan" value={formatDate(episode.lastScanAt)} />

          {listError && <Alert severity="error" sx={{ my: 1 }}>{listError}</Alert>}

          <Box sx={{ py: 1.5, borderBottom: '1px solid', borderColor: 'divider' }}>
            <Typography sx={{ color: 'text.secondary', fontSize: 14, mb: 0.5 }}>Blacklist</Typography>
            <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 0.5, mb: 1 }}>
              {episode.blackList?.map((e) => (
                <Chip
                  key={e.infoHash}
                  label={`${e.infoHash} (${e.reason})`}
                  size="small"
                  onDelete={() => handleListAction(() => removeFromEpisodeBlackList(user!, tvShowId!, episodeId!, e.infoHash))}
                />
              ))}
            </Box>
            <Box sx={{ display: 'flex', gap: 1 }}>
              <TextField size="small" placeholder="info hash" value={blacklistInput} onChange={(e) => setBlacklistInput(e.target.value)} sx={{ flex: 1 }} />
              <IconButton size="small" disabled={!blacklistInput.trim()} onClick={() => { handleListAction(() => addToEpisodeBlackList(user!, tvShowId!, episodeId!, blacklistInput.trim())); setBlacklistInput(''); }}>
                <AddIcon fontSize="small" />
              </IconButton>
            </Box>
          </Box>

          <Box sx={{ py: 1.5, borderBottom: '1px solid', borderColor: 'divider' }}>
            <Typography sx={{ color: 'text.secondary', fontSize: 14, mb: 0.5 }}>Whitelist</Typography>
            <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 0.5, mb: 1 }}>
              {episode.whiteList?.map((h) => (
                <Chip
                  key={h}
                  label={h}
                  size="small"
                  onDelete={() => handleListAction(() => removeFromEpisodeWhiteList(user!, tvShowId!, episodeId!, h))}
                />
              ))}
            </Box>
            <Box sx={{ display: 'flex', gap: 1 }}>
              <TextField size="small" placeholder="info hash" value={whitelistInput} onChange={(e) => setWhitelistInput(e.target.value)} sx={{ flex: 1 }} />
              <IconButton size="small" disabled={!whitelistInput.trim()} onClick={() => { handleListAction(() => addToEpisodeWhiteList(user!, tvShowId!, episodeId!, whitelistInput.trim())); setWhitelistInput(''); }}>
                <AddIcon fontSize="small" />
              </IconButton>
            </Box>
          </Box>

          <Box sx={{ py: 1.5, borderBottom: '1px solid', borderColor: 'divider' }}>
            <Typography sx={{ color: 'text.secondary', fontSize: 14, mb: 0.5 }}>Cooldown</Typography>
            <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 0.5, mb: 1 }}>
              {episode.coolDownList?.map((h) => (
                <Chip
                  key={h}
                  label={h}
                  size="small"
                  onDelete={() => handleListAction(() => removeFromEpisodeCoolDown(user!, tvShowId!, episodeId!, h))}
                />
              ))}
            </Box>
            <Box sx={{ display: 'flex', gap: 1 }}>
              <TextField size="small" placeholder="info hash" value={cooldownInput} onChange={(e) => setCooldownInput(e.target.value)} sx={{ flex: 1 }} />
              <IconButton size="small" disabled={!cooldownInput.trim()} onClick={() => { handleListAction(() => addToEpisodeCoolDown(user!, tvShowId!, episodeId!, cooldownInput.trim())); setCooldownInput(''); }}>
                <AddIcon fontSize="small" />
              </IconButton>
            </Box>
          </Box>
        </Paper>

        <Paper sx={{ flex: '1 1 600px', p: 3 }}>
          <Typography variant="h6" fontWeight="bold" sx={{ mb: 1 }}>
            Release Candidates ({episode.releaseCandidates?.length ?? 0})
          </Typography>
          <Divider sx={{ mb: 1 }} />
          {!episode.releaseCandidates?.length ? (
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
                {episode.releaseCandidates.map((rc) => (
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
        <DialogTitle>Edit Episode</DialogTitle>
        <DialogContent sx={{ display: 'flex', flexDirection: 'column', gap: 2, pt: 2 }}>
          {formError && <Alert severity="error">{formError}</Alert>}
          <TextField
            label="Season Number"
            type="number"
            value={form.seasonNumber}
            onChange={(e) => setForm({ ...form, seasonNumber: e.target.value })}
          />
          <TextField
            label="Episode Number"
            type="number"
            value={form.episodeNumber}
            onChange={(e) => setForm({ ...form, episodeNumber: e.target.value })}
          />
          <TextField
            label="Absolute Number"
            type="number"
            value={form.absoluteNumber}
            onChange={(e) => setForm({ ...form, absoluteNumber: e.target.value })}
          />
          <TextField
            label="Air Date"
            type="date"
            value={form.airDate}
            onChange={(e) => setForm({ ...form, airDate: e.target.value })}
            slotProps={{ inputLabel: { shrink: true } }}
          />
          <TextField
            label="Runtime (seconds)"
            type="number"
            value={form.runtimeSeconds}
            onChange={(e) => setForm({ ...form, runtimeSeconds: e.target.value })}
          />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setEditOpen(false)}>Cancel</Button>
          <Button variant="contained" onClick={handleUpdate} disabled={saving}>
            {saving ? <CircularProgress size={20} /> : 'Save'}
          </Button>
        </DialogActions>
      </Dialog>
    </>
  );
}
