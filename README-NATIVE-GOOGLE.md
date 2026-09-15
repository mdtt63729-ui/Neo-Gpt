# Native Android Google Sign-In (v22)

v22 upgrades the Google button to use the native Firebase Authentication plugin on Android/iOS. On Android, the Google flow uses Android Credential Manager / Google Play Services instead of an in-WebView OAuth page, so the account picker is presented by the operating system/Google.

## Important Firebase setup

The web Firebase configuration alone is not enough for native Android Google authentication. Firebase requires an Android app registration, SHA-1 certificate fingerprints, and the generated `google-services.json` file. Firebase's current Android documentation explicitly requires the Android SHA-1 and updated `google-services.json` for Google Sign-In. See the official Firebase documentation for the exact project-side setup.

For GitHub Actions, add the complete `google-services.json` contents as a repository secret named:

`GOOGLE_SERVICES_JSON`

The workflow will install it as `android/app/google-services.json` and enable the Google Services Gradle plugin automatically. If that secret is absent, the APK still builds and the web Firebase Google flow remains available as a fallback, but the native Android account picker cannot authenticate until the Android Firebase configuration is supplied.

The Firebase Authentication provider `google.com` must also be enabled in the Firebase Console.

## Why this is native

The app calls `FirebaseAuthentication.signInWithGoogle({ useCredentialManager: true })` on Android. The native plugin's Google flow uses the Android Credential Manager on supported versions. After successful authentication, Neo Gpt restores the native session and uses the Firebase ID token for its app session.

The web/PWA path continues to use Firebase's web Google provider with `prompt=select_account`.
