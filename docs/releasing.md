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

Bump `versionCode` and `versionName` in `app/build.gradle.kts` for every upload.

## 3. Play Console checklist

- **Testing first.** New personal developer accounts must run a closed test with at least 12 testers opted in for 14 days before production access.
- **Privacy policy URL:** `https://github.com/vaanai/fixyodopaminebro/blob/main/docs/privacy-policy.md`
- **Data safety:** no data collected, no data shared. (The Accessibility service reads screens on the device only and nothing leaves it.)
- **Accessibility declaration** (App content → Sensitive permissions): Quiet is not an accessibility tool (`isAccessibilityTool="false"`). Core feature: blocking short-video feeds and tracking time in social apps. Record a short video showing the in-app disclosure (setup step "Before you turn it on"), the Agree button, turning the service on, and a blocked Reels tab.
- **App access for reviewers:** "No login. During setup you pick a settings lock (1 to 5 minutes): after setup, making Quiet less strict asks you to wait that long. Choose 1 minute to review quickly."
- **Target audience:** 13+ (not designed for children).
- **Content rating:** questionnaire; no user content, no ads (until ads are added).
- **Countries:** English-speaking countries first; detection matches English labels.
