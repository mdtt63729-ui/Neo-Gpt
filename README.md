# Neo Gpt

Production-oriented Android-only Neo Gpt chat application.

## Build

```bash
./gradlew assembleDebug --no-daemon --stacktrace
```

The GitHub Actions workflow at `.github/workflows/build-apk.yml` runs on every push and uploads `Neo-Gpt-debug-apk`.

## Runtime configuration

- Venus 3.1 is available by default through the existing Pico/HTML response path.
- OpenRouter, NVIDIA NIM, and Gemini become available after their API key is configured in Settings.
- API keys are encrypted with the Android Keystore before being stored locally.
- Theme and font selections persist with DataStore.

## Attachments

The composer supports Android Photo Picker, camera capture through FileProvider, and the Android document picker. Image-capable providers receive resized image data; unsupported generic file types produce an actionable error.

## App icon

The launcher icon uses the supplied Neo Gpt icon artwork from the project upgrade request.
