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

// Never let the native Android launch screen wait for Firebase/native auth.
// Hide it as soon as the first WebView frame has been scheduled; the React
// loading/auth UI then takes over without leaving the native splash stuck.
if (typeof window !== 'undefined') {
  const hideNativeSplash = () => {
    SplashScreen.hide({ fadeOutDuration: 220 }).catch(() => undefined);
  };
  requestAnimationFrame(() => requestAnimationFrame(hideNativeSplash));
  window.setTimeout(hideNativeSplash, 1800);
}
