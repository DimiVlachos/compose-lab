#!/usr/bin/env python3
"""Record one compose-lab demo and encode a LinkedIn-ready clip: 4:5, or 16:9 for a landscape demo.

Usage: scripts/record.py <android|ios> <demoId> [--label] [--landscape]

The app logs LAB_DEMO_START / LAB_DEMO_DONE (via Kermit, tag LabRecorder) when
the demo starts and ends, and the clip is cut between those two moments (timed
on this machine). Every script holds a still frame at both ends, so small
timing errors don't show. Android needs the app installed
(./gradlew :androidApp:installDebug) and one device connected. iOS needs a
booted simulator with the app installed (scripts/install-ios.sh).
"""
import argparse
import pathlib
import queue
import signal
import subprocess
import sys
import threading
import time

ROOT = pathlib.Path(__file__).resolve().parent.parent
OUT = ROOT / "out"
APP_ID = "dev.dimvlachos.lab"
ACTIVITY = f"{APP_ID}/.MainActivity"
DEVICE_FILE = "/sdcard/compose-lab-demo.mp4"
MAX_BYTES = 5 * 1024 * 1024
MARKER_TIMEOUT_S = 60
# How long a landscape demo takes to turn the screen before recording starts.
LANDSCAPE_SETTLE_S = 2.5
# screenrecord captures its first frame about this long before the file has any bytes (measured on a Galaxy S23 Ultra).
ANDROID_CAPTURE_LEAD_S = 0.08


def run(cmd):
    subprocess.run(cmd, check=True)


def stream_lines(proc):
    """Read a process's stdout on a background thread, as (monotonic time, line); None marks the end."""
    lines = queue.Queue()

    def pump():
        for line in proc.stdout:
            lines.put((time.monotonic(), line))
        lines.put((time.monotonic(), None))

    threading.Thread(target=pump, daemon=True).start()
    return lines


def wait_for_markers(lines, demo_id, t0):
    """Return (start, done) in seconds since t0, or exit with a clear message."""
    start = None
    deadline = time.monotonic() + MARKER_TIMEOUT_S
    while True:
        remaining = deadline - time.monotonic()
        if remaining <= 0:
            sys.exit(f"Timed out after {MARKER_TIMEOUT_S}s waiting for demo '{demo_id}' to finish")
        try:
            at, line = lines.get(timeout=remaining)
        except queue.Empty:
            continue
        if line is None:
            sys.exit("The app's output ended before the demo finished")
        text = line.strip()
        if "LAB_WARN" in text:
            sys.exit(f"The app reported: {text[text.index('LAB_WARN'):]}")
        if text.endswith(f"LAB_DEMO_START {demo_id}"):
            start = at - t0
        elif text.endswith(f"LAB_DEMO_DONE {demo_id}"):
            if start is None:
                sys.exit("Saw LAB_DEMO_DONE without LAB_DEMO_START")
            return start, at - t0


def android_record_size():
    """1080 wide, keeping the screen's aspect ratio (screenrecord needs even sizes)."""
    out = subprocess.run(["adb", "shell", "wm", "size"], check=True, capture_output=True, text=True).stdout
    size = out.strip().splitlines()[-1].split(":")[1].strip()  # an override size, when present, is the last line
    width, height = (int(value) for value in size.split("x"))
    return 1080, round(height * 1080 / width / 2) * 2


def wait_for_recording_file(deadline_s=5.0, poll_s=0.05):
    """Poll until the device recording file is non-empty and return when that poll was sent.

    The send time, not the reply time, is the closer estimate of when recording began; the rest
    of the gap is ANDROID_CAPTURE_LEAD_S.
    """
    deadline = time.monotonic() + deadline_s
    while time.monotonic() < deadline:
        asked = time.monotonic()
        result = subprocess.run(
            ["adb", "shell", "stat", "-c", "%s", DEVICE_FILE], capture_output=True, text=True
        )
        if result.returncode == 0:
            try:
                if int(result.stdout.strip()) > 0:
                    return asked
            except ValueError:
                pass
        time.sleep(poll_s)
    sys.exit(f"screenrecord did not start writing {DEVICE_FILE} within {deadline_s}s")


