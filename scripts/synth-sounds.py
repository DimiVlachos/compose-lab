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


# ---------- Paper bag ----------

def crinkles(rng, n, rate, per_second, bands, envelope):
    """Kraft paper crinkling: a scatter of tiny creases, each a few milliseconds of noise rung
    through one of the paper's resonances, louder or softer at random, and closer together
    where [envelope] is high."""
    track = [0.0] * n
    grain_cache = {}
    t = 0.0
    while True:
        gap = rng.expovariate(per_second)
        t += gap
        i = seconds(t, rate)
        if i >= n:
            break
        if rng.random() > envelope(i / n):
            continue
        centre = rng.choice(bands) * rng.uniform(0.8, 1.25)
        length = seconds(rng.uniform(0.002, 0.009), rate)
        key = (round(centre, -2), length)
        if key not in grain_cache:
            g = mul(bandpass(noise(length + 64, rng), centre, 2.2, rate)[64:], env_exp(length, rate, length / rate / 3, 0.0003))
            grain_cache[key] = normalise(g, 1.0)
        grain = grain_cache[key]
        place(track, scale(grain, rng.uniform(0.15, 1.0) ** 1.6), i)
    return track


def bag_drop(rng, rate):
    """Something dropped into a kraft grocery bag: the stiff mouth crinkles as it passes, the
    sides rustle against it, and it lands on the folded floor with a soft papery thump that the
    bag's body booms back."""
    n = seconds(0.34, rate)
    crinkle = crinkles(rng, n, rate, 900, [1800, 3200, 5400, 7600], lambda u: math.exp(-u * 5) * (0.35 + 0.65 * (u > 0.03)))
    rustle_env = [math.sin(math.pi * min(1.0, i / seconds(0.2, rate))) ** 2 if i < seconds(0.2, rate) else 0.0 for i in range(n)]
    rustle = mul(bandpass(noise(n, rng), 850, 0.8, rate), rustle_env)
    land = seconds(0.055, rate)
    m = seconds(0.16, rate)
    glide = [math.sin(2 * math.pi * (105 - 30 * i / m) * i / rate) * math.exp(-i / (0.035 * rate)) for i in range(m)]
    floor = mul(lowpass(noise(m, rng), 380, rate), env_exp(m, rate, 0.018))
    boom = damped_sine(m, rate, 190, 0.05)
    track = mix(scale(crinkle, 0.55), scale(rustle, 0.5))
    place(track, scale(mix(scale(glide, 0.9), scale(floor, 1.3), scale(boom, 0.25)), 0.9), land)
    # The crinkle the landing shakes from the walls.
    shake = crinkles(rng, m, rate, 500, [2600, 4400], lambda u: math.exp(-u * 7))
    place(track, scale(shake, 0.35), land + seconds(0.01, rate))
    return fade(normalise(track, 0.82), rate, 0.002, 0.04)


def bag_lift(rng, rate):
    """Something lifted out of the bag: it slides up against the paper, a dry rising scrape with
    the mouth crinkling as it clears."""
    n = seconds(0.3, rate)
    sweep = [0.0] * n
    src = noise(n, rng)
    # A band that rises as the thing slides up and out.
    step = seconds(0.01, rate)
    for start in range(0, n, step):
        u = start / n
        chunk = bandpass(src[max(0, start - 64):start + step], 1100 + 2600 * u, 1.1, rate)[-min(step, n - start):]
        for k, x in enumerate(chunk):
            sweep[start + k] = x
    shape = [math.sin(math.pi * min(1.0, i / n)) ** 1.4 for i in range(n)]
    crinkle = crinkles(rng, n, rate, 700, [2400, 4200, 6400], lambda u: 0.2 + 0.8 * max(0.0, (u - 0.45) / 0.55))
    track = mix(scale(mul(sweep, shape), 0.6), scale(crinkle, 0.5))
    return fade(normalise(track, 0.62), rate, 0.01, 0.05)


def bag_tip(rng, rate):
    """The bag tipped toward you to look inside: a slow sweep of stiff paper bending, a broad
    rustle that swells and settles, with creases popping along it."""
    n = seconds(0.46, rate)
    swell = [math.sin(math.pi * min(1.0, i / n)) ** 1.8 for i in range(n)]
    body = mul(mix(scale(bandpass(noise(n, rng), 700, 0.6, rate), 0.8), scale(bandpass(noise(n, rng), 2600, 0.9, rate), 0.5)), swell)
    crinkle = crinkles(rng, n, rate, 600, [1600, 3000, 5200], lambda u: math.sin(math.pi * u) ** 1.2)
    track = mix(scale(body, 0.6), scale(crinkle, 0.45))
    return fade(normalise(track, 0.55), rate, 0.02, 0.06)
