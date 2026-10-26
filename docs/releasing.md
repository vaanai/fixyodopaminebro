# Releasing Quiet on Google Play

## 1. Make an upload key (once)

Play signs the app for users ("Play App Signing"); you sign uploads with your own **upload key**. Keep the file and passwords somewhere safe and never commit them.

```sh
keytool -genkeypair -v -keystore quiet-upload.jks -alias quiet-upload \
  -keyalg RSA -keysize 4096 -validity 10000
```

`keytool` comes with any JDK (Android Studio bundles one).

## 2. Build a signed bundle

Either locally:

```sh
export QUIET_UPLOAD_STORE_FILE=/path/to/quiet-upload.jks
export QUIET_UPLOAD_STORE_PASSWORD=...
export QUIET_UPLOAD_KEY_ALIAS=quiet-upload
export QUIET_UPLOAD_KEY_PASSWORD=...
./gradlew :app:bundleRelease
# -> app/build/outputs/bundle/release/app-release.aab
```

(The same four names also work as Gradle properties in `~/.gradle/gradle.properties`.)

Or on GitHub: add these repository secrets (Settings → Secrets and variables → Actions) and every CI run uploads a signed `quiet-release-aab` artifact:

| Secret | Value |
|--------|-------|
| `QUIET_UPLOAD_KEYSTORE_B64` | `base64 -w0 quiet-upload.jks` (PowerShell: `[Convert]::ToBase64String([IO.File]::ReadAllBytes("quiet-upload.jks"))`) |
| `QUIET_UPLOAD_STORE_PASSWORD` | keystore password |
| `QUIET_UPLOAD_KEY_ALIAS` | `quiet-upload` |
| `QUIET_UPLOAD_KEY_PASSWORD` | key password |

Without the key, CI still builds the release bundle (unsigned) to prove R8 shrinking works.

### AdMob IDs

Quiet has one ad: an optional rewarded ad that skips an unlock's wait, shown only on Quiet's own "Skip the wait" screen. Debug builds always use Google's test IDs. A signed release needs the real ones, and the build stops if the upload key is set without them, so a Play upload can never ship test ads.

1. In AdMob, add the app (Android, not yet on Play is fine) and note its **App ID** (`ca-app-pub-…~…`).
2. Add one **Rewarded** ad unit, e.g. "Skip the wait", and note its **ad unit ID** (`ca-app-pub-…/…`).
3. In AdMob → Privacy & messaging, create a **GDPR** message (EEA, UK and Switzerland) for the app. Quiet shows it before any ad loads, and shows an "Ad privacy choices" button on the Skip the wait screen where the law requires one.
4. Set them like the signing secrets (environment, Gradle property or GitHub secret):

| Name | Value |
|------|-------|
| `QUIET_ADMOB_APP_ID` | the App ID |
| `QUIET_ADMOB_SKIP_WAIT_UNIT` | the rewarded ad unit ID |

Bump `versionCode` and `versionName` in `app/build.gradle.kts` for every upload.

## 3. Play Console checklist

- **Testing first.** New personal developer accounts must run a closed test with at least 12 testers opted in for 14 days before production access.
- **Privacy policy URL:** `https://github.com/vaanai/fixyodopaminebro/blob/main/docs/privacy-policy.md`
- **Data safety:** Quiet itself collects nothing; the Accessibility service reads screens on the device only. The Google Mobile Ads SDK does collect and share data, so declare what Google lists for it (see the form drafts doc): approximate location (IP address), app interactions, diagnostics, and device or other IDs (advertising ID), shared for advertising and fraud prevention.
- **Ads:** answer "Yes, my app contains ads".
- **Accessibility declaration** (App content → Sensitive permissions): Quiet is not an accessibility tool (`isAccessibilityTool="false"`). Core feature: blocking short-video feeds and tracking time in social apps. Record a short video showing the in-app disclosure (setup step "Before you turn it on"), the Agree button, turning the service on, and a blocked Reels tab.
- **App access for reviewers:** "No login. During setup you pick a settings lock (1 to 5 minutes): after setup, making Quiet less strict asks you to wait that long. Choose 1 minute to review quickly."
- **Target audience:** 13+ (not designed for children).
- **Content rating:** questionnaire; no user content; contains ads (one optional rewarded ad).
- **Countries:** English-speaking countries first; detection matches English labels.
