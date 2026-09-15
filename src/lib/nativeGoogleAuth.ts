import { Capacitor } from '@capacitor/core';

let pluginPromise: Promise<any | null> | null = null;

async function getPlugin() {
  if (Capacitor.getPlatform() !== 'android' && Capacitor.getPlatform() !== 'ios') return null;
  if (!pluginPromise) {
    pluginPromise = import('@capacitor-firebase/authentication')
      .then(module => module.FirebaseAuthentication)
      .catch(() => null);
  }
  return pluginPromise;
}

export function nativeGoogleAvailable() {
  return Capacitor.getPlatform() === 'android' || Capacitor.getPlatform() === 'ios';
}

export async function signInWithNativeGoogle() {
  const plugin = await getPlugin();
  if (!plugin) throw new Error('Native Google Sign-In is not available in this build.');

  const result = await plugin.signInWithGoogle({
    useCredentialManager: true,
  });
  const user = result?.user;
  if (!user) throw new Error('Google Sign-In completed without a user.');

  let token = '';
  try {
    const tokenResult = await plugin.getIdToken({ forceRefresh: true });
    token = tokenResult?.token || '';
  } catch {
    // The native auth session itself is still valid even if the token bridge is unavailable.
  }

  return {
    user,
    token,
  };
}

export async function getNativeCurrentUser() {
  const plugin = await getPlugin();
  if (!plugin) return null;
  try {
    const result = await plugin.getCurrentUser();
    return result?.user || null;
  } catch {
    return null;
  }
}

export async function nativeSignOut() {
  const plugin = await getPlugin();
  if (!plugin) return;
  try {
    await plugin.signOut();
  } catch {
    // Keep web/local logout resilient when native auth is not configured.
  }
}