# ---------- Star stickers ----------

def stick_slip(rng, rate, n, start, end, count, lo, hi, gain_from, gain_to):
    """A run of tiny releases, as adhesive lets go in stick-slip jumps: each a short band of
    noise ringing at its own pitch, closer together and higher as the peel speeds up."""
    track = [0.0] * n
    s0, s1 = seconds(start, rate), seconds(end, rate)
    for k in range(count):
        u = k / max(1, count - 1)
        # Jumps bunch up towards the end of the run: the peel front accelerates.
        at = s0 + int((s1 - s0) * (u ** 0.7)) + rng.randint(-seconds(0.002, rate), seconds(0.002, rate))
        m = seconds(rng.uniform(0.0015, 0.004), rate)
        f = rng.uniform(lo, hi) * (1 + 0.6 * u)
        click = mul(bandpass(noise(m, rng), f, 3.0, rate), env_exp(m, rate, m / rate / 3, 0.0002))
        g = (gain_from + (gain_to - gain_from) * u) * rng.uniform(0.35, 1.0)
        place(track, scale(click, g), at)
    return track


def sticker_peel(rng, rate):
    """A foil star peeled off its liner: the tack crackling as it lets go in quick stick-slip
    jumps that rise and speed up, a thin tearing hiss under them, and the freed paper flicking."""
    n = seconds(0.3, rate)
    crackle = stick_slip(rng, rate, n, 0.004, 0.2, 46, 1800, 4200, 0.5, 1.0)
    hiss_src = noise(n, rng)
    hiss, chunk = [], seconds(0.01, rate)
    for s in range(0, n, chunk):
        hiss.extend(bandpass(hiss_src[s:s + chunk], 2600 + 3400 * (s / n), 1.1, rate))
    shape = [math.sin(math.pi * min(1.0, i / seconds(0.22, rate))) ** 1.5 if i < seconds(0.22, rate) else 0.0 for i in range(n)]
    hiss = scale(mul(hiss, shape), 0.22)
    flick = [0.0] * n
    m = seconds(0.06, rate)
    place(flick, scale(mul(bandpass(noise(m, rng), 700, 1.4, rate), env_exp(m, rate, 0.016, 0.003)), 0.5), seconds(0.2, rate))
    return fade(normalise(mix(crackle, hiss, flick), 0.6), rate, 0.001, 0.04)


def sticker_press(rng, rate):
    """A star pressed down with a thumb: a soft felt thud through the paper, the adhesive
    squeezing flat, and the faint crinkle of the foil settling into its embossing."""
    n = seconds(0.16, rate)
    thud = mul(lowpass(noise(n, rng), 320, rate), env_exp(n, rate, 0.022, 0.003))
    body = scale(damped_sine(n, rate, 96, 0.035), 0.5)
    squish = scale(mul(bandpass(noise(n, rng), 1300, 2.0, rate), env_exp(n, rate, 0.01, 0.004)), 0.35)
    crinkle = stick_slip(rng, rate, n, 0.006, 0.05, 9, 5200, 8200, 0.18, 0.08)
    return fade(normalise(mix(thud, body, squish, crinkle), 0.7), rate, 0.0008, 0.03)


def sticker_settle(rng, rate):
    """A star laid back on the sheet: a dry pat of paper on paper and a whisper of tack."""
    n = seconds(0.07, rate)
    pat = mul(lowpass(noise(n, rng), 1100, rate), env_exp(n, rate, 0.009, 0.001))
    crisp = scale(mul(bandpass(noise(n, rng), 2900, 1.2, rate), env_exp(n, rate, 0.006, 0.001)), 0.4)
    tack = stick_slip(rng, rate, n, 0.004, 0.03, 5, 2500, 4000, 0.12, 0.06)
    return fade(normalise(mix(pat, crisp, tack), 0.4), rate, 0.0005, 0.015)


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
    bag = random.Random(1021)
    write("bag_drop", bag_drop(bag, fast), fast)
    write("bag_lift", bag_lift(bag, fast), fast)
    write("bag_tip", bag_tip(bag, fast), fast)
    sticker = random.Random(1021)
    write("sticker_peel", sticker_peel(sticker, fast), fast)
    write("sticker_press", sticker_press(sticker, fast), fast)
    write("sticker_settle", sticker_settle(sticker, fast), fast)


if __name__ == "__main__":
    main()