def record_android(demo_id, label, raw, landscape):
    width, height = android_record_size()
    if landscape:
        width, height = height, width
    run(["adb", "logcat", "-c"])
    log = subprocess.Popen(
        ["adb", "logcat", "-v", "raw", "-s", "LabRecorder:V"], stdout=subprocess.PIPE, text=True
    )
    lines = stream_lines(log)
    launch = ["adb", "shell", "am", "start", "-S", "-n", ACTIVITY, "--es", "demo", demo_id, "--ez", "record", "true"]
    if label:
        launch += ["--ez", "label", "true"]
    recorder = None
    failed = True
    try:
        if landscape:
            # A landscape demo turns the screen as it opens, and screenrecord keeps the orientation it
            # started in: start it once the app has turned, well inside the warm-up run.
            run(launch)
            time.sleep(LANDSCAPE_SETTLE_S)
        recorder = subprocess.Popen(
            ["adb", "shell", "screenrecord", "--bit-rate", "20000000", "--size", f"{width}x{height}", DEVICE_FILE]
        )
        t0 = wait_for_recording_file() - ANDROID_CAPTURE_LEAD_S
        if not landscape:
            run(launch)
        start, done = wait_for_markers(lines, demo_id, t0)
        failed = False
    finally:
        time.sleep(0.5)
        if recorder is not None:
            subprocess.run(["adb", "shell", "pkill", "-INT", "screenrecord"])
            recorder.wait()
        log.terminate()
        if failed:
            subprocess.run(["adb", "shell", "rm", "-f", DEVICE_FILE])
    time.sleep(1)  # let screenrecord finish writing the file
    run(["adb", "pull", DEVICE_FILE, str(raw)])
    run(["adb", "shell", "rm", DEVICE_FILE])
    return start, done


def record_ios(demo_id, label, raw):
    recorder = subprocess.Popen(
        ["xcrun", "simctl", "io", "booted", "recordVideo", "--codec=h264", "--force", str(raw)],
        stderr=subprocess.PIPE,
        text=True,
    )
    for line in recorder.stderr:
        if "Recording started" in line:
            break
    else:
        sys.exit("simctl did not start recording (is a simulator booted?)")
    t0 = time.monotonic()
    launch = ["xcrun", "simctl", "launch", "--console-pty", "--terminate-running-process", "booted", APP_ID,
              "-demo", demo_id, "-record"]
    if label:
        launch.append("-label")
    app = subprocess.Popen(launch, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True)
    lines = stream_lines(app)
    try:
        start, done = wait_for_markers(lines, demo_id, t0)
    finally:
        time.sleep(0.5)
        recorder.send_signal(signal.SIGINT)
        recorder.wait()
        app.terminate()
    return start, done


def encode(raw, start, done, dest, landscape):
    """Cut [start, done], crop the centred stage (4:5, or 16:9 in landscape) and raise the CRF until the clip fits in MAX_BYTES.

    Recorders only write a frame when the screen changes, so still holds are gaps in the raw file.
    fps=60 runs over the whole stream first to fill those gaps with the frame on screen, then the
    window is trimmed; tpad covers a still tail after the last written frame.
    """
    frame = (
        "crop=ih*16/9:ih:(iw-ih*16/9)/2:0,scale=1920:1080"
        if landscape
        else "crop=iw:iw*5/4:0:(ih-iw*5/4)/2,scale=1080:1350"
    )
    crf = 18
    while True:
        run([
            "ffmpeg", "-loglevel", "error", "-y",
            "-i", str(raw),
            "-map", "0:v:0",
            "-vf", f"fps=60,trim=start={start:.3f},setpts=PTS-STARTPTS,"
                   "tpad=stop_mode=clone:stop_duration=3,"
                   f"{frame}:flags=lanczos,format=yuv420p",
            "-t", f"{done - start:.3f}",
            "-c:v", "libx264", "-preset", "slow", "-crf", str(crf), "-movflags", "+faststart", "-an", str(dest),
        ])
        size = dest.stat().st_size
        if size <= MAX_BYTES:
            return size
        if crf >= 34:
            sys.exit(f"{dest.name} is still {size / 1e6:.1f} MB at CRF {crf}; shorten the demo script")
        crf += 2


def main():
    parser = argparse.ArgumentParser(description="Record one compose-lab demo as a LinkedIn-ready clip.")
    parser.add_argument("platform", choices=["android", "ios"])
    parser.add_argument("demo_id")
    parser.add_argument("--label", action="store_true", help="draw the platform name on the stage (for side-by-side clips)")
    parser.add_argument("--landscape", action="store_true", help="the demo runs in landscape: a 16:9 clip (Android only)")
    args = parser.parse_args()
    if args.landscape and args.platform == "ios":
        sys.exit("--landscape records on Android only; the simulator can't be turned from here")

    OUT.mkdir(exist_ok=True)
    suffix = "-labeled" if args.label else ""
    raw = OUT / f"{args.demo_id}-{args.platform}-raw.{'mp4' if args.platform == 'android' else 'mov'}"
    dest = OUT / f"{args.demo_id}-{args.platform}{suffix}.mp4"
    try:
        if args.platform == "android":
            start, done = record_android(args.demo_id, args.label, raw, args.landscape)
        else:
            start, done = record_ios(args.demo_id, args.label, raw)
    except SystemExit:
        raw.unlink(missing_ok=True)
        raise
    size = encode(raw, start, done, dest, args.landscape)
    raw.unlink()
    print(f"{dest.relative_to(ROOT)}  {done - start:.1f}s  {size / 1e6:.1f} MB")


if __name__ == "__main__":
    main()
