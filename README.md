<div align="center">
<img width="1200" height="475" alt="GHBanner" src="https://ai.google.dev/static/site-assets/images/share-ais-513315318.png" />
</div>

# Run and deploy your AI Studio app

This contains everything you need to run your app locally.

View your app in AI Studio: https://ai.studio/apps/90255f01-01f2-4df6-a36d-0115a99c2f59

## Run Locally

**Prerequisites:**  Node.js


1. Install dependencies:
   `npm install`
2. Set the `GEMINI_API_KEY` in [.env.local](.env.local) to your Gemini API key
3. Run the app:
   `npm run dev`

## Authentication setup
Neo Gpt uses Firebase Authentication for email/password sign-in, account creation, password reset, and persistent local auth restoration. The Firebase web configuration is bundled in `src/lib/firebase.ts`. Firebase LOCAL persistence is configured before email/password sign-in and again during app startup; Firebase's `onAuthStateChanged` callback is the source of truth for restoring an existing account.

Google sign-in is intentionally not exposed in the login UI. Guest mode remains available.
