import { useState } from 'react';
import {
  Box,
  Card,
  CardContent,
  Typography,
  Button,
  TextField,
  Divider,
  Alert,
  CircularProgress,
} from '@mui/material';
import GoogleIcon from '@mui/icons-material/Google';
import EmailIcon from '@mui/icons-material/Email';
import { Navigate } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';

export function Login() {
  const { user, signInWithGoogle, sendMagicLink } = useAuth();
  const [email, setEmail] = useState('');
  const [emailSent, setEmailSent] = useState(false);
  const [error, setError] = useState('');
  const [loadingGoogle, setLoadingGoogle] = useState(false);
  const [loadingEmail, setLoadingEmail] = useState(false);

  const anyLoading = loadingGoogle || loadingEmail;

  if (user) return <Navigate to="/dashboard" replace />;

  const handleGoogle = async () => {
    setError('');
    setLoadingGoogle(true);
    try {
      await signInWithGoogle();
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : 'Google sign-in failed');
      setLoadingGoogle(false);
    }
  };

  const handleSendLink = async () => {
    if (!email) {
      setError('Please enter your email address');
      return;
    }
    setError('');
    setLoadingEmail(true);
    try {
      await sendMagicLink(email);
      setEmailSent(true);
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : 'Failed to send magic link');
    } finally {
      setLoadingEmail(false);
    }
  };

  return (
    <Box sx={{ minHeight: '100vh', display: 'flex', alignItems: 'center', justifyContent: 'center', bgcolor: 'grey.50' }}>
      <Card sx={{ width: '100%', maxWidth: 400, mx: 2 }} elevation={2}>
        <CardContent sx={{ p: 4 }}>
          <Box sx={{ textAlign: 'center', mb: 4 }}>
            <Typography variant="h4" fontWeight="bold" color="primary">
              Probarr
            </Typography>
            <Typography variant="body2" color="text.secondary" sx={{ mt: 0.5 }}>
              Admin Panel
            </Typography>
          </Box>

          {error && (
            <Alert severity="error" sx={{ mb: 2 }}>
              {error}
            </Alert>
          )}

          <Button
            fullWidth
            variant="contained"
            size="large"
            disableElevation
            startIcon={loadingGoogle ? <CircularProgress size={18} color="inherit" /> : <GoogleIcon />}
            onClick={handleGoogle}
            disabled={anyLoading}
            sx={{
              mb: 2,
              bgcolor: '#db4437',
              '&:hover': { bgcolor: '#c53929' },
              '&.Mui-disabled': { bgcolor: '#db4437', opacity: 0.5 },
            }}
          >
            Continue with Google
          </Button>

          <Divider sx={{ my: 2 }}>
            <Typography variant="body2" color="text.secondary">
              or continue with email
            </Typography>
          </Divider>

          {emailSent ? (
            <Alert severity="success">
              Magic link sent to <strong>{email}</strong>. Check your inbox!
            </Alert>
          ) : (
            <>
              <TextField
                fullWidth
                label="Email address"
                type="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                onKeyDown={(e) => e.key === 'Enter' && handleSendLink()}
                disabled={anyLoading}
                sx={{ mb: 1.5 }}
              />
              <Button
                fullWidth
                variant="contained"
                size="large"
                disableElevation
                startIcon={loadingEmail ? <CircularProgress size={18} color="inherit" /> : <EmailIcon />}
                onClick={handleSendLink}
                disabled={anyLoading}
              >
                Send magic link
              </Button>
            </>
          )}
        </CardContent>
      </Card>
    </Box>
  );
}
