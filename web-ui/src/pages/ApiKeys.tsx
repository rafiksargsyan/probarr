import { useEffect, useState } from 'react';
import {
  Box, Typography, Button, TextField, Dialog, DialogTitle,
  DialogContent, DialogActions, Alert, CircularProgress,
  Table, TableBody, TableCell, TableHead, TableRow, Paper,
  IconButton, Tooltip, Chip, Stack,
} from '@mui/material';
import AddIcon from '@mui/icons-material/Add';
import DeleteIcon from '@mui/icons-material/Delete';
import ContentCopyIcon from '@mui/icons-material/ContentCopy';
import CheckIcon from '@mui/icons-material/Check';
import { useAuth } from '../hooks/useAuth';
import { listApiKeys, createApiKey, disableApiKey, enableApiKey, deleteApiKey } from '../api/apiKeys';
import type { ApiKeyDTO } from '../types/api.types';

function formatDate(iso: string | null) {
  if (!iso) return '—';
  return new Date(iso).toLocaleDateString(undefined, { year: 'numeric', month: 'short', day: 'numeric' });
}

export function ApiKeys() {
  const { user, userId } = useAuth();
  const [keys, setKeys] = useState<ApiKeyDTO[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [createOpen, setCreateOpen] = useState(false);
  const [description, setDescription] = useState('');
  const [creating, setCreating] = useState(false);
  const [newKey, setNewKey] = useState<ApiKeyDTO | null>(null);
  const [copied, setCopied] = useState(false);
  const [actionError, setActionError] = useState<string | null>(null);

  const load = () => {
    if (!user || !userId) return;
    setLoading(true);
    listApiKeys(userId, user)
      .then(setKeys)
      .catch((e: Error) => setError(e.message))
      .finally(() => setLoading(false));
  };

  useEffect(load, [user, userId]);

  const handleCreate = async () => {
    if (!user || !userId || !description.trim()) return;
    setCreating(true);
    setActionError(null);
    try {
      const created = await createApiKey(userId, description.trim(), user);
      setNewKey(created);
      setDescription('');
      setCreateOpen(false);
      load();
    } catch (e: unknown) {
      setActionError(e instanceof Error ? e.message : 'Failed to create key');
    } finally {
      setCreating(false);
    }
  };

  const handleToggle = async (key: ApiKeyDTO) => {
    if (!user || !userId) return;
    setActionError(null);
    try {
      if (key.disabled) {
        await enableApiKey(userId, key.id, user);
      } else {
        await disableApiKey(userId, key.id, user);
      }
      load();
    } catch (e: unknown) {
      setActionError(e instanceof Error ? e.message : 'Action failed');
    }
  };

  const handleDelete = async (key: ApiKeyDTO) => {
    if (!user || !userId) return;
    setActionError(null);
    try {
      await deleteApiKey(userId, key.id, user);
      load();
    } catch (e: unknown) {
      setActionError(e instanceof Error ? e.message : 'Delete failed');
    }
  };

  const handleCopy = async (text: string) => {
    try {
      await navigator.clipboard.writeText(text);
    } catch {
      const el = document.createElement('textarea');
      el.value = text;
      document.body.appendChild(el);
      el.select();
      document.execCommand('copy');
      document.body.removeChild(el);
    }
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  return (
    <Box>
      <Stack direction="row" justifyContent="space-between" alignItems="center" sx={{ mb: 3 }}>
        <Box>
          <Typography variant="h5" fontWeight="bold">API Keys</Typography>
          <Typography variant="body2" color="text.secondary">
            Use these keys to authenticate API requests. Maximum 2 keys per account.
          </Typography>
        </Box>
        <Button
          variant="contained"
          startIcon={<AddIcon />}
          onClick={() => { setCreateOpen(true); setActionError(null); }}
          disabled={keys.length >= 2}
        >
          New Key
        </Button>
      </Stack>

      {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}
      {actionError && <Alert severity="error" sx={{ mb: 2 }} onClose={() => setActionError(null)}>{actionError}</Alert>}

      {newKey?.key && (
        <Alert
          severity="success"
          sx={{ mb: 2 }}
          onClose={() => setNewKey(null)}
          action={
            <Tooltip title={copied ? 'Copied!' : 'Copy key'}>
              <IconButton size="small" color="inherit" onClick={() => handleCopy(newKey.key!)}>
                {copied ? <CheckIcon fontSize="small" /> : <ContentCopyIcon fontSize="small" />}
              </IconButton>
            </Tooltip>
          }
        >
          <Typography variant="body2" fontWeight="bold" gutterBottom>
            Save your API key — it will not be shown again.
          </Typography>
          <Typography variant="body2" color="text.secondary" gutterBottom>
            Key ID: <span style={{ fontFamily: 'monospace' }}>{newKey.id}</span>
          </Typography>
          <Typography
            variant="body2"
            sx={{ fontFamily: 'monospace', wordBreak: 'break-all' }}
          >
            {newKey.key}
          </Typography>
        </Alert>
      )}

      {loading ? (
        <Box sx={{ display: 'flex', justifyContent: 'center', mt: 6 }}>
          <CircularProgress />
        </Box>
      ) : (
        <Paper variant="outlined">
          <Table>
            <TableHead>
              <TableRow>
                <TableCell>Description</TableCell>
                <TableCell>Key ID</TableCell>
                <TableCell>Status</TableCell>
                <TableCell>Last used</TableCell>
                <TableCell align="right">Actions</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {keys.length === 0 ? (
                <TableRow>
                  <TableCell colSpan={6} align="center">
                    <Typography color="text.secondary" sx={{ py: 2 }}>
                      No API keys yet.
                    </Typography>
                  </TableCell>
                </TableRow>
              ) : (
                keys.map((k) => (
                  <TableRow key={k.id}>
                    <TableCell>{k.description}</TableCell>
                    <TableCell>
                      <Stack direction="row" alignItems="center" spacing={0.5}>
                        <Typography variant="body2" sx={{ fontFamily: 'monospace', fontSize: '0.75rem' }}>
                          {k.id}
                        </Typography>
                        <Tooltip title="Copy ID">
                          <IconButton size="small" onClick={() => handleCopy(k.id)}>
                            <ContentCopyIcon sx={{ fontSize: 14 }} />
                          </IconButton>
                        </Tooltip>
                      </Stack>
                    </TableCell>
                    <TableCell>
                      <Chip
                        label={k.disabled ? 'Disabled' : 'Active'}
                        color={k.disabled ? 'default' : 'success'}
                        size="small"
                      />
                    </TableCell>
                    <TableCell>{formatDate(k.lastAccessTime)}</TableCell>
                    <TableCell>—</TableCell>
                    <TableCell align="right">
                      <Tooltip title={k.disabled ? 'Enable' : 'Disable'}>
                        <Button size="small" onClick={() => handleToggle(k)}>
                          {k.disabled ? 'Enable' : 'Disable'}
                        </Button>
                      </Tooltip>
                      <Tooltip title={k.disabled ? 'Delete' : 'Disable the key first to delete it'}>
                        <span>
                          <IconButton
                            size="small"
                            color="error"
                            disabled={!k.disabled}
                            onClick={() => handleDelete(k)}
                          >
                            <DeleteIcon fontSize="small" />
                          </IconButton>
                        </span>
                      </Tooltip>
                    </TableCell>
                  </TableRow>
                ))
              )}
            </TableBody>
          </Table>
        </Paper>
      )}

      <Dialog open={createOpen} onClose={() => setCreateOpen(false)} maxWidth="xs" fullWidth>
        <DialogTitle>New API Key</DialogTitle>
        <DialogContent>
          <TextField
            autoFocus
            label="Description"
            fullWidth
            margin="dense"
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            inputProps={{ maxLength: 127 }}
            helperText="e.g. Q62 server, Home automation"
          />
          {actionError && <Alert severity="error" sx={{ mt: 1 }}>{actionError}</Alert>}
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setCreateOpen(false)}>Cancel</Button>
          <Button
            variant="contained"
            onClick={handleCreate}
            disabled={!description.trim() || creating}
          >
            {creating ? <CircularProgress size={18} /> : 'Create'}
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
}
