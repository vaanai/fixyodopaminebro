#!/usr/bin/env python3
"""Capture Quiet media screenshots using UI-automator text targets."""
import re
import subprocess
import time
from pathlib import Path

MEDIA = Path("/cursor/stores/bc-41cb6c44-cc62-437e-9170-1ef0290ea504/media")
APK = Path("/workspace/app/build/outputs/apk/debug/app-debug.apk")
ADB = "/home/ubuntu/Android/Sdk/platform-tools/adb"


def adb(*args: str) -> subprocess.CompletedProcess[bytes]:
    return subprocess.run([ADB, *args], check=True, capture_output=True)


def ui_xml() -> str:
    adb("shell", "uiautomator", "dump", "/sdcard/w.xml")
    return adb("shell", "cat", "/sdcard/w.xml").stdout.decode("utf-8", errors="ignore")


def ui_has_anr() -> bool:
    return "isn't responding" in ui_xml()


def dismiss_anr() -> None:
    if ui_has_anr():
        adb("shell", "input", "tap", "720", "1280")
        time.sleep(10)


def bounds_center(text: str) -> tuple[int, int]:
    xml = ui_xml()
    pattern = rf'text="{re.escape(text)}"[^>]*bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"'
    match = re.search(pattern, xml)
    if not match:
        raise RuntimeError(f"Could not find UI node for text: {text}")
    x1, y1, x2, y2 = map(int, match.groups())
    return (x1 + x2) // 2, (y1 + y2) // 2


def tap_text(text: str, delay: float = 2.5) -> None:
    dismiss_anr()
    x, y = bounds_center(text)
    adb("shell", "input", "tap", str(x), str(y))
    time.sleep(delay)


def tap_xy(x: int, y: int, delay: float = 2.5) -> None:
    dismiss_anr()
    adb("shell", "input", "tap", str(x), str(y))
    time.sleep(delay)


def ensure_quiet() -> None:
    adb("shell", "am", "start", "-n", "app.quiet/.MainActivity")
    time.sleep(2)
    dismiss_anr()


def screencap(path: Path) -> None:
    for _ in range(8):
        dismiss_anr()
        ensure_quiet()
        xml = ui_xml()
        if "package=\"app.quiet\"" not in xml:
            adb("shell", "input", "keyevent", "4")
            time.sleep(2)
            continue
        with path.open("wb") as f:
            subprocess.run([ADB, "exec-out", "screencap", "-p"], check=True, stdout=f)
        return
    raise SystemExit(f"Could not capture Quiet UI for {path}")


def wait_boot() -> None:
    adb("wait-for-device")
    while adb("shell", "getprop", "sys.boot_completed").stdout.decode().strip() != "1":
        time.sleep(5)
    time.sleep(10)


def main() -> None:
    wait_boot()
    adb("install", "-r", str(APK))
    adb("shell", "pm", "clear", "app.quiet")
    adb("shell", "am", "start", "-S", "-n", "app.quiet/.MainActivity")
    time.sleep(40)
    dismiss_anr()

    screencap(MEDIA / "onboarding-1.png")
    tap_text("Continue")
    tap_text("Not now")
    tap_text("Not now")
    tap_text("Go to Today", delay=8)
    screencap(MEDIA / "today.png")

    tap_text("Apps")
    time.sleep(2)
    screencap(MEDIA / "apps.png")

    tap_text("Instagram")
    time.sleep(3)
    screencap(MEDIA / "app-detail.png")

    tap_text("Library")
    time.sleep(3)
    screencap(MEDIA / "library.png")

    # Add sheet via + icon (no text) — top-right tap
    tap_xy(990, 192, 3)
    screencap(MEDIA / "library-edit.png")
    adb("shell", "input", "keyevent", "4")
    time.sleep(2)

    ensure_quiet()
    tap_text("Library")
    tap_xy(990, 192, 2)
    tap_xy(200, 720, 1)
    adb("shell", "input", "text", "Slow%20Horses")
    time.sleep(1)
    tap_text("Shows")
    tap_text("Save", delay=4)
    adb("shell", "input", "keyevent", "4")
    time.sleep(2)
    screencap(MEDIA / "library-card.png")

    ensure_quiet()
    tap_xy(990, 192, 3)
    screencap(MEDIA / "settings.png")
    subprocess.run(
        [ADB, "shell", "input", "swipe", "540", "1680", "540", "600", "500"],
        check=True,
    )
    time.sleep(2)
    screencap(MEDIA / "settings-lower.png")
    print("Captured all Quiet media screenshots.")


if __name__ == "__main__":
    main()
