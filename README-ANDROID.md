# Neo Gpt Android build

This project keeps the existing React/Vite UI unchanged and wraps it with Capacitor for Android.

## GitHub
Push the project to GitHub. The workflow at `.github/workflows/android-apk.yml` runs on every push and builds a unsigned release APK automatically. The APK is available in the workflow's Artifacts as `neo-gpt-release-unsigned-apk`.

## Local build

```bash
npm install
npm run android:add
npm run android:build
```

The unsigned release APK is generated at:

`android/app/build/outputs/apk/debug/app-release-unsigned.apk`

The APK is a debug-signed APK suitable for testing. A Play Store release should use a proper release signing key and AAB.


### Launch splash fix (v28)
The Android launch splash is now explicitly hidden after the first WebView frame, with an automatic 700ms fallback. Firebase/native-auth startup cannot keep the native splash visible indefinitely. Microphone permission is no longer requested during Activity startup.


## Release version / Force Update matching

The Android APK version is controlled by `app-version.json`:

```json
{
  "versionName": "1.0.25",
  "versionCode": 25
}
```

Before publishing an APK from the admin panel, set these two values to the same release values you publish in Firebase (`appUpdates/latest`). The workflow writes them into the native Android APK. The workflow also supports optional `version_name` and `version_code` inputs when started manually; those override `app-version.json`.

The app compares the Firebase `versionCode` with the installed native Android `versionCode`. If they are equal, the update/force-update page is not shown.
