#!/usr/bin/env bash
set -euo pipefail
MEDIA="/cursor/stores/bc-41cb6c44-cc62-437e-9170-1ef0290ea504/media"
APK="/workspace/app/build/outputs/apk/debug/app-debug.apk"
export PATH="/home/ubuntu/Android/Sdk/platform-tools:$PATH"

adb wait-for-device
adb shell getprop sys.boot_completed | grep -q 1

adb install -r "$APK" >/dev/null
adb shell pm clear app.quiet >/dev/null
adb shell am start -n app.quiet/.MainActivity >/dev/null
sleep 3

cap() { adb exec-out screencap -p > "$MEDIA/$1"; sleep 1; }
tap() { adb shell input tap "$1" "$2"; sleep 1; }

cap onboarding-1.png

# Step 1 Continue
tap 540 1680
sleep 1
# Step 2 — Open settings optional; Not now
tap 540 1780
sleep 1
tap 540 1780
sleep 1
# Step 4 Go to Today
tap 540 1680
sleep 2
cap today.png

tap 540 1840
sleep 1
cap apps.png

tap 360 520
sleep 1
cap app-detail.png

tap 120 1840
sleep 1
tap 900 1840
sleep 1
cap library.png

tap 980 120
sleep 2
cap library-edit.png
adb shell input keyevent KEYCODE_BACK
sleep 1

cap library-card.png

tap 980 120
sleep 1
cap settings.png

adb shell input swipe 540 1400 540 400 300
sleep 1
cap settings-lower.png

echo "Saved screenshots to $MEDIA"
