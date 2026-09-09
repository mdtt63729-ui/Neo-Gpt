# Building ALTREX CODE Android App

## Prerequisites

- Android Studio Hedgehog (2023.1.1) or later
- JDK 17 or later
- Android SDK Platform 34
- Kotlin 1.9.20 or later

## Local Build

1. Clone the repository
2. Copy `local.properties.template` to `local.properties` and set your SDK path
3. Open in Android Studio or run from command line:

```bash
# Debug APK
./gradlew assembleDebug

# Unsigned Release APK
./gradlew assembleRelease
```

The APK will be at:
- Debug: `app/build/outputs/apk/debug/app-debug.apk`
- Release: `app/build/outputs/apk/release/app-release-unsigned.apk`

## GitHub Actions Build

This project includes GitHub Actions workflows that automatically build APKs:

### On every push to main/master (build.yml):
- Builds debug APK
- Builds unsigned release APK
- Uploads both as artifacts (30 day retention)
- Go to Actions tab > click the latest run > scroll down to Artifacts

### On tag push v* (release.yml):
- Builds unsigned release APK
- Creates a GitHub Release with the APK attached
- Run: `git tag v0.1.0 && git push origin v0.1.0`

## Installing the Unsigned Release APK

Since the APK is unsigned, you need to sign it before installing, OR install it with:
```bash
# Using adb (debug install — works even for unsigned on some setups)
adb install app/build/outputs/apk/release/app-release-unsigned.apk
```

If installation fails due to signing, you can sign it with a debug key:
```bash
# Generate a debug keystore (one-time)
keytool -genkey -v -keystore debug.keystore -alias androiddebugkey -keyalg RSA -keysize 2048 -validity 10000 -storepass android -keypass android

# Sign the APK
jarsigner -verbose -sigalg SHA256withRSA -digestalg SHA-256 -keystore debug.keystore -storepass android -keypass android app/build/outputs/apk/release/app-release-unsigned.apk androiddebugkey

# Align the APK
zipalign -v 4 app/build/outputs/apk/release/app-release-unsigned.apk app-release.apk

# Install
adb install app-release.apk
```

Or simply build the debug variant which is auto-signed:
```bash
./gradlew assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk
```
