# Quiet privacy policy

_Last updated: 25 October 2026_

Quiet is an Android app that blocks endless short-video feeds (Instagram Reels, TikTok For You, YouTube Shorts) and counts the time you spend in social apps. It works entirely on your phone. There is no Quiet account and no Quiet server.

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

Screen content read through the Accessibility service is used in the moment and then discarded. Quiet never saves messages, captions, comments, or anything you type. The optional screen dump in **Settings → Advanced → Inspector** saves the layout of one screen to your phone when you tap it, for fixing detection; it stays on your phone unless you choose to share it.

## What Quiet shares

Nothing. Quiet does not send any data off your phone, has no analytics or advertising SDKs, and does not sell or share data with anyone. Quiet does not request internet access for any of its features.

## Your choices

- You can turn the Accessibility service off at any time in Android's Accessibility settings. Quiet then stops reading screens and stops blocking.
- **Settings → Privacy and your data → Export my data** saves your settings and stats as a file you can keep or share.
- **Reset stats** deletes all time totals and the activity log.
- Uninstalling Quiet deletes everything it stored.

## Children

Quiet is not directed at children under 13 and does not knowingly collect any personal information from anyone.

## Changes

If this policy changes, for example if a future version adds features that need data to leave your phone, the new policy will be posted here with a new date before that version is released, and the app will explain the change.

## Contact

Questions or requests: open an issue at <https://github.com/vaanai/fixyodopaminebro/issues>.
