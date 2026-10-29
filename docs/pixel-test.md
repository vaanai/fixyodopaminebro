# Pixel test before merging

One pass on a real phone for every open Quiet change, pull requests #12 to #18 (stories lag, Home on scroll, settings cleanup, the skip-the-wait ad, the mode fixes, TikTok misfires, and profiles, the unlock wait and the other rough edges). Install the `quiet-debug.apk` from the **test** release over the current app, so your settings carry over. Debug builds show Google's test ads, never real ones.

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

## 6. TikTok when it's allowed, and the way out

Set TikTok → For You to **Allow** (leave the rest as they are).

- [ ] Scroll For You for a couple of minutes, including past any LIVE preview in the feed. Nothing is covered.
- [ ] Go to Inbox, then tap Home. For You plays and stays open while you swipe.
- [ ] Open another app, then swipe back to TikTok with the gesture bar. Swipe through For You: still nothing covered.
- [ ] Open a video from a chat and swipe on. Nothing is covered (For You is open anyway).
- [ ] Tap the LIVE icon at the top left and enter a live room. That's still covered (LIVE is blocked).

Now set For You back to **Block**.

- [ ] Open a video someone sent you in a TikTok chat and swipe once. The cover comes up with "Go back". One tap takes you back to the chat, not out of TikTok.
- [ ] Tap "Go back" several times fast. It still only goes back once.
- [ ] Send yourself a TikTok in Messages, open it, wait a few seconds and swipe. The cover offers "Go to Inbox" first, which lands in your TikTok inbox, and "Back to Messages".

## 7. Profiles, the unlock wait and other rough edges

- [ ] On Instagram Home, tap a friend's name to open their profile. No cover, and you can scroll their grid and open their posts feed.
- [ ] Do the same from Search: their profile opens with no cover. Home itself and Explore are still covered when you scroll them.
- [ ] Open Reels and tap "I want…". Leave Instagram mid-wait, come back: the countdown carries on where it was instead of starting over.
- [ ] Mid-wait, tap "Skip the wait with 1 short ad", then back out of the ad screen without watching. The wait is still counting down.
- [ ] Unlock from the Today screen. The card for that unlock shows straight away, and "Lock now" takes it away straight away.
- [ ] Set Explore to **Pause**. Open Explore, tap "Not now". You land on another Instagram tab, not your phone's home screen.
- [ ] Play music, open Reels so the cover comes up, then lock the phone. The music can be played from the lock screen with sound. Turn the screen on without unlocking: no cover on the lock screen. Unlock: Reels is covered again.
- [ ] Take a 15 minute unlock on Reels, then install the new test build over the old one (or turn Quiet off and on in Accessibility). Reels is still unlocked, with the time left where it was.
- [ ] Settings → Start over from a preset. A daily app limit you set before is still there afterwards.

## Then

If everything passes, merge the pull requests in order: #12, #13, #14, #15, #16, #17, #18.

Each one is stacked on the one before it. For each: choose **Create a merge commit** (not squash), merge, then tap **Delete branch**. Deleting the branch moves the next pull request onto main by itself, so it's ready to merge.
