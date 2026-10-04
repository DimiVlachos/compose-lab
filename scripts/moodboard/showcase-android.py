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
    (1.5, "scroll", "down"),          # scroll: the large title collapses
    (3.0, "scroll", "up"),
    (4.5, "tap", "Mykonos"),          # open a photo
    (7.5, "back", None),              # close it
    (9.0, "long", "Santorini"),       # the photo menu
    (11.0, "tap", "Delete"),          # delete: the confirmation
    (12.5, "tap", "Cancel"),
    (14.0, "tap", "Filter"),          # the filter sheet
    (16.0, "dismiss", "Drag handle"),  # swiped away by its handle
    (17.5, "tap", "Search"),          # search tab: suggestions first
    (19.0, "tap", "Search photos and tags"),
    (20.0, "type", "ruins"),          # results as you type
    (21.5, "enter", None),
    (23.0, "tap", "Delphi"),          # open a result
    (25.5, "back", None),
    (27.0, "tap", "Gallery"),         # back to the gallery
    (28.5, "tap", "Boards"),          # boards tab
    (30.0, "tap", "Islands"),         # a board
    (32.5, "edgeback", None),         # the back gesture
    (34.0, "tap", "More options for Blue"),  # delete a board: the action sheet
    (35.5, "tap", "Delete"),
    (37.5, "tap", "Cancel"),
    (39.0, "tap", "Gallery"),         # home
]
END_S = 41.0

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


def screen():
    size = adb("shell", "wm", "size", capture=True).split()[-1]
    w, h = map(int, size.split("x"))
    return w, h


def restart():
    adb("shell", "am", "force-stop", APP)
    adb("shell", "am", "start", "-n", ACTIVITY)
    time.sleep(3)


def act(action, target, where):
    if action == "tap":
        adb("shell", "input", "tap", *map(str, where))
    elif action == "long":
        adb("shell", "input", "swipe", *map(str, where), *map(str, where), "700")
    elif action == "scroll":
        w, h = screen()
        a, b = (int(h * 0.8), int(h * 0.31)) if target == "down" else (int(h * 0.31), int(h * 0.8))
        # Unhurried (0.8 s, like the iOS drag), so the title collapse reads instead of flinging past.
        adb("shell", "input", "swipe", str(w // 2), str(a), str(w // 2), str(b), "800")
    elif action == "dismiss":
        _, h = screen()
        x, y = where
        adb("shell", "input", "swipe", str(x), str(y), str(x), str(int(h * 0.98)), "180")
    elif action == "edgeback":
        w, h = screen()
        adb("shell", "input", "swipe", "2", str(h // 2), str(int(w * 0.6)), str(h // 2), "350")
    elif action == "back":
        adb("shell", "input", "keyevent", "4")
    elif action == "type":
        adb("shell", "input", "text", target)
    elif action == "enter":
        # The keyboard's own Search key, as a finger presses it (it then closes the keyboard, as
        # iOS's does); a hardware Enter submits but leaves the keyboard up. Gboard on a Pixel puts
        # it at the bottom right.
        w, h = screen()
        adb("shell", "input", "tap", str(int(w * 0.922)), str(int(h * 0.911)))


def calibrate():
    """Walks the steps once, slowly, noting where each target is when its step comes."""
    restart()
    points = []
    for _, action, target in STEPS:
        where = find(target) if action in ("tap", "long", "dismiss") else None
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
