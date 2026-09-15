import { initializeApp, getApps, getApp, type FirebaseApp } from "firebase/app";
import { getAuth, setPersistence, browserLocalPersistence, type Auth } from "firebase/auth";
import { getDatabase, type Database } from "firebase/database";

export const firebaseConfig = {
  apiKey: "AIzaSyDkM0rd0uChAx8i-cldEJO4A_SeahluJco",
  authDomain: "dhun-website.firebaseapp.com",
  databaseURL: "https://dhun-website-default-rtdb.firebaseio.com",
  projectId: "dhun-website",
  storageBucket: "dhun-website.firebasestorage.app",
  messagingSenderId: "399124914707",
  appId: "1:399124914707:web:2d006d7ed65c672a4f95e6"
} as const;

let app: FirebaseApp | null = null;
let auth: Auth | null = null;
let database: Database | null = null;

function getFirebaseApp() {
  if (app) return app;
  app = getApps().length ? getApp() : initializeApp(firebaseConfig);
  return app;
}

export function firebaseReady() {
  try { getFirebaseApp(); return true; } catch { return false; }
}

export function firebaseAuth() {
  if (auth) return auth;
  auth = getAuth(getFirebaseApp());
  return auth;
}

export async function configureFirebasePersistence() {
  const currentAuth = firebaseAuth();
  await setPersistence(currentAuth, browserLocalPersistence);
  return currentAuth;
}

export function firebaseDatabase() {
  if (database) return database;
  database = getDatabase(getFirebaseApp());
  return database;
}
