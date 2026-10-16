#!/usr/bin/env python3
"""Synthesize the short sound clips the demos play, as mono 16-bit WAV files.

Usage: scripts/synth-sounds.py

Every clip is built from noise, damped sines and simple filters, with a fixed
seed, so running the script again gives the same files. Nothing is recorded or
sampled, which keeps the clips free of any licence. The output goes to
composeApp/src/commonMain/composeResources/files/sounds/.
"""
import math
import pathlib
import random
import struct
import wave

ROOT = pathlib.Path(__file__).resolve().parent.parent
OUT = ROOT / "composeApp/src/commonMain/composeResources/files/sounds"


def seconds(duration, rate):
    return int(duration * rate)


def noise(n, rng):
    return [rng.uniform(-1.0, 1.0) for _ in range(n)]


def lowpass(signal, cutoff, rate):
    """One-pole low-pass."""
    a = math.exp(-2.0 * math.pi * cutoff / rate)
    out, y = [], 0.0
    for x in signal:
        y = (1 - a) * x + a * y
        out.append(y)
    return out


def highpass(signal, cutoff, rate):
    low = lowpass(signal, cutoff, rate)
    return [x - l for x, l in zip(signal, low)]


def bandpass(signal, centre, q, rate):
    """RBJ biquad band-pass, constant peak gain."""
    w = 2 * math.pi * centre / rate
    alpha = math.sin(w) / (2 * q)
    b0, b2 = alpha, -alpha
    a0, a1, a2 = 1 + alpha, -2 * math.cos(w), 1 - alpha
    out, x1, x2, y1, y2 = [], 0.0, 0.0, 0.0, 0.0
    for x in signal:
        y = (b0 * x + b2 * x2 - a1 * y1 - a2 * y2) / a0
        x2, x1, y2, y1 = x1, x, y1, y
        out.append(y)
    return out


def env_exp(n, rate, decay, attack=0.0015):
    """A fast attack and an exponential decay (decay = time to fall by e)."""
    a = max(1, seconds(attack, rate))
    return [(i / a) if i < a else math.exp(-(i - a) / (decay * rate)) for i in range(n)]


def damped_sine(n, rate, freq, decay, phase=0.0):
    return [math.sin(2 * math.pi * freq * i / rate + phase) * math.exp(-i / (decay * rate)) for i in range(n)]


def mix(*tracks):
    n = max(len(t) for t in tracks)
    return [sum(t[i] if i < len(t) else 0.0 for t in tracks) for i in range(n)]


def scale(signal, gain):
    return [x * gain for x in signal]


def mul(a, b):
    return [x * y for x, y in zip(a, b)]


def place(track, signal, start):
    for i, x in enumerate(signal):
        if 0 <= start + i < len(track):
            track[start + i] += x


def fade(signal, rate, fade_in=0.004, fade_out=0.01):
    n, out = len(signal), list(signal)
    fi, fo = max(1, seconds(fade_in, rate)), max(1, seconds(fade_out, rate))
    for i in range(min(fi, n)):
        out[i] *= i / fi
    for i in range(min(fo, n)):
        out[n - 1 - i] *= i / fo
    return out


def normalise(signal, peak=0.89):
    m = max(1e-9, max(abs(x) for x in signal))
    return [x * peak / m for x in signal]


def write(name, signal, rate):
    OUT.mkdir(parents=True, exist_ok=True)
    path = OUT / f"{name}.wav"
    with wave.open(str(path), "wb") as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(rate)
        w.writeframes(b"".join(struct.pack("<h", int(max(-1.0, min(1.0, x)) * 32767)) for x in signal))
    print(f"{path.relative_to(ROOT)}  {path.stat().st_size // 1024} KB")


# ---------- Typewriter ----------

def strike(rng, rate, body_freq, ring_freq):
    """A type bar hitting the platen: a sharp click, a wooden thunk and a faint metal ring."""
    n = seconds(0.11, rate)
    click = mul(highpass(noise(n, rng), 2500, rate), env_exp(n, rate, 0.004, 0.0004))
    thunk = mul(lowpass(noise(n, rng), 900, rate), env_exp(n, rate, 0.018))
    body = damped_sine(n, rate, body_freq, 0.022)
    ring = mix(damped_sine(n, rate, ring_freq, 0.03), scale(damped_sine(n, rate, ring_freq * 1.47, 0.02), 0.6))
    return fade(normalise(mix(scale(click, 1.0), scale(thunk, 1.6), scale(body, 0.5), scale(ring, 0.07))), rate, 0.0003, 0.02)


def bell(rate):
    """The margin bell: inharmonic partials of a small struck dome."""
    n = seconds(1.3, rate)
    partials = [(1.0, 1.0, 0.9), (2.76, 0.45, 0.5), (5.40, 0.25, 0.28), (8.93, 0.12, 0.16)]
    f0 = 2150
    tone = mix(*[scale(damped_sine(n, rate, f0 * r, d), a) for r, a, d in partials])
    tick = mul(highpass(noise(n, random.Random(7)), 3000, rate), env_exp(n, rate, 0.002, 0.0003))
    return fade(normalise(mix(tone, scale(tick, 0.4))), rate, 0.0003, 0.05)


