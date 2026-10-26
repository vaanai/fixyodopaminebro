# Quiet

Quiet keeps the endless parts of Instagram, TikTok and YouTube out of your day, and leaves the parts you actually need (chats, friends, the videos people send you) alone.

- **Reels tab / For You / Shorts**: blocked, limited, or behind a breathing pause. You choose per feed.
- **A reel a friend sent you** plays normally. Swiping past it into the algorithm is what gets stopped, with a separate allowance for chats, profiles, your feed, Explore and stories. Links opened from other apps (a TikTok someone texted you) play too, even when the app opens them inside a blocked feed.
- **Blocked means a way out**: Instagram's Reels tab drops you into Messages, and TikTok skips For You and opens on your Inbox. Come straight back and you get the block screen with unlock options.
- **Instagram Home** can be left alone or blocked. Blocked, the top of Home and the **Stories row stay open**, and the block starts when you scroll down past them. Block Stories too and all of Home is covered.
- **Unlocks** if you want them: you choose whether each one gives you N more videos, N minutes, or a pick each time, after a short wait, a few times a day.
- **Settings lock**: picked during setup (1 to 5 minutes). Making Quiet less strict means waiting that long first; then settings stay open for 5 minutes. Tightening is always instant.
- **Focus**: schedules (they can cross midnight), one-tap sessions, and a Quick Settings tile.
- **Tracking**: daily time in every social app, endless-feed vs shared-video time, hourly heat strip, streaks, and daily limits for any app.

Everything stays on the phone. See the [privacy policy](docs/privacy-policy.md).

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

- **openTop** (optional, per surface): a signal for the top of a feed that stays open when the feed is blocked, like Instagram's Stories row. While it's on screen nothing is covered; scroll past it and the block starts. `surface` names where it leads (blocking that closes the top too) and `back` the nav target that scrolls back up.

To fix rules on a phone without rebuilding (debug builds only), push an edited copy and reload:

```sh
adb push instagram.json /sdcard/Android/data/app.quiet/files/rules/instagram.json
adb shell am broadcast -a app.quiet.DEBUG --es cmd reload -p app.quiet
```

Other debug commands (debug builds only; `-p app.quiet` is required):

| cmd | does |
|-----|------|
| `dump` | saves the current screen's tree to `/sdcard/Android/data/app.quiet/files/dumps/` |
| `reload` | reloads rule files |
| `state` | logs what the service sees (`adb logcat -s Quiet`) |
| `relock` | ends any unlock for the app in front |

The in-app **Inspector** (Settings → Advanced) shows the same live readout.

## Develop

- JDK 21, Android SDK 36
- `./gradlew :core:test` runs the engine tests (pure JVM, fast)
- `./gradlew :app:assembleDebug` builds the APK

- `./gradlew :app:bundleRelease` builds the Play bundle (see [releasing](docs/releasing.md))

`core` holds all the decision logic (detector, engine, settings, stats) with no Android dependency; `app` is the accessibility service, overlays and Compose UI.
