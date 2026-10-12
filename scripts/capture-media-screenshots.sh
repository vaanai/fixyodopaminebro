#!/usr/bin/env bash
set -euo pipefail
MEDIA="/cursor/stores/bc-41cb6c44-cc62-437e-9170-1ef0290ea504/media"
APK="/workspace/app/build/outputs/apk/debug/app-debug.apk"
export PATH="/home/ubuntu/Android/Sdk/platform-tools:$PATH"

adb wait-for-device
until [[ "$(adb shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" == "1" ]]; do sleep 2; done

dismiss_anr() {
  adb shell input keyevent KEYCODE_BACK 2>/dev/null || true
  sleep 0.5
  adb shell input tap 700 1050 2>/dev/null || true
  sleep 0.5
  adb shell input tap 540 1300 2>/dev/null || true
}

wait_quiet() {
  local tries=0
  while [[ $tries -lt 30 ]]; do
    dismiss_anr
    if adb shell dumpsys window 2>/dev/null | tr -d '\r' | grep -q "app.quiet"; then
      sleep 2
      return 0
    fi
    sleep 1
    tries=$((tries + 1))
  done
  return 1
}

cap() {
  local name="$1"
  dismiss_anr
  sleep 2
  adb exec-out screencap -p > "$MEDIA/$1"
  local size
  size=$(wc -c < "$MEDIA/$1")
  if [[ "$size" -lt 20000 ]]; then
    echo "WARN: $name only ${size} bytes"
    return 1
  fi
  echo "OK: $name (${size} bytes)"
}

tap() { adb shell input tap "$1" "$2"; sleep 1.5; }

SIZE=$(adb shell wm size | tr -d '\r' | awk '{print $3}')
W=${SIZE%x*}
H=${SIZE#*x}
CX=$((W / 2))
# Primary button above nav bar
PRIMARY_Y=$((H * 82 / 100))
NOT_NOW_Y=$((H * 88 / 100))
TAB_TODAY_X=$((W * 18 / 100))
TAB_APPS_X=$((W * 50 / 100))
TAB_LIBRARY_X=$((W * 82 / 100))
TAB_Y=$((H * 94 / 100))
GEAR_X=$((W * 92 / 100))
GEAR_Y=$((H * 8 / 100))

adb install -r "$APK" >/dev/null
adb shell pm clear app.quiet >/dev/null
adb shell am start -S -n app.quiet/.MainActivity >/dev/null
wait_quiet || true
sleep 5
dismiss_anr
sleep 2

cap onboarding-1.png

# Finish onboarding: Continue → Not now ×2 → Go to Today
tap "$CX" "$PRIMARY_Y"
sleep 1
tap "$CX" "$NOT_NOW_Y"
sleep 1
tap "$CX" "$NOT_NOW_Y"
sleep 1
tap "$CX" "$PRIMARY_Y"
wait_quiet || true
sleep 3
cap today.png

tap "$TAB_APPS_X" "$TAB_Y"
wait_quiet || true
sleep 2
cap apps.png

tap "$CX" "$((H * 35 / 100))"
wait_quiet || true
sleep 2
cap app-detail.png

tap "$TAB_LIBRARY_X" "$TAB_Y"
wait_quiet || true
sleep 2
cap library.png

tap "$((W * 92 / 100))" "$((H * 10 / 100))"
wait_quiet || true
sleep 2
cap library-edit.png
adb shell input keyevent KEYCODE_BACK
sleep 2

adb shell am start -n app.quiet/.MainActivity >/dev/null
wait_quiet || true
tap "$TAB_LIBRARY_X" "$TAB_Y"
sleep 2
# seed item via run-as not easy — open add sheet, save item via UI taps if empty
cap library-card.png

tap "$GEAR_X" "$GEAR_Y"
wait_quiet || true
sleep 2
cap settings.png

adb shell input swipe "$CX" "$((H * 70 / 100))" "$CX" "$((H * 25 / 100))" 400
sleep 2
cap settings-lower.png

echo "Done. Files in $MEDIA"
