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


def globe_swish(rng, rate):
    """The snow globe shaken: water sloshing behind glass, a low swell with a glassy edge."""
    n = seconds(0.55, rate)
    base = noise(n, rng)
    out, chunk = [], seconds(0.02, rate)
    for s in range(0, n, chunk):
        f = 380 + 520 * math.sin(math.pi * s / n)
        out.extend(bandpass(base[s:s + chunk], f, 1.4, rate))
    shape = [math.sin(math.pi * min(1.0, i / n)) ** 1.6 for i in range(n)]
    glass = scale(damped_sine(n, rate, 2350, 0.18), 0.05)
    return fade(normalise(mix(mul(out, shape), glass), 0.5), rate, 0.02, 0.06)


def music_box_phrase(rate):
    """A music box's comb plucked: five notes of a gentle phrase, each a bright decaying tine."""
    notes = [(0.00, 1046.5), (0.22, 1318.5), (0.44, 1568.0), (0.66, 1318.5), (0.92, 1760.0)]
    n = seconds(2.2, rate)
    out = [0.0] * n
    for start, f in notes:
        m = seconds(1.2, rate)
        tine = mix(damped_sine(m, rate, f, 0.45), scale(damped_sine(m, rate, f * 2.76, 0.12), 0.25))
        place(out, tine, seconds(start, rate))
    return fade(normalise(out, 0.55), rate, 0.002, 0.2)


def card_tick(rng, rate):
    """An index card flicking over the top of the wheel: a dry paper tick with a soft flap."""
    n = seconds(0.07, rate)
    tick = mul(highpass(noise(n, rng), 2400, rate), env_exp(n, rate, 0.006))
    flap = scale(mul(bandpass(noise(n, rng), 900, 1.2, rate), env_exp(n, rate, 0.02, attack=0.004)), 0.5)
    return fade(normalise(mix(tick, flap), 0.55), rate, 0.001, 0.01)


def book_slide(rng, rate):
    """A book drawn off the shelf: cloth sliding on wood, a band of noise that rises as it frees."""
    n = seconds(0.3, rate)
    base = noise(n, rng)
    out, chunk = [], seconds(0.015, rate)
    for s in range(0, n, chunk):
        f = 700 + 1300 * (s / n)
        out.extend(bandpass(base[s:s + chunk], f, 0.9, rate))
    shape = [math.sin(math.pi * min(1.0, i / n)) ** 1.2 for i in range(n)]
    return fade(normalise(mul(out, shape), 0.5), rate, 0.01, 0.05)


def book_open(rng, rate):
    """The cover laid open: a low board thump under a breath of paper."""
    n = seconds(0.18, rate)
    thump = mul(lowpass(noise(n, rng), 260, rate), env_exp(n, rate, 0.03, attack=0.002))
    body = scale(damped_sine(n, rate, 110, 0.05), 0.35)
    paper = scale(mul(bandpass(noise(n, rng), 3000, 0.8, rate), env_exp(n, rate, 0.05, attack=0.01)), 0.3)
    return fade(normalise(mix(thump, body, paper), 0.6), rate, 0.001, 0.03)


def page_turn(rng, rate):
    """A leaf flipped over its spine: a soft rush of air off the paper that swells and falls away."""
    n = seconds(0.22, rate)
    air = mul(bandpass(noise(n, rng), 1400, 0.7, rate), env_exp(n, rate, 0.07, attack=0.04))
    edge = scale(mul(highpass(noise(n, rng), 3500, rate), env_exp(n, rate, 0.04, attack=0.03)), 0.25)
    return fade(normalise(mix(air, edge), 0.45), rate, 0.004, 0.05)


def page_land(rng, rate):
    """A leaf lying down on the pages under it: a short, soft pat of paper."""
    n = seconds(0.05, rate)
    pat = mul(lowpass(noise(n, rng), 900, rate), env_exp(n, rate, 0.008, attack=0.001))
    crisp = scale(mul(bandpass(noise(n, rng), 2500, 1.0, rate), env_exp(n, rate, 0.005, attack=0.001)), 0.3)
    return fade(normalise(mix(pat, crisp), 0.4), rate, 0.0005, 0.01)


def ring_tick(rng, rate):
    """A book settling at the front of the ring: a short wooden click."""
    n = seconds(0.025, rate)
    click = mix(
        mul(bandpass(noise(n, rng), 2400, 4.0, rate), env_exp(n, rate, 0.003, 0.0002)),
        scale(damped_sine(n, rate, 2400, 0.004), 0.4),
    )
    return fade(normalise(click, 0.45), rate, 0.0002, 0.005)


# ---------- Mailbox ----------

def tin_ring(n, rate, f0, decay, detune=2.5):
    """A thin galvanized panel ringing: inharmonic plate modes, each a close pair that beats."""
    modes = [(1.0, 1.0, 1.0), (1.59, 0.62, 0.8), (2.14, 0.45, 0.62), (2.65, 0.3, 0.5), (3.48, 0.2, 0.36), (4.91, 0.1, 0.24)]
    out = []
    for r, a, d in modes:
        f = f0 * r
        out.append(scale(damped_sine(n, rate, f, decay * d), a * 0.6))
        out.append(scale(damped_sine(n, rate, f + detune * r, decay * d, 1.3), a * 0.4))
    return mix(*out)


