# Pixel test before merging

One pass on a real phone for the open Quiet changes (stories lag, Home on scroll, settings cleanup, the skip-the-wait ad, and the mode fixes). Install the `quiet-debug.apk` from the **test** release over the current app, so your settings carry over. Debug builds show Google's test ads, never real ones.

If something is off, note what you tapped and what you saw. With the phone plugged into a computer, Settings → Advanced → Verbose logging also writes what Quiet saw to logcat (tag Quiet).

## 1. Instagram Home and Stories

Set Instagram → Home to **Block**, Stories to **Allow**.

- [ ] Open Instagram on Home. The top with the Stories row is open, no cover.
- [ ] Scroll down past Stories. The cover appears above the tab bar with "Back up to Stories".
- [ ] Tap "Back up to Stories". You're back at the top and the cover goes at once.
- [ ] From the top, tap a story. It opens with no cover and no delay.
- [ ] Close the story. You're back at the top of Home, still no cover.
- [ ] Press the phone's Home button from a story, then reopen Instagram. No cover stuck on screen.
- [ ] Set Stories to **Block** too. Now Home is covered from the top, and tapping a story is covered.

## 2. Settings make sense

- [ ] Instagram's Feeds card lists Reels, Home, Stories and Explore. Chats and profiles aren't on it, and both always open.
- [ ] "When a blocked video feed opens" only mentions video feeds.
- [ ] Daily limit card: "Chats stay open" appears once a limit is set.
- [ ] Focus page has the "Unlocks during focus" switch. The Unlocks page points to it.
- [ ] YouTube with "Manage YouTube" off and a 10 min daily limit: after 10 minutes YouTube is covered.

## 3. Skip the wait with an ad

This setting is off on your phone after the update, because turning it on counts as loosening. Turn it on in Unlocks (the settings lock wait applies).

- [ ] Open Reels, tap unlock. During the wait there's "Skip the wait with 1 short ad".
- [ ] Tap it. Quiet's own "Skip the wait" screen opens with no Instagram cover over it.
- [ ] Watch the test ad to the end. You land back in Instagram on the unlock choices with "Wait skipped".
- [ ] Close an ad early. No skip, and the wait is still there.
- [ ] From the Today screen, "Skip with 1 short ad" works the same and unlocks.
- [ ] Today's unlock count goes up by one per unlock, not per ad.
- [ ] If you can, try once with a VPN to an EU country: a consent form comes first, and Settings → Privacy and your data shows "Ad privacy choices".

## 4. Unlocks and focus

- [ ] With unlocks set to videos, the cover on Reels offers "Watch N more". With Home set to Block, the cover past Stories offers minutes instead.
- [ ] Take "Watch 5 more" on Reels, then go to Explore or Home (blocked). They're still covered.
- [ ] Take a 15 min unlock, then start focus from the Today screen. Reels are covered at once.
- [ ] Settings → Privacy and your data → Reset stats. Today's allowance, unlocks and app time stay where they were.

## 5. Links from other apps

- [ ] Send yourself a reel and a TikTok in WhatsApp or Messages. Tapping each plays it, and swiping on is stopped after the chat allowance.
- [ ] Open Reels, switch to WhatsApp, then swipe back to Instagram with the gesture bar. Reels should still be covered. If it plays like a link instead, note it: that's the one case this can't check without a phone.

## Then

If everything passes, merge the pull requests in order: #12, #13, #14, #15, #16.
