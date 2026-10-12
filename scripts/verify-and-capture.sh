#!/usr/bin/env bash
set -euo pipefail
export PATH="/home/ubuntu/Android/Sdk/platform-tools:$PATH"
MEDIA="/cursor/stores/bc-41cb6c44-cc62-437e-9170-1ef0290ea504/media"
APK="/workspace/app/build/outputs/apk/debug/app-debug.apk"

until [[ "$(adb shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" == "1" ]]; do sleep 5; done
sleep 10
adb install -r "$APK"
adb shell pm clear app.quiet
adb shell am start -S -n app.quiet/.MainActivity
sleep 22

verify_png() {
  python3 - "$1" <<'PY'
import sys
from PIL import Image
path = sys.argv[1]
img = Image.open(path).convert("RGB")
w, h = img.size
samples = 0
bright = 0
for y in range(0, h, max(1, h // 40)):
    for x in range(0, w, max(1, w // 20)):
        r, g, b = img.getpixel((x, y))
        samples += 1
        if r + g + b > 120:
            bright += 1
ratio = bright / max(1, samples)
print(f"{path}: bright_ratio={ratio:.2f}")
sys.exit(0 if ratio > 0.02 else 1)
PY
}

cap() {
  adb exec-out screencap -p > "$1"
  verify_png "$1"
}

CX=540; PRIMARY_Y=1968; NOT_NOW_Y=2112; TAB_Y=2256
cap "$MEDIA/onboarding-1.png"
adb shell input tap $CX $PRIMARY_Y; sleep 2
adb shell input tap $CX $NOT_NOW_Y; sleep 2
adb shell input tap $CX $NOT_NOW_Y; sleep 2
adb shell input tap $CX $PRIMARY_Y; sleep 8
cap "$MEDIA/today.png"
adb shell input tap 540 $TAB_Y; sleep 2
adb shell input tap 540 $TAB_Y; sleep 3
cap "$MEDIA/apps.png"
adb shell input tap $CX 840; sleep 3
cap "$MEDIA/app-detail.png"
adb shell input tap 890 $TAB_Y; sleep 3
cap "$MEDIA/library.png"
adb shell input tap 990 192; sleep 3
cap "$MEDIA/library-edit.png"
adb shell input keyevent KEYCODE_BACK; sleep 2
adb shell input tap 990 192; sleep 2
adb shell input tap 200 720; adb shell input text "Slow%20Horses"; sleep 1
adb shell input tap 216 1008; sleep 1
adb shell input tap $CX $PRIMARY_Y; sleep 3
adb shell input keyevent KEYCODE_BACK; sleep 2
cap "$MEDIA/library-card.png"
adb shell input tap 990 192; sleep 3
cap "$MEDIA/settings.png"
adb shell input swipe $CX 1680 $CX 600 500; sleep 2
cap "$MEDIA/settings-lower.png"
echo "All captures verified"
