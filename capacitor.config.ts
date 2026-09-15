import type { CapacitorConfig } from '@capacitor/cli';

const config: CapacitorConfig = {
  plugins: {
    SplashScreen: {
      launchAutoHide: false,
      launchShowDuration: 0,
      launchFadeOutDuration: 160,
      backgroundColor: '#121212',
      splashFullScreen: true,
      splashImmersive: true,
      showSpinner: false,
    },
  },
  appId: 'com.neogpt.app',
  appName: 'Neo Gpt',
  webDir: 'dist',
  android: {
    allowMixedContent: false,
  },
  server: {
    androidScheme: 'https',
  },
};

export default config;
