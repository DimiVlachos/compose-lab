#!/usr/bin/env python3
"""Retimes the iOS showcase take onto the Android take's clock, step by step.

Usage: showcase-align.py <ios.mov> <iosStart> <android.mp4> <androidStart> <out-ios.mp4>

XCUITest starts each step a little early or late (its element queries take a variable time), so
one offset can't keep the takes in step. This finds where each step visibly begins in both takes,
cuts the iOS take into one piece per step (starting in the still moment just before it), and lays
each piece where Android's step begins, holding the last frame over any gap. The result runs on the
Android take's timeline: pass androidStart as the iOS start to showcase-compose.sh.
"""
import subprocess
import sys

# When each step starts on the script's clock; keep in step with showcase-android.py.
STEPS = [1.5, 3.0, 4.5, 7.5, 9.0, 11.0, 12.5, 14.0, 16.0, 17.5, 19.0, 20.0, 21.5, 23.0, 25.5, 27.0,
         28.5, 30.0, 32.5, 34.0, 35.5, 37.5, 39.0]
END = 41.0
LEAD = 0.35  # each piece starts this long before its step's visible start, while nothing moves
FPS = 60
W, H = 36, 80


def frames(path):
    raw = subprocess.run(
        ["ffmpeg", "-loglevel", "error", "-i", path, "-vf", f"fps={FPS},scale={W}:{H}", "-f", "rawvideo", "-pix_fmt", "gray", "-"],
        capture_output=True, check=True).stdout
    n = len(raw) // (W * H)
    return [raw[i * W * H:(i + 1) * W * H] for i in range(n)]


def onsets(path, start):
    """The first visible change near each step, in seconds of the take; the expected time if none."""
    fr = frames(path)
    found = []
    for at in STEPS:
        lo = max(0, int((start + at - 0.5) * FPS))
        hi = min(len(fr), int((start + at + 1.2) * FPS))
        hit = start + at
        for i in range(lo + 1, hi):
            if sum(abs(a - b) for a, b in zip(fr[i], fr[lo])) / (W * H) > 4:
                hit = i / FPS
                break
        # A change at the very start of the window is the previous step still animating, and a
        # step can't start before the one before it: use the scripted time then.
        if hit - lo / FPS < 0.1 or (found and hit < found[-1] + 0.3):
            hit = start + at
        found.append(hit)
    return found


def main():
    ios, ios0, android, android0, out = sys.argv[1], float(sys.argv[2]), sys.argv[3], float(sys.argv[4]), sys.argv[5]
    i_on, a_on = onsets(ios, ios0), onsets(android, android0)
    # Piece k covers Android time [a_on[k] - LEAD, a_on[k+1] - LEAD); the first starts at the take's start.
    bounds = [android0] + [a - LEAD for a in a_on[1:]] + [android0 + END]
    # The first piece starts as far before the first step as Android's take does.
    sources = [i_on[0] - (a_on[0] - android0)] + [i - LEAD for i in i_on[1:]]
    parts, filters = [], []
    for k, (src, a, b) in enumerate(zip(sources, bounds, bounds[1:])):
        length = b - a
        filters.append(
            f"[0:v]trim=start={src:.3f}:duration={length:.3f},setpts=PTS-STARTPTS,"
            f"tpad=stop_mode=clone:stop_duration={length:.3f},trim=duration={length:.3f}[p{k}]")
        parts.append(f"[p{k}]")
    graph = ";".join(filters) + ";" + "".join(parts) + f"concat=n={len(parts)}:v=1:a=0[v]"
    # The simulator only writes a frame when the screen changes: make it constant-rate first, so a
    # cut never starts on a missing (black) frame.
    cfr = out + ".cfr.mp4"
    subprocess.run(["ffmpeg", "-loglevel", "error", "-y", "-i", ios, "-vf", f"fps={FPS}", "-c:v", "libx264", "-crf", "12", cfr], check=True)
    # Pad the front so the output's clock is the Android take's.
    subprocess.run(
        ["ffmpeg", "-loglevel", "error", "-y", "-i", cfr, "-filter_complex", graph + f";[v]tpad=start_duration={android0:.3f}:start_mode=clone[o]",
         "-map", "[o]", "-c:v", "libx264", "-crf", "14", "-pix_fmt", "yuv420p", out], check=True)
    for k, (i, a) in enumerate(zip(i_on, a_on)):
        print(f"step {k:2d}: ios {i - ios0:5.2f}  android {a - android0:5.2f}")
    print(out)


if __name__ == "__main__":
    main()
