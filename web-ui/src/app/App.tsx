import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider } from './contexts/AuthContext';
import { ProtectedRoute } from '../components/ProtectedRoute/ProtectedRoute';
import { EmailConfirmation } from '../components/EmailConfirmation/EmailConfirmation';
import { Layout } from '../components/Layout/Layout';
import { Login } from '../pages/Login';
import { Dashboard } from '../pages/Dashboard';

export default function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <EmailConfirmation />
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
          </Route>
          <Route path="*" element={<Navigate to="/login" replace />} />
        </Routes>
      </BrowserRouter>
    </AuthProvider>
  );
}
