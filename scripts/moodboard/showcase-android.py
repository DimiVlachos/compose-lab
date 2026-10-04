#!/usr/bin/env python3
"""Plays the Moodboard showcase on an Android device or emulator, on the same timeline as the iOS
UI test (moodboard/iosApp/MoodboardUITests/ShowcaseUITests.swift), and records it.

Usage: scripts/moodboard/showcase-android.py <out.mp4>

A first, unrecorded pass finds each step's target on screen (uiautomator), so the recorded pass
only sends input, on time. Needs the app installed and one device (or ANDROID_SERIAL).
"""
import pathlib
import re
import subprocess
import sys
import time

APP = "dev.dimvlachos.moodboard"
ACTIVITY = f"{APP}/.android.MainActivity"

# (seconds from the start, action, target). Keep in step with ShowcaseUITests.swift.
STEPS = [
    (1.5, "tap", "Mykonos"),          # open a photo
    (4.5, "back", None),              # close it
    (6.5, "long", "Santorini"),       # the photo menu
    (8.5, "tap", "Delete"),           # delete: the confirmation
    (10.0, "tap", "Cancel"),
    (11.5, "tap", "Search"),          # search tab: suggestions first
    (13.0, "tap", "Search photos and tags"),
    (14.0, "type", "ruins"),          # results as you type
    (15.5, "enter", None),
    (17.0, "tap", "Delphi"),          # open a result
    (19.5, "back", None),
    (21.0, "tap", "Gallery"),         # back to the gallery
    (22.5, "tap", "Boards"),          # boards tab
    (24.0, "tap", "Islands"),         # a board
    (26.5, "back", None),
    (28.0, "tap", "Gallery"),         # home
]
END_S = 30.5


def adb(*args, capture=False):
    cmd = ["adb", *args]
    if capture:
        return subprocess.run(cmd, check=True, capture_output=True, text=True).stdout
    subprocess.run(cmd, check=True, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)


def find(label):
    """Centre of the node whose text or content description is exactly [label]."""
    adb("shell", "uiautomator", "dump", "/sdcard/ui.xml")
    xml = adb("shell", "cat", "/sdcard/ui.xml", capture=True)
    for node in re.findall(r"<node [^>]*>", xml):
        names = re.findall(r'(?:text|content-desc)="([^"]*)"', node)
        if label in names:
            a, b, c, d = map(int, re.search(r'bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', node).groups())
            return (a + c) // 2, (b + d) // 2
    sys.exit(f"'{label}' not on screen")


def restart():
    adb("shell", "am", "force-stop", APP)
    adb("shell", "am", "start", "-n", ACTIVITY)
    time.sleep(3)


def act(action, target, where):
    if action == "tap":
        adb("shell", "input", "tap", *map(str, where))
    elif action == "long":
        adb("shell", "input", "swipe", *map(str, where), *map(str, where), "700")
    elif action == "back":
        adb("shell", "input", "keyevent", "4")
    elif action == "type":
        adb("shell", "input", "text", target)
    elif action == "enter":
        adb("shell", "input", "keyevent", "66")


def calibrate():
    """Walks the steps once, slowly, noting where each target is when its step comes."""
    restart()
    points = []
    for _, action, target in STEPS:
        where = find(target) if action in ("tap", "long") else None
        points.append(where)
        act(action, target, where)
        time.sleep(2.5)
    return points


def main():
    out = sys.argv[1]
    points = calibrate()
    restart()
    # An emulator records on the host (its own encoder can't do full resolution); a phone records
    # on the device.
    emulator = adb("get-serialno", capture=True).strip().startswith("emulator-")
    if emulator:
        host_file = str(pathlib.Path(out).with_suffix(".webm").resolve())
        adb("emu", "screenrecord", "start", "--bit-rate", "20000000", host_file)
        rec = None
    else:
        rec = subprocess.Popen(
            ["adb", "shell", "screenrecord", "--bit-rate", "20000000", "--time-limit", str(int(END_S) + 4), "/sdcard/showcase.mp4"]
        )
    time.sleep(1.0)
    t0 = time.monotonic()
    for (at, action, target), where in zip(STEPS, points):
        delay = t0 + at - time.monotonic()
        if delay > 0:
            time.sleep(delay)
        act(action, target, where)
    time.sleep(max(0.0, t0 + END_S - time.monotonic()))
    if emulator:
        adb("emu", "screenrecord", "stop")
        time.sleep(2.0)
        subprocess.run(["ffmpeg", "-loglevel", "error", "-y", "-i", host_file, "-c:v", "libx264", "-crf", "14", "-pix_fmt", "yuv420p", out], check=True)
    else:
        rec.wait()
        time.sleep(1.0)
        adb("pull", "/sdcard/showcase.mp4", out)
    print(out)


if __name__ == "__main__":
    main()
