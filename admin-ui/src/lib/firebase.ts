import { initializeApp } from 'firebase/app';
import { getAuth } from 'firebase/auth';
import { env } from './env';

// Admin Firebase project (separate from end-user project)
const firebaseConfig = {
  apiKey: env.VITE_ADMIN_FIREBASE_API_KEY,
  authDomain: env.VITE_ADMIN_FIREBASE_AUTH_DOMAIN,
  projectId: env.VITE_ADMIN_FIREBASE_PROJECT_ID,
  storageBucket: env.VITE_ADMIN_FIREBASE_STORAGE_BUCKET,
  messagingSenderId: env.VITE_ADMIN_FIREBASE_MESSAGING_SENDER_ID,
  appId: env.VITE_ADMIN_FIREBASE_APP_ID,
};

const app = initializeApp(firebaseConfig, 'admin');
export const auth = getAuth(app);
export default app;