def carriage_return(rng, rate):
    """The lever pushed: an accelerating zip of escapement clicks, then a bang on the stop."""
    n = seconds(0.55, rate)
    track = [0.0] * n
    t, gap = 0.0, 0.034
    while t < 0.40:
        c = seconds(0.012, rate)
        click = mul(bandpass(noise(c, rng), 3200, 2.5, rate), env_exp(c, rate, 0.003, 0.0003))
        place(track, scale(click, 0.55), seconds(t, rate))
        t += gap
        gap = max(0.011, gap * 0.9)
    m = seconds(0.14, rate)
    bang = mix(scale(mul(lowpass(noise(m, rng), 1200, rate), env_exp(m, rate, 0.03)), 1.8), scale(damped_sine(m, rate, 120, 0.05), 0.8))
    place(track, bang, seconds(0.41, rate))
    slide = mul(bandpass(noise(n, rng), 900, 0.8, rate), [0.12 * math.sin(math.pi * min(1, i / seconds(0.41, rate))) for i in range(n)])
    return fade(normalise(mix(track, slide)), rate, 0.002, 0.03)


def ratchet_tick(rng, rate):
    n = seconds(0.03, rate)
    click = mul(bandpass(noise(n, rng), 2600, 3.0, rate), env_exp(n, rate, 0.004, 0.0003))
    return fade(normalise(click, 0.7), rate, 0.0002, 0.006)


def sheet_release(rng, rate):
    """The sheet pulled free: a paper swish that rises and a soft flap at the end."""
    n = seconds(0.42, rate)
    swell = [math.sin(math.pi * min(1.0, i / n)) ** 1.6 for i in range(n)]
    swish = mul(bandpass(noise(n, rng), 2400, 0.7, rate), swell)
    m = seconds(0.06, rate)
    flap = mul(lowpass(noise(m, rng), 700, rate), env_exp(m, rate, 0.012))
    track = list(swish)
    place(track, scale(flap, 1.4), seconds(0.33, rate))
    return fade(normalise(track, 0.8), rate, 0.01, 0.04)


def tape(rng, rate):
    """Correction tape lifting a letter: a short dry scratch."""
    n = seconds(0.14, rate)
    grain = [x * (0.5 + 0.5 * rng.random()) for x in bandpass(noise(n, rng), 4200, 1.2, rate)]
    shape = [math.sin(math.pi * i / n) for i in range(n)]
    return fade(normalise(mul(grain, shape), 0.6), rate, 0.004, 0.02)


# ---------- Smoke ----------

def burn_loop(rng, rate):
    """A seamless loop of soft hiss and sparse crackles; its two halves are cross-faded so it repeats cleanly."""
    n = seconds(1.6, rate)
    total = n + seconds(0.3, rate)
    hiss = lowpass(highpass(noise(total, rng), 500, rate), 5000, rate)
    hiss = [x * (0.75 + 0.25 * math.sin(2 * math.pi * 0.9 * i / rate)) for i, x in enumerate(hiss)]
    crackle = [0.0] * total
    for _ in range(46):
        start = rng.randrange(total - 400)
        c = seconds(rng.uniform(0.002, 0.007), rate)
        pop = mul(highpass(noise(c, rng), 1800, rate), env_exp(c, rate, 0.0015, 0.0002))
        place(crackle, scale(pop, rng.uniform(0.3, 1.0)), start)
    raw = mix(scale(hiss, 0.5), scale(crackle, 0.9))
    x = seconds(0.3, rate)
    loop = raw[:n]
    for i in range(x):
        w = i / x
        loop[i] = raw[n + i] * (1 - w) + loop[i] * w
    return normalise(loop, 0.7)


def mist_whoosh(rng, rate):
    """Archive: a soft, airy swell that drifts up in pitch."""
    n = seconds(0.75, rate)
    base = noise(n, rng)
    out, chunk = [], seconds(0.025, rate)
    for s in range(0, n, chunk):
        f = 500 + 1300 * (s / n)
        out.extend(bandpass(base[s:s + chunk], f, 0.9, rate))
    shape = [math.sin(math.pi * min(1.0, i / n)) ** 1.3 for i in range(n)]
    return fade(normalise(mul(out, shape), 0.6), rate, 0.02, 0.05)


def inhale(rng, rate):
    """Undo: a breath drawn in, the smoke gathering back, rising to a soft stop."""
    n = seconds(0.6, rate)
    base = noise(n, rng)
    out, chunk = [], seconds(0.02, rate)
    for s in range(0, n, chunk):
        f = 400 + 2200 * (s / n) ** 1.5
        out.extend(bandpass(base[s:s + chunk], f, 1.1, rate))
    shape = [(i / n) ** 1.8 * (1 - max(0.0, (i / n - 0.92) / 0.08)) for i in range(n)]
    return fade(normalise(mul(out, shape), 0.6), rate, 0.01, 0.02)


def card_tick(rng, rate):
    """An index card flicking over the top of the wheel: a dry paper tick with a soft flap."""
    n = seconds(0.07, rate)
    tick = mul(highpass(noise(n, rng), 2400, rate), env_exp(n, rate, 0.006))
    flap = scale(mul(bandpass(noise(n, rng), 900, 1.2, rate), env_exp(n, rate, 0.02, attack=0.004)), 0.5)
    return fade(normalise(mix(tick, flap), 0.55), rate, 0.001, 0.01)


def main():
    rng = random.Random(2026)
    fast, soft = 44100, 22050
    for i, (body, ring) in enumerate([(165, 2600), (148, 2380), (182, 2850)], start=1):
        write(f"strike_{i}", strike(rng, fast, body, ring), fast)
    write("bell", bell(fast), fast)
    write("carriage_return", carriage_return(rng, fast), fast)
    write("ratchet_tick", ratchet_tick(rng, fast), fast)
    write("sheet_release", sheet_release(rng, soft), soft)
    write("tape", tape(rng, fast), fast)
    write("burn_loop", burn_loop(rng, soft), soft)
    write("mist_whoosh", mist_whoosh(rng, soft), soft)
    write("inhale", inhale(rng, soft), soft)
    write("card_tick", card_tick(rng, fast), fast)


if __name__ == "__main__":
    main()
