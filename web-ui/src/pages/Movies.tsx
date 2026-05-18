import { useEffect, useState } from 'react';
import {
  Box, Typography, Card, CardActionArea, CardContent, Grid,
  Pagination, Chip, CircularProgress, Alert,
} from '@mui/material';
import MovieIcon from '@mui/icons-material/Movie';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';
import { listMovies } from '../api/movies';
import type { ClientMovieDTO, PageDTO } from '../types/api.types';

export function Movies() {
  const { user } = useAuth();
  const navigate = useNavigate();
  const [page, setPage] = useState(0);
  const [data, setData] = useState<PageDTO<ClientMovieDTO> | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!user) return;
    setLoading(true);
    setError(null);
    listMovies(user, page)
      .then(setData)
      .catch((e: Error) => setError(e.message))
      .finally(() => setLoading(false));
  }, [user, page]);

  return (
    <Box>
      <Typography variant="h5" fontWeight="bold" gutterBottom>
        Movies
      </Typography>

      {loading && (
        <Box sx={{ display: 'flex', justifyContent: 'center', mt: 6 }}>
          <CircularProgress />
        </Box>
      )}

      {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}

      {!loading && data && (
        <>
          <Grid container spacing={2}>
            {data.content.map((movie) => (
              <Grid key={movie.id} size={{ xs: 12, sm: 6, md: 4 }}>
                <Card variant="outlined" sx={{ height: '100%' }}>
                  <CardActionArea
                    onClick={() => navigate(`/movies/${movie.id}`)}
                    sx={{ height: '100%', alignItems: 'flex-start', display: 'flex', flexDirection: 'column' }}
                  >
                    <CardContent sx={{ width: '100%' }}>
                      <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 1 }}>
                        <MovieIcon color="primary" fontSize="small" />
                        <Typography variant="subtitle1" fontWeight="bold" noWrap>
                          {movie.originalTitle}
                        </Typography>
                      </Box>
                      <Typography variant="body2" color="text.secondary" gutterBottom>
                        {movie.releaseDate ? movie.releaseDate.substring(0, 4) : '—'}
                        {movie.runtimeMinutes ? ` · ${movie.runtimeMinutes} min` : ''}
                      </Typography>
                      <Box sx={{ display: 'flex', gap: 0.5, flexWrap: 'wrap', mt: 1 }}>
                        {movie.releases.length === 0 ? (
                          <Chip label="No releases" size="small" variant="outlined" />
                        ) : (
                          movie.releases.map((r, i) => (
                            <Chip
                              key={i}
                              label={[r.resolution, r.ripType].filter(Boolean).join(' · ')}
                              size="small"
                              color="primary"
                              variant="outlined"
                            />
                          ))
                        )}
                      </Box>
                    </CardContent>
                  </CardActionArea>
                </Card>
              </Grid>
            ))}
          </Grid>

          {data.totalPages > 1 && (
            <Box sx={{ display: 'flex', justifyContent: 'center', mt: 4 }}>
              <Pagination
                count={data.totalPages}
                page={page + 1}
                onChange={(_, v) => setPage(v - 1)}
                color="primary"
              />
            </Box>
          )}

          {data.content.length === 0 && (
            <Typography color="text.secondary" sx={{ mt: 4, textAlign: 'center' }}>
              No movies found.
            </Typography>
          )}
        </>
      )}
    </Box>
  );
}
