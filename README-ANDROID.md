# Neo Gpt Android build

This project keeps the existing React/Vite UI unchanged and wraps it with Capacitor for Android.

## GitHub
Push the project to GitHub. The workflow at `.github/workflows/android-apk.yml` runs on every push and builds a debug APK automatically. The APK is available in the workflow's Artifacts as `neo-gpt-debug-apk`.

## Local build

```bash
npm install
npm run android:add
npm run android:build
```

The debug APK is generated at:

`android/app/build/outputs/apk/debug/app-debug.apk`

The APK is a debug-signed APK suitable for testing. A Play Store release should use a proper release signing key and AAB.
