import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider } from './contexts/AuthContext';
import { ProtectedRoute } from '../components/ProtectedRoute/ProtectedRoute';
import { Layout } from '../components/Layout/Layout';
import { Login } from '../pages/Login';
import { Dashboard } from '../pages/Dashboard';
import { MoviesPage } from '../pages/MoviesPage';
import { MovieDetailPage } from '../pages/MovieDetailPage';
import { TVShowsPage } from '../pages/TVShowsPage';
import { TVShowDetailPage } from '../pages/TVShowDetailPage';
import { SeasonDetailPage } from '../pages/SeasonDetailPage';
import { CandidatesPage } from '../pages/CandidatesPage';
import { CandidateDetailPage } from '../pages/CandidateDetailPage';
import { EmailConfirmation } from '../components/EmailConfirmation/EmailConfirmation';

export default function App() {
  return (
    <AuthProvider>
      <EmailConfirmation />
      <BrowserRouter>
        <Routes>
          <Route path="/login" element={<Login />} />
          <Route
            element={
              <ProtectedRoute>
                <Layout />
              </ProtectedRoute>
            }
          >
            <Route path="/" element={<Navigate to="/dashboard" replace />} />
            <Route path="/dashboard" element={<Dashboard />} />
            <Route path="/movies" element={<MoviesPage />} />
            <Route path="/movies/:id" element={<MovieDetailPage />} />
            <Route path="/tvshows" element={<TVShowsPage />} />
            <Route path="/tvshows/:id" element={<TVShowDetailPage />} />
            <Route path="/tvshows/:id/seasons/:seasonId" element={<SeasonDetailPage />} />
            <Route path="/candidates" element={<CandidatesPage />} />
            <Route path="/candidates/:id" element={<CandidateDetailPage />} />
          </Route>
          <Route path="*" element={<Navigate to="/login" replace />} />
        </Routes>
      </BrowserRouter>
    </AuthProvider>
  );
}
