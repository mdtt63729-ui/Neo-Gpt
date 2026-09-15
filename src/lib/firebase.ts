export const firebaseConfig = {
  apiKey: "AIzaSyDkM0rdUoChAx8i-cldEJO4A_SeahluJco",
  authDomain: "dhun-website.firebaseapp.com",
  databaseURL: "https://dhun-website-default-rtdb.firebaseio.com",
  projectId: "dhun-website",
  storageBucket: "dhun-website.firebasestorage.app",
  messagingSenderId: "399124914707",
  appId: "1:399124914707:web:2d006d7ed65c672a4f95e6"
} as const;

declare global {
  interface Window {
    firebase?: any;
  }
}

export function firebaseReady() {
  return typeof window !== 'undefined' && !!window.firebase;
}

export function firebaseAuth() {
  if (!firebaseReady()) throw new Error('Firebase Authentication SDK is not loaded.');
  const app = window.firebase.apps?.length ? window.firebase.app() : window.firebase.initializeApp(firebaseConfig);
  return window.firebase.auth(app);
}

export function firebaseDatabase() {
  if (!firebaseReady()) throw new Error('Firebase SDK is not loaded.');
  const app = window.firebase.apps?.length ? window.firebase.app() : window.firebase.initializeApp(firebaseConfig);
  return window.firebase.database(app);
}
