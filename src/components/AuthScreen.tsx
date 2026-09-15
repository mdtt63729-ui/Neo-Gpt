import React, { useMemo, useState } from 'react';
import { ArrowRight, CheckCircle2, Eye, EyeOff, LockKeyhole, Mail, UserRound } from 'lucide-react';
import { configureFirebasePersistence, firebaseReady } from '../lib/firebase';
import { createUserWithEmailAndPassword, sendPasswordResetEmail, signInWithEmailAndPassword } from 'firebase/auth';

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
      const auth = await configureFirebasePersistence();
      if (mode === 'forgot') {
        await sendPasswordResetEmail(auth, email.trim());
        setNotice('Password reset instructions have been sent to your email.');
      } else {
        const result = mode === 'signup'
          ? await createUserWithEmailAndPassword(auth, email.trim(), password)
          : await signInWithEmailAndPassword(auth, email.trim(), password);
        const user = result.user;
        if (!user) throw new Error('Firebase did not return a user.');
        const session = await makeSession(user);
        localStorage.setItem('neo-gpt-auth-session', JSON.stringify(session));
        onAuthenticated(userFromFirebase(user), session);
      }
    } catch (err: any) { setError(firebaseMessage(err)); }
    finally { setBusy(false); }
  };

  return (
    <main className="neo-auth-screen">
      <section className="neo-auth-card" aria-label="Neo Gpt authentication">
        <div className="neo-auth-brand">
          <img className="neo-auth-logo" src="/neo-gpt-icon.svg" alt="Neo Gpt" />
          <div>
            <div className="neo-auth-brand-name">Neo Gpt</div>
            <div className="neo-auth-brand-sub">Your AI workspace</div>
          </div>
        </div>
        <div className="neo-auth-heading"><h1>{title}</h1><p>{subtitle}</p></div>
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
      </section>
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
    'auth/operation-not-allowed': 'This sign-in method is not enabled in Firebase Authentication.',
  };
  return messages[code] || String(err?.message || 'Could not complete authentication.');
}
