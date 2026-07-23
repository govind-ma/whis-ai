# google-services.json — Placeholder

> **⚠️ Replace this file with the real `google-services.json` from the shared Whis Firebase project before building any Firebase-dependent module (Call, AI Agent).**

The placeholder checked into VCS (`app/google-services.json`) contains dummy values so the Gradle build succeeds without a real Firebase config. It will **not** connect to any real Firebase backend.

## How to replace

1. Go to the [Firebase Console](https://console.firebase.google.com/) → Whis project → Project Settings → Your Apps → Android → Download `google-services.json`.
2. Drop the downloaded file into `app/google-services.json`, overwriting the placeholder.
3. **Do NOT commit the real file.** The `.gitignore` has a rule that ignores any `google-services.json` that differs from the tracked placeholder. If you accidentally stage it, `git checkout app/google-services.json` will restore the placeholder.

## Why the placeholder exists

The `google-services` Gradle plugin requires this file at sync/build time. Without it, `./gradlew assembleDebug` fails even for modules (Message, UI) that don't touch Firebase. The placeholder keeps the build green for everyone.
