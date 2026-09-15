import React, { useEffect, useMemo, useState } from 'react';
import { motion, AnimatePresence } from 'motion/react';
import { ArrowRight, CheckCircle2, Eye, EyeOff, LockKeyhole, Mail, UserRound } from 'lucide-react';
import { firebaseAuth, firebaseReady } from '../lib/firebase';
import { nativeGoogleAvailable, signInWithNativeGoogle } from '../lib/nativeGoogleAuth';

export interface AuthUser {
  id: string;
  email: string;
  name?: string;
  avatarUrl?: string;
}

export interface AuthSession {
  access_token: string;
  refresh_token: string;
  expires_at?: number;
}

function userFromFirebase(user: any): AuthUser {
  return {
    id: String(user?.uid || ''),
    email: String(user?.email || ''),
    name: user?.displayName || user?.email?.split('@')[0],
    avatarUrl: user?.photoURL || undefined,
  };
}

async function makeSession(user: any): Promise<AuthSession> {
  const token = await user.getIdToken();
  return { access_token: token, refresh_token: '', expires_at: Date.now() / 1000 + 3600 };
}

export function AuthScreen({ onAuthenticated, onSkip, onCancel }: AuthScreenProps) {
  const [mode, setMode] = useState<'login' | 'signup' | 'forgot'>('login');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [confirm, setConfirm] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');
  const [showPremiumIntro, setShowPremiumIntro] = useState(() => localStorage.getItem('neo-gpt-auth-intro-seen') !== '1');

  useEffect(() => {
    if (showPremiumIntro) {
      const timer = window.setTimeout(() => {
        localStorage.setItem('neo-gpt-auth-intro-seen', '1');
        setShowPremiumIntro(false);
      }, 1050);
      return () => window.clearTimeout(timer);
    }
  }, [showPremiumIntro]);

  useEffect(() => {
    if (!firebaseReady()) { setError('Firebase could not be loaded. Check your internet connection and try again.'); return; }
    const auth = firebaseAuth();
    auth.getRedirectResult().then(async result => {
      if (!result?.user) return;
      const session = await makeSession(result.user);
      localStorage.setItem('neo-gpt-auth-session', JSON.stringify(session));
      onAuthenticated(userFromFirebase(result.user), session);
    }).catch((err: any) => setError(firebaseMessage(err)));
  }, []);

  const title = useMemo(() => mode === 'signup' ? 'Create your Neo Gpt account' : mode === 'forgot' ? 'Reset your password' : 'Welcome to Neo Gpt', [mode]);
  const subtitle = useMemo(() => mode === 'signup' ? 'Create a secure Firebase account to keep your chats and preferences with you.' : mode === 'forgot' ? 'Enter your email and we will send a password reset link.' : 'Sign in to continue to your personal AI workspace.', [mode]);

  const submit = async (event: React.FormEvent) => {
    event.preventDefault(); setError(''); setNotice('');
    if (!firebaseReady()) { setError('Firebase is not available. Please reload the app.'); return; }
    if (!email.trim()) { setError('Enter your email address.'); return; }
    if (mode !== 'forgot' && password.length < 6) { setError('Password must be at least 6 characters.'); return; }
    if (mode === 'signup' && password !== confirm) { setError('Passwords do not match.'); return; }
    setBusy(true);
    try {
      const auth = firebaseAuth();
      if (mode === 'forgot') {
        await auth.sendPasswordResetEmail(email.trim());
        setNotice('Password reset instructions have been sent to your email.');
      } else {
        const result = mode === 'signup'
          ? await auth.createUserWithEmailAndPassword(email.trim(), password)
          : await auth.signInWithEmailAndPassword(email.trim(), password);
        const user = result.user;
        if (!user) throw new Error('Firebase did not return a user.');
        const session = await makeSession(user);
        localStorage.setItem('neo-gpt-auth-session', JSON.stringify(session));
        onAuthenticated(userFromFirebase(user), session);
      }
    } catch (err: any) { setError(firebaseMessage(err)); }
    finally { setBusy(false); }
  };

  const google = async () => {
    setError(''); setNotice('');
    setBusy(true);
    try {
      // Android/iOS: use the native Firebase Google flow. On Android this uses
      // Credential Manager / Google Play Services, so the account picker is
      // presented by the OS instead of inside the WebView.
      if (nativeGoogleAvailable()) {
        const result = await signInWithNativeGoogle();
        const user = result.user;
        const token = result.token || '';
        const session: AuthSession = { access_token: token, refresh_token: '', expires_at: Date.now() / 1000 + 3600 };
        localStorage.setItem('neo-gpt-auth-session', JSON.stringify(session));
        onAuthenticated(userFromFirebase(user), session);
        return;
      }

      if (!firebaseReady()) { throw new Error('Firebase is not available. Please reload the app.'); }
      const auth = firebaseAuth();
      const provider = new window.firebase.auth.GoogleAuthProvider();
      provider.setCustomParameters({ prompt: 'select_account' });
      await auth.signInWithRedirect(provider);
    } catch (err: any) { setError(firebaseMessage(err)); }
    finally { setBusy(false); }
  };

  return (
    <main className="neo-auth-screen">
      <AnimatePresence>
        {showPremiumIntro && (
          <motion.div
            className="neo-auth-premium-intro"
            initial={{ opacity: 1 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            transition={{ duration: 0.38, ease: [0.22, 1, 0.36, 1] }}
          >
            <motion.div
              className="neo-auth-premium-logo"
              initial={{ opacity: 0, scale: 0.72, y: 10 }}
              animate={{ opacity: 1, scale: 1, y: 0 }}
              transition={{ duration: 0.62, ease: [0.22, 1, 0.36, 1] }}
            >N</motion.div>
            <motion.div
              className="neo-auth-premium-name"
              initial={{ opacity: 0, y: 8 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: 0.18, duration: 0.48, ease: [0.22, 1, 0.36, 1] }}
            >Neo Gpt</motion.div>
          </motion.div>
        )}
      </AnimatePresence>
      <motion.div initial={{ opacity: 0, y: 16 }} animate={{ opacity: showPremiumIntro ? 0 : 1, y: showPremiumIntro ? 16 : 0 }} transition={{ delay: showPremiumIntro ? 0 : 0.05, duration: 0.55, ease: [0.22, 1, 0.36, 1] }} className="neo-auth-card">
        <div className="neo-auth-brand"><div className="neo-auth-logo" aria-hidden="true">N</div><div><div className="neo-auth-brand-name">Neo Gpt</div><div className="neo-auth-brand-sub">Your AI workspace</div></div></div>
        <div className="neo-auth-heading"><h1>{title}</h1><p>{subtitle}</p></div>
        <button type="button" disabled={busy} onClick={google} className="neo-auth-google"><span className="neo-google-mark">G</span><span>Continue with Google</span></button>
        <div className="neo-auth-divider"><span>or continue with email</span></div>
        <form onSubmit={submit} className="neo-auth-form">
          <label className="neo-auth-field"><Mail size={18}/><input value={email} onChange={e => setEmail(e.target.value)} type="email" autoComplete="email" inputMode="email" placeholder="Email address" disabled={busy}/></label>
          {mode !== 'forgot' && <label className="neo-auth-field"><LockKeyhole size={18}/><input value={password} onChange={e => setPassword(e.target.value)} type={showPassword ? 'text' : 'password'} autoComplete={mode === 'signup' ? 'new-password' : 'current-password'} placeholder="Password" disabled={busy}/><button type="button" onClick={() => setShowPassword(v => !v)} aria-label={showPassword ? 'Hide password' : 'Show password'}>{showPassword ? <EyeOff size={18}/> : <Eye size={18}/>}</button></label>}
          {mode === 'signup' && <label className="neo-auth-field"><UserRound size={18}/><input value={confirm} onChange={e => setConfirm(e.target.value)} type="password" autoComplete="new-password" placeholder="Confirm password" disabled={busy}/></label>}
          {error && <div className="neo-auth-error">{error}</div>}
          {notice && <div className="neo-auth-notice"><CheckCircle2 size={17}/>{notice}</div>}
          <button type="submit" disabled={busy} className="neo-auth-submit">{busy ? 'Please wait…' : mode === 'signup' ? 'Create account' : mode === 'forgot' ? 'Send reset link' : 'Sign in'}<ArrowRight size={18}/></button>
        </form>
        {onCancel && <button type="button" onClick={onCancel} disabled={busy} className="neo-auth-skip">Back to chat</button>}
        {!onCancel && <button type="button" onClick={onSkip} disabled={busy} className="neo-auth-skip">Continue as guest</button>}
        <div className="neo-auth-links">
          {mode === 'login' && <button type="button" onClick={() => setMode('forgot')}>Forgot password?</button>}
          {mode !== 'forgot' && <button type="button" onClick={() => setMode(mode === 'login' ? 'signup' : 'login')}>{mode === 'login' ? 'Create an account' : 'Already have an account? Sign in'}</button>}
          {mode === 'forgot' && <button type="button" onClick={() => setMode('login')}>Back to sign in</button>}
        </div>
        <p className="neo-auth-terms">Authentication is secured by Firebase.</p>
      </motion.div>
    </main>
  );
}

interface AuthScreenProps {
  onAuthenticated: (user: AuthUser, session: AuthSession) => void;
  onSkip: () => void;
  onCancel?: () => void;
}

function firebaseMessage(err: any) {
  const code = String(err?.code || '');
  const messages: Record<string, string> = {
    'auth/invalid-credential': 'Email or password is incorrect.',
    'auth/invalid-login-credentials': 'Email or password is incorrect.',
    'auth/user-not-found': 'No account was found with this email.',
    'auth/wrong-password': 'Email or password is incorrect.',
    'auth/email-already-in-use': 'An account already exists with this email.',
    'auth/weak-password': 'Password must be at least 6 characters.',
    'auth/invalid-email': 'Enter a valid email address.',
    'auth/too-many-requests': 'Too many attempts. Please wait and try again.',
    'auth/popup-blocked': 'Google sign-in was blocked. Please allow the sign-in window and try again.',
    'auth/operation-not-allowed': 'This sign-in method is not enabled in Firebase Authentication.',
  };
  return messages[code] || String(err?.message || 'Could not complete authentication.');
}
