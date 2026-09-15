import React, { useEffect, useMemo, useState } from 'react';
import { motion } from 'motion/react';
import { ArrowRight, CheckCircle2, Eye, EyeOff, LockKeyhole, Mail, UserRound } from 'lucide-react';

export interface AuthUser {
  id: string;
  email: string;
  name?: string;
  avatarUrl?: string;
}

interface AuthScreenProps {
  onAuthenticated: (user: AuthUser, session: AuthSession) => void;
  onSkip: () => void;
  onCancel?: () => void;
}

export interface AuthSession {
  access_token: string;
  refresh_token: string;
  expires_at?: number;
}

const SUPABASE_URL = String(import.meta.env.VITE_SUPABASE_URL || '').replace(/\/$/, '');
const SUPABASE_ANON_KEY = String(import.meta.env.VITE_SUPABASE_ANON_KEY || '');
const REDIRECT_URL = String(import.meta.env.VITE_AUTH_REDIRECT_URL || window.location.origin);

function configured() { return Boolean(SUPABASE_URL && SUPABASE_ANON_KEY); }

async function supabase(path: string, init: RequestInit = {}) {
  const res = await fetch(`${SUPABASE_URL}${path}`, {
    ...init,
    headers: { apikey: SUPABASE_ANON_KEY, 'Content-Type': 'application/json', ...(init.headers || {}) },
  });
  const data = await res.json().catch(() => ({}));
  if (!res.ok) throw new Error(typeof data?.msg === 'string' ? data.msg : typeof data?.error_description === 'string' ? data.error_description : typeof data?.message === 'string' ? data.message : 'Authentication request failed.');
  return data;
}

function userFromSession(session: any): AuthUser {
  const u = session?.user || {};
  return { id: String(u.id || ''), email: String(u.email || ''), name: u.user_metadata?.full_name || u.user_metadata?.name || u.email?.split('@')[0], avatarUrl: u.user_metadata?.avatar_url || u.user_metadata?.picture };
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

  useEffect(() => {
    const hash = new URLSearchParams(window.location.hash.replace(/^#/, ''));
    const access = hash.get('access_token');
    const refresh = hash.get('refresh_token');
    if (!access || !refresh || !configured()) return;
    setBusy(true);
    supabase('/auth/v1/user', { headers: { Authorization: `Bearer ${access}` } })
      .then(user => {
        const session = { access_token: access, refresh_token: refresh, expires_at: Number(hash.get('expires_at') || 0) || undefined };
        localStorage.setItem('neo-gpt-auth-session', JSON.stringify(session));
        window.history.replaceState({}, document.title, window.location.pathname + window.location.search);
        onAuthenticated(userFromSession({ user }), session);
      })
      .catch(() => setError('Google sign-in could not be completed. Please try again.'))
      .finally(() => setBusy(false));
  }, []);

  const title = useMemo(() => mode === 'signup' ? 'Create your Neo Gpt account' : mode === 'forgot' ? 'Reset your password' : 'Welcome to Neo Gpt', [mode]);
  const subtitle = useMemo(() => mode === 'signup' ? 'Create a secure account to keep your chats and preferences with you.' : mode === 'forgot' ? 'Enter your email and we will send a password reset link.' : 'Sign in to continue to your personal AI workspace.', [mode]);

  const submit = async (event: React.FormEvent) => {
    event.preventDefault(); setError(''); setNotice('');
    if (!configured()) { setError('Authentication is not configured. Add VITE_SUPABASE_URL and VITE_SUPABASE_ANON_KEY to the app environment.'); return; }
    if (!email.trim()) { setError('Enter your email address.'); return; }
    if (mode !== 'forgot' && password.length < 6) { setError('Password must be at least 6 characters.'); return; }
    if (mode === 'signup' && password !== confirm) { setError('Passwords do not match.'); return; }
    setBusy(true);
    try {
      if (mode === 'forgot') {
        await supabase('/auth/v1/recover', { method: 'POST', body: JSON.stringify({ email: email.trim(), redirect_to: REDIRECT_URL }) });
        setNotice('Password reset instructions have been sent to your email.');
      } else {
        const data = await supabase(mode === 'signup' ? '/auth/v1/signup' : '/auth/v1/token?grant_type=password', { method: 'POST', body: JSON.stringify({ email: email.trim(), password }) });
        if (mode === 'signup' && !data.access_token) setNotice('Account created. Check your email if confirmation is required, then sign in.');
        else if (data.access_token) {
          const session: AuthSession = { access_token: data.access_token, refresh_token: data.refresh_token, expires_at: data.expires_at ? Date.now() / 1000 + Number(data.expires_in || 3600) : undefined };
          localStorage.setItem('neo-gpt-auth-session', JSON.stringify(session));
          onAuthenticated(userFromSession(data), session);
        }
      }
    } catch (err) { setError(err instanceof Error ? err.message : 'Could not complete authentication.'); }
    finally { setBusy(false); }
  };

  const google = () => {
    setError('');
    if (!configured()) { setError('Authentication is not configured. Add the Supabase environment values first.'); return; }
    window.location.assign(`${SUPABASE_URL}/auth/v1/authorize?provider=google&redirect_to=${encodeURIComponent(REDIRECT_URL)}`);
  };

  return (
    <main className="neo-auth-screen">
      <motion.div initial={{ opacity: 0, y: 10 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: 0.48, ease: [0.22, 1, 0.36, 1] }} className="neo-auth-card">
        <div className="neo-auth-brand">
          <div className="neo-auth-logo" aria-hidden="true">N</div>
          <div><div className="neo-auth-brand-name">Neo Gpt</div><div className="neo-auth-brand-sub">Your AI workspace</div></div>
        </div>
        <div className="neo-auth-heading"><h1>{title}</h1><p>{subtitle}</p></div>
        <button type="button" disabled={busy} onClick={google} className="neo-auth-google"><span className="neo-google-mark">G</span><span>Continue with Google</span></button>
        <div className="neo-auth-divider"><span>or continue with email</span></div>
        <form onSubmit={submit} className="neo-auth-form">
          <label className="neo-auth-field"><Mail size={18}/><input value={email} onChange={e => setEmail(e.target.value)} type="email" autoComplete="email" placeholder="Email address" disabled={busy}/></label>
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
        <p className="neo-auth-terms">By continuing, you agree to use Neo Gpt responsibly and comply with your authentication provider's terms.</p>
      </motion.div>
    </main>
  );
}
