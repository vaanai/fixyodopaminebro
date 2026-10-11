# Quiet

Quiet is an Android sideload app that pauses short-form feeds inside Instagram and TikTok while keeping messages reachable. It never closes the host apps.

## Install

1. Build a debug APK: `./gradlew :app:assembleDebug`
2. Copy `app/build/outputs/apk/debug/app-debug.apk` to your phone.
3. Install from Files or `adb install -r app-debug.apk`.
4. On Android 13+, if the accessibility toggle is greyed out: App info → Quiet → menu → **Allow restricted settings**, then enable the Quiet accessibility service.
5. Set battery to **Unrestricted** / **Don't optimize** and lock Quiet in Recents when your OEM offers those options.

Release builds: set `QUIET_KEYSTORE_PATH`, `QUIET_KEYSTORE_PASSWORD`, `QUIET_KEY_ALIAS`, and `QUIET_KEY_PASSWORD`, then run `scripts/build-release.sh`.

## What v1 enforces

Only **Instagram Home and Reels** and **TikTok For You and Following** (Balanced default). Stories, Explore, Discover, and in-chat media are not enforced yet. Unrecognized screens fail open.

## Morning check

See [MORNING.md](MORNING.md) for snapshots and on-device verification.

## Develop

- JDK 21
- Android SDK API 35
- Tests: `./gradlew :core:test :app:test :app:assembleDebug`
