# Quiet privacy policy

_Last updated: 27 October 2026_

Quiet is an Android app that blocks endless short-video feeds (Instagram Reels, TikTok For You, YouTube Shorts) and counts the time you spend in social apps. Its blocking and time tracking work entirely on your phone. There is no Quiet account and no Quiet server. The one exception is an optional ad, described below, that you can watch to skip an unlock's wait.

## What Quiet accesses

Quiet uses Android's **Accessibility service** (the `AccessibilityService` API). With it, Quiet can see:

- **Which app is open**, so it can count your time in social apps and apply the limits you set.
- **The layout of Instagram, TikTok and YouTube screens**: tab names, button labels, content descriptions and view IDs. Quiet uses these to tell an endless video feed apart from a chat, a profile or a video someone sent you.

Quiet uses the Accessibility service to:

- decide whether to cover a feed you chose to block, and draw the block screen over it;
- tap a tab (such as Messages) to skip a blocked feed, and press Back or Home when you choose to leave;
- count time spent in the apps you track.

Quiet is not an assistive-technology tool. It uses the Accessibility service only for the blocking and time tracking described here, and asks for your agreement on a separate screen before you turn it on.

## What Quiet stores

On your phone only, in Quiet's private app storage:

- your settings;
- daily totals: time per app, time in video feeds, number of blocks and unlocks;
- a short activity log ("Blocked a feed", "Unlocked for 5 min") with times.

Screen content read through the Accessibility service is used in the moment and then discarded. The version of Quiet on Google Play never saves messages, captions, comments, anything you type, or any other screen content. Test builds used for fixing detection have an extra screen dump tool that saves one screen, including its text, to the phone when you tap it; it stays on your phone unless you choose to share it.

## What Quiet shares

Quiet itself sends nothing off your phone and does not sell or share your data. Your settings, stats, activity log and anything read through the Accessibility service never leave the phone. Quiet has no analytics.

## Ads

When you unlock a blocked feed, Quiet first asks you to wait. Instead of waiting, you can choose to watch one short ad. That ad is shown by **Google AdMob**, only on Quiet's own "Skip the wait" screen, and only when you open that screen. Ads never appear over other apps.

To show and measure ads, Google's Mobile Ads SDK, which is part of the app, collects information such as your IP address (and so your approximate location), your device's advertising ID, and how you interact with the ad, and Google may use it as described in [Google's privacy policy](https://policies.google.com/privacy) and [how Google uses information from apps that use its services](https://policies.google.com/technologies/partner-sites). Quiet does not give Google anything from your settings, stats or screen content.

If you are in the European Economic Area, the UK or Switzerland, Quiet asks for your consent through Google's consent form before any ad loads, and you can change your choice later with **Ad privacy choices** on the Skip the wait screen. Anywhere, you can reset or delete your advertising ID in Android's settings (Settings → Privacy → Ads), or turn the ad off in Quiet (**Settings → Unlocks → Skip the wait with an ad**), in which case unlocking always means waiting.

## Your choices

- You can turn the Accessibility service off at any time in Android's Accessibility settings. Quiet then stops reading screens and stops blocking.
- **Settings → Privacy and your data → Export my data** saves your settings and stats as a file you can keep or share.
- **Reset stats** deletes all time totals and the activity log.
- Uninstalling Quiet deletes everything it stored.

## Children

Quiet is not directed at children under 13 and does not knowingly collect any personal information from anyone.

## Changes

If this policy changes, for example if a future version adds other features that need data to leave your phone, the new policy will be posted here with a new date before that version is released, and the app will explain the change.

## Contact

Questions or requests: open an issue at <https://github.com/vaanai/fixyodopaminebro/issues>.
