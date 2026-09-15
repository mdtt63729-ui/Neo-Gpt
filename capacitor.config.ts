import type { CapacitorConfig } from '@capacitor/cli';

const config: CapacitorConfig = {
  plugins: {
    SplashScreen: {
      launchAutoHide: true,
      launchShowDuration: 700,
      launchFadeOutDuration: 250,
      backgroundColor: '#121212',
      splashFullScreen: true,
      splashImmersive: true,
      showSpinner: false,
    },
    FirebaseAuthentication: {
      providers: ['google.com'],
      skipNativeAuth: false,
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
