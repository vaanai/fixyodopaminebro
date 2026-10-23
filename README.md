# Quiet

Quiet keeps the endless parts of Instagram, TikTok and YouTube out of your day, and leaves the parts you actually need (chats, friends, the videos people send you) alone.

- **Reels tab / For You / Shorts**: blocked, limited, or behind a breathing pause. You choose per feed.
- **A reel a friend sent you** plays normally. Swiping past it into the algorithm is what gets stopped, with a separate allowance for chats, profiles, your feed, Explore and stories.
- **Blocked means a way out**: Instagram's Reels tab drops you into Messages, and TikTok skips For You and opens on your Inbox. Come straight back and you get the block screen with unlock options.
- **Unlocks** if you want them: watch N more, or N minutes, after a short wait, a few times a day. Strict mode removes them.
- **Focus**: schedules (they can cross midnight), one-tap sessions, and a Quick Settings tile.
- **Tracking**: daily time in every social app, endless-feed vs shared-video time, hourly heat strip, streaks, and daily limits for any app.

Everything stays on the phone.

## Install

```sh
adb install -r quiet-debug.apk
```

The newest build of `main` is always on the [`latest` release](../../releases/tag/latest). Every PR's CI run also uploads an APK artifact. All builds share one signing key, so they install over each other and keep your data.

Then open Quiet and follow the setup. If the accessibility switch is greyed out (Android 13+ sideload restriction): **App info → ⋮ → Allow restricted settings**, then try again.

## How detection works

Each supported app has a rules file in [`core/src/main/resources/rules`](core/src/main/resources/rules):

- **signals**: named node matchers (`id`, `idExact`, `text`, `desc`, `descStartsWith`, `hintContains`, `labelRegex`, `cls`, `selected`, `editable`, screen-position fractions, …). A signal is present when any matcher matches a visible node.
- **rules**: ordered; the first whose `all`/`any`/`none` signals hold decides the surface.
- **surfaces**: what the user configures (`VIDEO_FEED`, `VIDEO_VIEWER`, `FEED`, `EXPLORE`, `STORIES`, `MESSAGES`, …).
- **nav**: tabs the block screen and auto-skip can jump to.

To fix rules on a phone without rebuilding, push an edited copy and reload:

```sh
adb push instagram.json /sdcard/Android/data/app.quiet/files/rules/instagram.json
adb shell am broadcast -a app.quiet.DEBUG --es cmd reload -p app.quiet
```

Other debug commands (`-p app.quiet` is required):

| cmd | does |
|-----|------|
| `dump` | saves the current screen's tree to `/sdcard/Android/data/app.quiet/files/dumps/` |
| `reload` | reloads rule files |
| `state` | logs what the service sees (`adb logcat -s Quiet`) |
| `relock` | ends any unlock for the app in front |

The in-app **Inspector** (Settings → Diagnostics) shows the same live readout.

## Develop

- JDK 21, Android SDK 35
- `./gradlew :core:test` runs the engine tests (pure JVM, fast)
- `./gradlew :app:assembleDebug` builds the APK

`core` holds all the decision logic (detector, engine, settings, stats) with no Android dependency; `app` is the accessibility service, overlays and Compose UI.
