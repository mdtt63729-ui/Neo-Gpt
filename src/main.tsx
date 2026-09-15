import {StrictMode} from 'react';
import {createRoot} from 'react-dom/client';
import App from './App.tsx';
import './index.css';
import { SplashScreen } from '@capacitor/splash-screen';

// Capacitor/WebView mobile zoom guard. Normal scrolling and text selection remain enabled.
if (typeof document !== 'undefined') {
  document.addEventListener('gesturestart', (e) => e.preventDefault(), { passive: false });
  document.addEventListener('gesturechange', (e) => e.preventDefault(), { passive: false });
  document.addEventListener('gestureend', (e) => e.preventDefault(), { passive: false });
  document.addEventListener('wheel', (e: WheelEvent) => { if (e.ctrlKey) e.preventDefault(); }, { passive: false });
}

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <App />
  </StrictMode>,
);

// The native splash hands off as soon as Firebase has produced the first
// definitive auth state. This prevents a white/N placeholder frame while still
// keeping auth restoration out of the visible React UI.
if (typeof window !== 'undefined') {
  let hidden = false;
  const hideNativeSplash = () => {
    if (hidden) return;
    hidden = true;
    SplashScreen.hide({ fadeOutDuration: 160 }).catch(() => undefined);
  };
  window.addEventListener('neo-gpt-auth-ready', hideNativeSplash, { once: true });
}