def tin_tunk(rng, rate):
    """The mailbox's door or flag landing on tin: a dull knock, then the box rings and beats briefly."""
    n = seconds(0.7, rate)
    knock = mul(bandpass(noise(n, rng), 1100, 1.6, rate), env_exp(n, rate, 0.012, 0.0006))
    thump = damped_sine(n, rate, 170, 0.035)
    ring = tin_ring(n, rate, 560, 0.22)
    rattle = mul(highpass(noise(n, rng), 3500, rate), env_exp(n, rate, 0.03, 0.002))
    return fade(normalise(mix(scale(knock, 1.2), scale(thump, 0.7), scale(ring, 0.55), scale(rattle, 0.12)), 0.8), rate, 0.0003, 0.06)


def flag_twang(rng, rate):
    """The flag swinging up: its spring-steel arm twangs with a wobble, over a light knock on the stop."""
    n = seconds(0.62, rate)
    out, phase = [], 0.0
    for i in range(n):
        t = i / rate
        glide = 330 * (400 / 330) ** (t / 0.06) if t < 0.06 else 400 * (350 / 400) ** min(1.0, (t - 0.06) / 0.34)
        f = glide + 18 * math.sin(2 * math.pi * 15 * t)
        phase += 2 * math.pi * f / rate
        tone = math.sin(phase) + 0.35 * math.sin(2 * phase + 0.4) + 0.12 * math.sin(3 * phase)
        out.append(tone * min(1.0, t / 0.004) * math.exp(-t / 0.16))
    ping = scale(damped_sine(n, rate, 1760, 0.06), 0.25)
    knock = mul(bandpass(noise(n, rng), 1300, 2.0, rate), env_exp(n, rate, 0.01, 0.0005))
    ring = tin_ring(n, rate, 610, 0.12)
    return fade(normalise(mix(scale(out, 0.55), ping, scale(knock, 0.6), scale(ring, 0.25)), 0.7), rate, 0.0003, 0.05)


def door_creak(rng, rate):
    """The door swinging down on its hinge: stick-slip pulses quickening through the hinge's resonances."""
    n = seconds(0.36, rate)
    pulses, t = [0.0] * n, 0.0
    while True:
        p = t / 0.36
        hz = 38 + 110 * p * p
        t += (1 / hz) * rng.uniform(0.82, 1.18)
        i = int(t * rate)
        if i >= n:
            break
        pulses[i] = rng.uniform(0.6, 1.0)
    body = mix(scale(bandpass(pulses, 880, 9.0, rate), 1.0), scale(bandpass(pulses, 2150, 7.0, rate), 0.55), scale(bandpass(pulses, 430, 5.0, rate), 0.4))
    swell = [math.sin(math.pi * min(1.0, i / n)) ** 0.7 for i in range(n)]
    air = mul(bandpass(noise(n, rng), 1500, 1.2, rate), [s * 0.04 for s in swell])
    return fade(normalise(mix(mul(body, swell), air), 0.45), rate, 0.004, 0.04)


def envelope_slip(rng, rate):
    """An envelope sliding in over tin: dry paper hiss that swells and stops with a soft pat."""
    n = seconds(0.26, rate)
    hiss = highpass(bandpass(noise(n, rng), 3200, 0.8, rate), 1800, rate)
    swell = [math.sin(math.pi * i / (0.85 * n)) ** 1.5 if i < 0.85 * n else 0.0 for i in range(n)]
    grain = [1.0 + 0.5 * math.sin(2 * math.pi * 47 * i / rate) for i in range(n)]
    pat = [0.0] * n
    m = seconds(0.04, rate)
    place(pat, mul(lowpass(noise(m, rng), 1400, rate), env_exp(m, rate, 0.007, 0.0008)), seconds(0.215, rate))
    return fade(normalise(mix(mul(mul(hiss, swell), grain), scale(pat, 1.6)), 0.4), rate, 0.004, 0.02)


def mail_tick(rate):
    """The count on the flag ticking: a small bright tick off the tin."""
    n = seconds(0.07, rate)
    tick = mix(damped_sine(n, rate, 2350, 0.012), scale(damped_sine(n, rate, 3870, 0.007), 0.5), scale(damped_sine(n, rate, 1180, 0.02), 0.3))
    return fade(normalise(tick, 0.35), rate, 0.0003, 0.01)


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
    write("globe_swish", globe_swish(rng, soft), soft)
    write("music_box_phrase", music_box_phrase(soft), soft)


    # Its own seed, so the clips added after it on the shared rng stay the same.
    write("card_tick", card_tick(random.Random(1016), fast), fast)
    shelf = random.Random(1018)
    write("book_slide", book_slide(shelf, soft), soft)
    write("book_open", book_open(shelf, fast), fast)
    # The draws of the page flip the leaf sounds replaced, so the ring tick after them stays the same.
    noise(seconds(0.14, fast), shelf)
    noise(seconds(0.14, fast), shelf)
    write("ring_tick", ring_tick(shelf, fast), fast)
    leaf = random.Random(1019)
    write("page_turn", page_turn(leaf, fast), fast)
    write("page_land", page_land(leaf, fast), fast)
    tin = random.Random(1021)
    write("tin_tunk", tin_tunk(tin, fast), fast)
    write("flag_twang", flag_twang(tin, fast), fast)
    write("door_creak", door_creak(tin, fast), fast)
    write("envelope_slip", envelope_slip(tin, fast), fast)
    write("mail_tick", mail_tick(fast), fast)


if __name__ == "__main__":
    main()
