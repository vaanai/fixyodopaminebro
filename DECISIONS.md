# Decisions

| Topic | Decision | Reason |
|-------|----------|--------|
| compileSdk / targetSdk | API 35 | Current stable platform at build time (Oct 2026). |
| TikTok package | `com.zhiliaoapp.musically` | International TikTok application id on current builds. |
| Gate hold | Fixed 3 s ring; no hold-time setting in Settings | Manager override: hold must not trap; "Not now" dismisses immediately. |
| Quiet hours end bound | End minute exclusive (07:00 is outside window) | Matches build plan window test at 07:00 exactly. |
| UI | `docs/ui-direction.md` tokens only | Manager override; dark is the product reference. |
