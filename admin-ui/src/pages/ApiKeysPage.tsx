import { Fragment, useCallback, useEffect, useState } from 'react';
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
  IconButton,
  Paper,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableRow,
  TextField,
  Tooltip,
  Typography,
} from '@mui/material';
import AddIcon from '@mui/icons-material/Add';
import ContentCopyIcon from '@mui/icons-material/ContentCopy';
import { useAuth } from '../hooks/useAuth';
import {
  signup,
  listAdminApiKeys,
  createAdminApiKey,
  disableAdminApiKey,
  enableAdminApiKey,
  deleteAdminApiKey,
} from '../api/adminApiKeys';
import type { AdminApiKey } from '../types';

const MAX_ACTIVE_KEYS = 2;

function formatDate(iso: string | null): string {
  if (!iso) return 'Never';
  return new Date(iso).toLocaleString();
}

export function ApiKeysPage() {
  const { user } = useAuth();
  const [keys, setKeys] = useState<AdminApiKey[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const [createOpen, setCreateOpen] = useState(false);
  const [createDescription, setCreateDescription] = useState('');
  const [creating, setCreating] = useState(false);
  const [createError, setCreateError] = useState<string | null>(null);

  const [revealedKey, setRevealedKey] = useState<string | null>(null);
  const [revealedKeyId, setRevealedKeyId] = useState<string | null>(null);
  const [copied, setCopied] = useState(false);

  const [actionError, setActionError] = useState<{ keyId: string; message: string } | null>(null);
  const [actionInProgress, setActionInProgress] = useState<string | null>(null);

  const [deleteTarget, setDeleteTarget] = useState<AdminApiKey | null>(null);
  const [deleting, setDeleting] = useState(false);
  const [deleteError, setDeleteError] = useState<string | null>(null);

  const load = useCallback(() => {
    if (!user) return;
    setLoading(true);
    listAdminApiKeys(user)
      .then((data) => setKeys(data.sort((a, b) => a.createdAt.localeCompare(b.createdAt))))
      .catch((e: Error) => setError(e.message))
      .finally(() => setLoading(false));
  }, [user]);

  useEffect(() => {
    if (!user) return;
    signup(user)
      .then(() => load())
      .catch((e: Error) => {
        setError(e.message);
        setLoading(false);
      });
  }, [user, load]);

  const activeCount = keys.filter((k) => !k.disabled).length;
  const atLimit = activeCount >= MAX_ACTIVE_KEYS;

  async function handleCreate() {
    if (!user) return;
    setCreating(true);
    setCreateError(null);
    try {
      const created = await createAdminApiKey(user, createDescription.trim());
      setCreateOpen(false);
      setCreateDescription('');
      setRevealedKey(created.key);
      setRevealedKeyId(created.id);
      setCopied(false);
      load();
    } catch (e: unknown) {
      setCreateError(e instanceof Error ? e.message : 'Failed to create');
    } finally {
      setCreating(false);
    }
  }

  async function handleDisable(keyId: string) {
    if (!user) return;
    setActionInProgress(keyId);
    setActionError(null);
    try {
      await disableAdminApiKey(user, keyId);
      load();
    } catch (e: unknown) {
      setActionError({ keyId, message: e instanceof Error ? e.message : 'Failed to disable' });
    } finally {
      setActionInProgress(null);
    }
  }

  async function handleEnable(keyId: string) {
    if (!user) return;
    setActionInProgress(keyId);
    setActionError(null);
    try {
      await enableAdminApiKey(user, keyId);
      load();
    } catch (e: unknown) {
      setActionError({ keyId, message: e instanceof Error ? e.message : 'Failed to enable' });
    } finally {
      setActionInProgress(null);
    }
  }

  async function handleDelete() {
    if (!user || !deleteTarget) return;
    setDeleting(true);
    setDeleteError(null);
    try {
      await deleteAdminApiKey(user, deleteTarget.id);
      setDeleteTarget(null);
      load();
    } catch (e: unknown) {
      setDeleteError(e instanceof Error ? e.message : 'Failed to delete');
    } finally {
      setDeleting(false);
    }
  }

  function handleCopy() {
    if (!revealedKey) return;
    navigator.clipboard.writeText(revealedKey).then(() => setCopied(true));
  }

  return (
    <>
      <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', mb: 3 }}>
        <Typography variant="h5" fontWeight="bold">API Keys</Typography>
        <Tooltip title={atLimit ? 'Maximum of 2 active keys reached. Disable a key to create a new one.' : ''}>
          <span>
            <Button
              variant="contained"
              startIcon={<AddIcon />}
              onClick={() => { setCreateError(null); setCreateDescription(''); setCreateOpen(true); }}
              disabled={atLimit}
            >
              Create API Key
            </Button>
          </span>
        </Tooltip>
      </Box>

      {atLimit && (
        <Alert severity="info" sx={{ mb: 2 }}>
          You have reached the maximum of {MAX_ACTIVE_KEYS} active API keys. Disable a key before creating a new one.
        </Alert>
      )}

      {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}

      <Paper>
        {loading ? (
          <Box sx={{ display: 'flex', justifyContent: 'center', p: 4 }}>
            <CircularProgress />
          </Box>
        ) : (
          <Table size="small">
            <TableHead>
              <TableRow>
                <TableCell>ID</TableCell>
                <TableCell>Description</TableCell>
                <TableCell>Status</TableCell>
                <TableCell>Last Used</TableCell>
                <TableCell>Created At</TableCell>
                <TableCell />
              </TableRow>
            </TableHead>
            <TableBody>
              {keys.length === 0 ? (
                <TableRow>
                  <TableCell colSpan={6} align="center" sx={{ color: 'text.secondary', py: 4 }}>
                    No API keys yet
                  </TableCell>
                </TableRow>
              ) : (
                keys.map((apiKey) => (
                  <Fragment key={apiKey.id}>
                    <TableRow hover>
                      <TableCell sx={{ fontFamily: 'monospace', fontSize: 12 }}>{apiKey.id}</TableCell>
                      <TableCell sx={{ color: apiKey.description ? 'text.primary' : 'text.secondary' }}>
                        {apiKey.description ?? '—'}
                      </TableCell>
                      <TableCell>
                        <Chip
                          label={apiKey.disabled ? 'Disabled' : 'Active'}
                          size="small"
                          color={apiKey.disabled ? 'default' : 'success'}
                          variant="outlined"
                        />
                      </TableCell>
                      <TableCell>{formatDate(apiKey.lastAccessTime)}</TableCell>
                      <TableCell>{formatDate(apiKey.createdAt)}</TableCell>
                      <TableCell align="right" sx={{ whiteSpace: 'nowrap' }}>
                        {actionInProgress === apiKey.id ? (
                          <CircularProgress size={20} />
                        ) : apiKey.disabled ? (
                          <>
                            <Button
                              size="small"
                              onClick={() => handleEnable(apiKey.id)}
                              sx={{ mr: 1 }}
                            >
                              Enable
                            </Button>
                            <Button
                              size="small"
                              color="error"
                              onClick={() => { setDeleteError(null); setDeleteTarget(apiKey); }}
                            >
                              Delete
                            </Button>
                          </>
                        ) : (
                          <Button size="small" onClick={() => handleDisable(apiKey.id)}>
                            Disable
                          </Button>
                        )}
                      </TableCell>
                    </TableRow>
                    {actionError?.keyId === apiKey.id && (
                      <TableRow>
                        <TableCell colSpan={6} sx={{ p: 0, border: 0 }}>
                          <Alert severity="error" onClose={() => setActionError(null)} sx={{ borderRadius: 0 }}>
                            {actionError.message}
                          </Alert>
                        </TableCell>
                      </TableRow>
                    )}
                  </Fragment>
                ))
              )}
            </TableBody>
          </Table>
        )}
      </Paper>

      {/* Create dialog */}
      <Dialog open={createOpen} onClose={() => setCreateOpen(false)} maxWidth="xs" fullWidth>
        <DialogTitle>Create API Key</DialogTitle>
        <DialogContent sx={{ display: 'flex', flexDirection: 'column', gap: 2, pt: 2 }}>
          {createError && <Alert severity="error">{createError}</Alert>}
          <TextField
            label="Description"
            value={createDescription}
            onChange={(e) => setCreateDescription(e.target.value)}
            fullWidth
            autoFocus
            helperText="Optional. Helps you identify this key later."
          />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setCreateOpen(false)}>Cancel</Button>
          <Button variant="contained" onClick={handleCreate} disabled={creating}>
            {creating ? <CircularProgress size={20} /> : 'Create'}
          </Button>
        </DialogActions>
      </Dialog>

      {/* Key reveal dialog */}
      <Dialog open={revealedKey !== null} maxWidth="sm" fullWidth>
        <DialogTitle>API Key Created</DialogTitle>
        <DialogContent sx={{ display: 'flex', flexDirection: 'column', gap: 2, pt: 2 }}>
          <Alert severity="warning">
            This key will not be shown again. Copy it now and store it somewhere safe.
          </Alert>
          <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1 }}>
            <Typography variant="caption" color="text.secondary">Key ID</Typography>
            <Box sx={{ bgcolor: 'action.hover', borderRadius: 1, p: 1.5 }}>
              <Typography sx={{ fontFamily: 'monospace', fontSize: 13, userSelect: 'all' }}>
                {revealedKeyId}
              </Typography>
            </Box>
          </Box>
          <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1 }}>
            <Typography variant="caption" color="text.secondary">Secret Key</Typography>
            <Box
              sx={{
                display: 'flex',
                alignItems: 'center',
                gap: 1,
                bgcolor: 'action.hover',
                borderRadius: 1,
                p: 1.5,
              }}
            >
              <Typography
                sx={{
                  fontFamily: 'monospace',
                  fontSize: 13,
                  wordBreak: 'break-all',
                  flexGrow: 1,
                  userSelect: 'all',
                }}
              >
                {revealedKey}
              </Typography>
              <Tooltip title={copied ? 'Copied!' : 'Copy to clipboard'}>
                <IconButton size="small" onClick={handleCopy}>
                  <ContentCopyIcon fontSize="small" />
                </IconButton>
              </Tooltip>
            </Box>
          </Box>
        </DialogContent>
        <DialogActions>
          <Button
            variant="contained"
            onClick={() => { setRevealedKey(null); setRevealedKeyId(null); setCopied(false); }}
          >
            I've copied the key
          </Button>
        </DialogActions>
      </Dialog>

      {/* Delete dialog */}
      <Dialog open={deleteTarget !== null} onClose={() => setDeleteTarget(null)} maxWidth="xs" fullWidth>
        <DialogTitle>Delete API Key</DialogTitle>
        <DialogContent>
          {deleteError && <Alert severity="error" sx={{ mb: 2 }}>{deleteError}</Alert>}
          <Typography>
            Permanently delete this API key
            {deleteTarget?.description ? <> (<strong>{deleteTarget.description}</strong>)</> : ''}?
            This cannot be undone.
          </Typography>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setDeleteTarget(null)}>Cancel</Button>
          <Button variant="contained" color="error" onClick={handleDelete} disabled={deleting}>
            {deleting ? <CircularProgress size={20} /> : 'Delete'}
          </Button>
        </DialogActions>
      </Dialog>
    </>
  );
}
