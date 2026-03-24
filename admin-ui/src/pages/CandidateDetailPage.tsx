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
import { getCandidate, getRelease } from '../api/candidates';
import type { Release, ReleaseCandidate } from '../types';

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

function formatBytes(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  if (bytes < 1024 * 1024 * 1024) return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
  return `${(bytes / (1024 * 1024 * 1024)).toFixed(2)} GB`;
}

export function CandidateDetailPage() {
  const { id } = useParams<{ id: string }>();
  const { user } = useAuth();
  const navigate = useNavigate();

  const [candidate, setCandidate] = useState<ReleaseCandidate | null>(null);
  const [release, setRelease] = useState<Release | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [releaseError, setReleaseError] = useState<string | null>(null);

  useEffect(() => {
    if (!user || !id) return;
    setLoading(true);
    getCandidate(user, id)
      .then((c) => {
        setCandidate(c);
        return getRelease(user, c.id).catch((e: Error) => {
          // Not found is expected when no release has been indexed yet
          if (e.message.includes('404') || e.message.includes('not found') || e.message.includes('Request failed: 404')) {
            return null;
          }
          setReleaseError(e.message);
          return null;
        });
      })
      .then((r) => {
        setRelease(r);
      })
      .catch((e: Error) => setError(e.message))
      .finally(() => setLoading(false));
  }, [user, id]);

  if (loading) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', mt: 6 }}>
        <CircularProgress />
      </Box>
    );
  }

  if (error || !candidate) {
    return <Alert severity="error">{error ?? 'Candidate not found'}</Alert>;
  }

  return (
    <>
      <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 3 }}>
        <Button startIcon={<ArrowBackIcon />} onClick={() => navigate('/candidates')}>
          Candidates
        </Button>
        <Typography color="text.secondary">/</Typography>
        <Typography fontWeight="medium" sx={{ maxWidth: 400, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
          {candidate.name}
        </Typography>
      </Box>

      <Box sx={{ display: 'flex', gap: 3, flexWrap: 'wrap', mb: 3 }}>
        <Paper sx={{ flex: '1 1 400px', p: 3 }}>
          <Typography variant="h6" fontWeight="bold" gutterBottom>
            Candidate
          </Typography>
          <Divider sx={{ mb: 1 }} />
          <DetailRow label="ID" value={<Typography sx={{ fontSize: 13, fontFamily: 'monospace' }}>{candidate.id}</Typography>} />
          <DetailRow label="Name" value={candidate.name} />
          <DetailRow label="Source" value={<Chip label={candidate.source} size="small" />} />
          <DetailRow label="Status" value={<Chip label={candidate.status} size="small" color={candidate.status === 'INDEXED' ? 'success' : candidate.status === 'FAILED' ? 'error' : 'default'} />} />
          <DetailRow label="Movie ID" value={candidate.movieId} />
          <DetailRow label="Episode ID" value={candidate.episodeId} />
          <DetailRow label="Info Hash" value={candidate.infoHash ? <Typography sx={{ fontSize: 13, fontFamily: 'monospace' }}>{candidate.infoHash}</Typography> : null} />
          <DetailRow label="Size" value={candidate.sizeBytes != null ? formatBytes(candidate.sizeBytes) : null} />
          <DetailRow label="Tracker" value={candidate.tracker} />
          <DetailRow label="Created At" value={candidate.createdAt} />
        </Paper>
      </Box>

      <Typography variant="h6" fontWeight="bold" gutterBottom>
        Release
      </Typography>

      {releaseError && <Alert severity="error" sx={{ mb: 2 }}>{releaseError}</Alert>}

      {release ? (
        <Paper sx={{ p: 3, mb: 3 }}>
          <DetailRow label="ID" value={<Typography sx={{ fontSize: 13, fontFamily: 'monospace' }}>{release.id}</Typography>} />
          <DetailRow label="File Path" value={release.filePath} />
          <DetailRow label="File Size" value={formatBytes(release.fileSizeBytes)} />
          <DetailRow label="Video Codec" value={release.videoCodec} />
          <DetailRow label="Resolution" value={release.resolution} />
          <DetailRow label="Rip Type" value={release.ripType} />
          <DetailRow label="Runtime" value={`${Math.floor(release.runtimeSeconds / 60)}m ${release.runtimeSeconds % 60}s`} />
          <DetailRow label="Created At" value={release.createdAt} />

          {release.audioTracks.length > 0 && (
            <>
              <Typography variant="subtitle2" fontWeight="bold" sx={{ mt: 2, mb: 1 }}>
                Audio Tracks
              </Typography>
              <TableContainer>
                <Table size="small">
                  <TableHead>
                    <TableRow>
                      <TableCell>Language</TableCell>
                      <TableCell>Codec</TableCell>
                      <TableCell>Channels</TableCell>
                      <TableCell>Default</TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {release.audioTracks.map((t, i) => (
                      <TableRow key={i}>
                        <TableCell>{t.language ?? '—'}</TableCell>
                        <TableCell>{t.codec}</TableCell>
                        <TableCell>{t.channels}</TableCell>
                        <TableCell>{t.isDefault ? 'Yes' : 'No'}</TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </TableContainer>
            </>
          )}

          {release.subtitleTracks.length > 0 && (
            <>
              <Typography variant="subtitle2" fontWeight="bold" sx={{ mt: 2, mb: 1 }}>
                Subtitle Tracks
              </Typography>
              <TableContainer>
                <Table size="small">
                  <TableHead>
                    <TableRow>
                      <TableCell>Language</TableCell>
                      <TableCell>Format</TableCell>
                      <TableCell>Default</TableCell>
                      <TableCell>Forced</TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {release.subtitleTracks.map((t, i) => (
                      <TableRow key={i}>
                        <TableCell>{t.language ?? '—'}</TableCell>
                        <TableCell>{t.format}</TableCell>
                        <TableCell>{t.isDefault ? 'Yes' : 'No'}</TableCell>
                        <TableCell>{t.isForced ? 'Yes' : 'No'}</TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </TableContainer>
            </>
          )}
        </Paper>
      ) : (
        !releaseError && (
          <Paper sx={{ p: 3 }}>
            <Typography color="text.secondary">No release indexed yet.</Typography>
          </Paper>
        )
      )}
    </>
  );
}
