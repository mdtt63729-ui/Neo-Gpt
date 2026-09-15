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

// Native splash is never tied to Firebase/network. Hand it to the React
// premium splash as soon as the first React paint is ready.
if (typeof window !== 'undefined') {
  requestAnimationFrame(() => {
    requestAnimationFrame(() => {
      SplashScreen.hide({ fadeOutDuration: 180 }).catch(() => undefined);
    });
  });
}
