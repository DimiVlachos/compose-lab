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


# ---------- Soft toolkit (shared by the demos' clips; keep this block identical in every branch) ----------
# Real objects heard close by have no instant clicks, little energy up high and a small room around
# them. These helpers give a clip those three things and a quieter peak than normalise's.

SOFT_PEAK = 0.56


def soft_env(n, rate, attack, decay):
    """A raised-cosine rise over attack seconds, then an exponential fall (decay = time to fall by e)."""
    a = max(1, seconds(attack, rate))
    return [0.5 - 0.5 * math.cos(math.pi * i / a) if i < a else math.exp(-(i - a) / (decay * rate)) for i in range(n)]


def soft_attack(signal, rate, attack=0.004):
    """Eases the first attack seconds in on a raised cosine, so the clip never starts with a click."""
    a, out = max(1, seconds(attack, rate)), list(signal)
    for i in range(min(a, len(out))):
        out[i] *= 0.5 - 0.5 * math.cos(math.pi * i / a)
    return out


def soft_tilt(signal, rate, cutoff=3200):
    """A gentle high cut: two one-pole low-passes, so the highs fall away smoothly, as through a hand."""
    return lowpass(lowpass(signal, cutoff, rate), cutoff, rate)


def soft_mode(rng, n, rate, freq, q, decay, attack=0.002):
    """A resonance excited by a short noise burst: a wood or ceramic mode that rings a little, never a pure sine."""
    return mul(bandpass(noise(n, rng), freq, q, rate), soft_env(n, rate, attack, decay))


def soft_room(signal, rate, wet=0.12, length=0.09, decay=0.028, seed=7):
    """A tiny room: sparse velvet-noise reflections over length seconds, darkened, mixed in at wet."""
    taps, room = [], random.Random(seed)
    step = length / 48
    for k in range(48):
        t = k * step + room.uniform(0, step)
        taps.append((seconds(0.004 + t, rate), room.choice((-1.0, 1.0)) * math.exp(-t / decay)))
    norm = math.sqrt(sum(g * g for _, g in taps))
    tail = [0.0] * (len(signal) + seconds(length + 0.006, rate))
    for d, g in taps:
        g /= norm
        for i, x in enumerate(signal):
            tail[d + i] += x * g
    tail = lowpass(lowpass(tail, 2400, rate), 2400, rate)
    return [(signal[i] if i < len(signal) else 0.0) + wet * tail[i] for i in range(len(tail))]


def soft_finish(signal, rate, peak=SOFT_PEAK, attack=0.004, release=0.03):
    """The last step for every soft clip: a level of [peak], a raised-cosine start and a faded tail."""
    return fade(soft_attack(normalise(signal, peak), rate, attack), rate, 0.0, release)


# ---------- End of the soft toolkit ----------


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
# ---------- Padlock ----------

def chirp(n, rate, f0, f1, sweep, decay):
    """A damped sine whose pitch glides exponentially from f0 to f1 over sweep seconds."""
    out, phase = [], 0.0
    for i in range(n):
        t = i / rate
        f = f1 if t >= sweep else f0 * (f1 / f0) ** (t / sweep)
        phase += 2 * math.pi * f / rate
        out.append(math.sin(phase) * math.exp(-t / decay))
    return out


def modes(n, rate, partials):
    """A struck metal body: inharmonic partials (freq, gain, decay), each ringing down on its own."""
    return mix(*[scale(damped_sine(n, rate, f, d, phase=f * 0.37), a) for f, a, d in partials])


def brass_impact(rng, rate, n, weight):
    """One hit of the shackle on the brass body: a hard tick, a thud of mass, and the block's ring."""
    tick = mul(highpass(noise(n, rng), 2500, rate), env_exp(n, rate, 0.0018, 0.0002))
    thud = mul(lowpass(noise(n, rng), 600, rate), env_exp(n, rate, 0.028))
    body = chirp(n, rate, 150, 72, 0.22, 0.085)
    block = modes(n, rate, [(1180, 0.6, 0.12), (1734, 0.45, 0.09), (2611, 0.3, 0.07), (3487, 0.18, 0.05), (4953, 0.1, 0.03)])
    return mix(scale(tick, 0.9), scale(thud, 1.5 * weight), scale(body, 0.75 * weight), scale(block, 0.32))


def padlock_clunk(rng, rate):
    """The shackle dropping home: the latch snapping, a heavy brass hit, a small bounce and a ring."""
    n = seconds(0.6, rate)
    track = [0.0] * n
    latch = mix(
        mul(bandpass(noise(n, rng), 4200, 5.0, rate), env_exp(n, rate, 0.0015, 0.0002)),
        scale(damped_sine(n, rate, 4200, 0.004), 0.3),
    )
    place(track, scale(latch, 0.35), 0)
    place(track, brass_impact(rng, rate, n, 1.0), seconds(0.006, rate))
    # The body bounces once on the shackle and lands again, lighter.
    place(track, scale(brass_impact(rng, rate, n, 0.5), 0.34), seconds(0.044, rate))
    # The hardened steel shackle sings on after the brass has stopped.
    place(track, scale(modes(n, rate, [(2213, 0.2, 0.34), (6094, 0.07, 0.18), (11950, 0.02, 0.06)]), 0.55), seconds(0.006, rate))
    return fade(normalise(track), rate, 0.0002, 0.06)


def padlock_pop(rng, rate):
    """The shackle springing open: the latch letting go, and the spring's short twang."""
    n = seconds(0.24, rate)
    release = mix(
        mul(bandpass(noise(n, rng), 3200, 5.0, rate), env_exp(n, rate, 0.0016, 0.0002)),
        scale(damped_sine(n, rate, 3200, 0.005), 0.35),
    )
    twang = mix(chirp(n, rate, 640, 420, 0.09, 0.05), scale(chirp(n, rate, 1920, 1260, 0.09, 0.03), 0.18))
    coil = mul(damped_sine(n, rate, 180, 0.04), [0.5 + 0.5 * math.sin(2 * math.pi * 31 * i / rate) for i in range(n)])
    ring = damped_sine(n, rate, 2213, 0.08)
    track = mix(scale(release, 1.0), scale(twang, 0.45), scale(coil, 0.25), scale(ring, 0.12))
    return fade(normalise(track, 0.7), rate, 0.0002, 0.03)


def padlock_rattle(rng, rate):
    """A loose shackle knocking in its holes: three small metal ticks, each quieter."""
    n = seconds(0.1, rate)
    track = [0.0] * n
    for at, freq, gain in [(0.0, 3400, 1.0), (0.024, 2900, 0.7), (0.052, 3700, 0.45)]:
        tick = mix(
            mul(bandpass(noise(n, rng), freq, 7.0, rate), env_exp(n, rate, 0.004, 0.0002)),
            scale(damped_sine(n, rate, freq * 0.65, 0.008), 0.4),
        )
        place(track, scale(tick, gain), seconds(at, rate))
    return fade(normalise(track, 0.5), rate, 0.0002, 0.01)


def padlock_swing(rng, rate):
    """The keyhole cover turning on its rivet: a soft brass scrape that ends in a small tick."""
    n = seconds(0.17, rate)
    track = [0.0] * n
    w = seconds(0.075, rate)
    swell = [math.sin(math.pi * i / w) if i < w else 0.0 for i in range(n)]
    scrape = mul(bandpass(noise(n, rng), 1900, 3.0, rate), swell)
    place(track, scale(scrape, 0.5), 0)
    stop = mix(
        mul(bandpass(noise(n, rng), 2600, 6.0, rate), env_exp(n, rate, 0.003, 0.0002)),
        scale(damped_sine(n, rate, 1300, 0.03), 0.25),
    )
    place(track, stop, w)
    return fade(normalise(track, 0.45), rate, 0.002, 0.02)


# ---------- Pencil cup ----------

def pencil_clink(rng, rate):
    """A tool landing in the glazed cup: a dull wooden knock on stoneware and a short, muffled ceramic note."""
    n = seconds(0.24, rate)
    wood = mix(soft_mode(rng, n, rate, 900, 2.5, 0.012, attack=0.004), scale(soft_mode(rng, n, rate, 1700, 3.0, 0.008, attack=0.004), 0.5))
    body = soft_mode(rng, n, rate, 320, 1.8, 0.02, attack=0.005)
    glaze = mix(soft_mode(rng, n, rate, 1850, 22.0, 0.05, attack=0.004), scale(soft_mode(rng, n, rate, 2950, 20.0, 0.03, attack=0.004), 0.4))
    hit = mix(scale(wood, 1.0), scale(body, 0.9), scale(glaze, 0.55))
    return soft_finish(soft_room(soft_tilt(hit, rate, 3000), rate, 0.13), rate, SOFT_PEAK, 0.004, 0.04)


def pencil_lift(rng, rate):
    """A tool leaping out of the cup: a light wooden scuff on the rim, like a pencil drawn out of a mug."""
    n = seconds(0.11, rate)
    scuff = mul(bandpass(noise(n, rng), 1100, 1.4, rate), soft_env(n, rate, 0.012, 0.025))
    tap = soft_mode(rng, n, rate, 650, 2.0, 0.012, attack=0.004)
    return soft_finish(soft_room(soft_tilt(mix(scuff, scale(tap, 0.6)), rate, 2600), rate, 0.1), rate, 0.42, 0.005, 0.03)


def pencil_pick(rng, rate):
    """A tool picked: a soft breath of air as it flies up into its sheet."""
    n = seconds(0.22, rate)
    air = bandpass(lowpass(noise(n, rng), 3000, rate), 1400, 0.8, rate)
    env = [math.sin(math.pi * min(1.0, i / (0.22 * rate))) ** 2 for i in range(n)]
    return soft_finish(soft_room(soft_tilt(mul(air, env), rate, 2800), rate, 0.1), rate, 0.24, 0.02, 0.05)


# ---------- Locket ----------
# Small gold heard close to the ear: the catch and the lid are soft knocks with a short, muffled ring of the gold,
# the photo a quiet glassy note, the strip and the chain light brushes and ticks. Built with the soft toolkit.


def locket_open(rng, rate):
    """The catch letting go: a small muffled click and the lid's short ring of gold."""
    n = seconds(0.22, rate)
    click = mix(soft_mode(rng, n, rate, 2100, 3.0, 0.006, attack=0.006), scale(soft_mode(rng, n, rate, 700, 2.0, 0.012, attack=0.006), 0.6))
    ring = mix(soft_mode(rng, n, rate, 2600, 24.0, 0.06, attack=0.006), scale(soft_mode(rng, n, rate, 3900, 22.0, 0.035, attack=0.006), 0.3))
    return soft_finish(soft_room(soft_tilt(mix(click, scale(ring, 0.45)), rate, 3000), rate, 0.12), rate, 0.19, 0.004, 0.04)


def locket_snap(rng, rate):
    """The lid snapping shut: a soft knock of gold on gold, then the case ringing briefly."""
    n = seconds(0.32, rate)
    knock = mix(soft_mode(rng, n, rate, 1500, 2.5, 0.01, attack=0.006), scale(soft_mode(rng, n, rate, 420, 1.8, 0.02, attack=0.005), 0.8))
    ring = mix(soft_mode(rng, n, rate, 3100, 26.0, 0.08, attack=0.006), scale(soft_mode(rng, n, rate, 1900, 18.0, 0.04, attack=0.006), 0.4))
    return soft_finish(soft_room(soft_tilt(mix(knock, scale(ring, 0.5)), rate, 3000), rate, 0.13), rate, SOFT_PEAK, 0.004, 0.05)


def locket_place(rng, rate):
    """A photo settling behind the glass: a soft glassy note and a breath of air."""
    n = seconds(0.42, rate)
    note = mix(soft_mode(rng, n, rate, 3300, 34.0, 0.12, attack=0.005), scale(soft_mode(rng, n, rate, 4950, 30.0, 0.07, attack=0.005), 0.3))
    air = mul(bandpass(lowpass(noise(n, rng), 3000, rate), 1500, 0.8, rate), soft_env(n, rate, 0.015, 0.03))
    return soft_finish(soft_room(soft_tilt(mix(note, scale(air, 0.25)), rate, 3500), rate, 0.14), rate, 0.2, 0.005, 0.06)


def locket_lift(rng, rate):
    """A photo lifted off the strip: a short, light brush."""
    n = seconds(0.07, rate)
    brush = mul(bandpass(noise(n, rng), 1500, 1.3, rate), soft_env(n, rate, 0.01, 0.018))
    return soft_finish(soft_room(soft_tilt(brush, rate, 2800), rate, 0.1), rate, 0.045, 0.005, 0.02)


def locket_back(rng, rate):
    """A photo sliding home: a lower, longer brush."""
    n = seconds(0.09, rate)
    brush = mul(bandpass(noise(n, rng), 1100, 1.3, rate), soft_env(n, rate, 0.013, 0.028))
    return soft_finish(soft_room(soft_tilt(brush, rate, 2600), rate, 0.1), rate, 0.045, 0.006, 0.025)


def locket_chain(rng, rate):
    """The chain let go: three tiny links ticking, uneven and dying away."""
    n = seconds(0.03, rate)
    track = [0.0] * seconds(0.11, rate)
    for start, freq, gain in ((0.0, 2400, 1.0), (0.026, 2650, 0.7), (0.059, 2900, 0.45)):
        tick = mix(soft_mode(rng, n, rate, freq, 8.0, 0.006, attack=0.005), scale(soft_mode(rng, n, rate, 900, 2.0, 0.005, attack=0.005), 0.5))
        place(track, scale(tick, gain), seconds(start, rate))
    return soft_finish(soft_room(soft_tilt(track, rate, 3000), rate, 0.1), rate, 0.045, 0.003, 0.02)


# ---------- Birdcage ----------

def canary_syllable(rng, rate, f0, f1, dur, trill=34, depth=0.06):
    """One note of a canary: a pitch glide with a fast trill on it, a breath of air at its start, never a pure beep."""
    n = seconds(dur, rate)
    phase, tone = 0.0, []
    for i in range(n):
        t = i / n
        f = f0 * (f1 / f0) ** t * (1 + depth * math.sin(2 * math.pi * trill * i / rate))
        phase += 2 * math.pi * f / rate
        # A little of the second harmonic, so it has a throat behind it.
        tone.append(math.sin(phase) + 0.18 * math.sin(2 * phase + 0.4))
    shape = [math.sin(math.pi * min(1.0, i / n)) ** 1.6 for i in range(n)]
    breath = mul(bandpass(noise(n, rng), (f0 + f1) / 2, 1.4, rate), soft_env(n, rate, 0.004, dur * 0.25))
    return mix(mul(tone, shape), scale(breath, 0.35))


def birdcage_clips(rng, rate):
    """The cage's five sounds: a small canary heard through the bars, linen and a muffled brass ring."""
    chirp = [0.0] * seconds(0.56, rate)
    for i, (f0, f1, start, dur) in enumerate([(1950, 2400, 0.0, 0.075), (2350, 1900, 0.085, 0.065),
                                               (2000, 2450, 0.17, 0.07), (2400, 1950, 0.25, 0.06),
                                               (2050, 2600, 0.34, 0.13)]):
        place(chirp, scale(canary_syllable(rng, rate, f0, f1, dur), 0.85 if i < 4 else 1.0), seconds(start, rate))
    # The cover settling over the cage: a soft fall of cloth, and the brass foot barely touched under it.
    n = seconds(0.5, rate)
    settle = mix(mul(bandpass(lowpass(noise(n, rng), 1400, rate), 420, 0.9, rate), soft_env(n, rate, 0.022, 0.11)),
                 scale(soft_mode(rng, n, rate, 1500, 18, 0.05, attack=0.005), 0.12),
                 scale(soft_mode(rng, n, rate, 320, 3, 0.03, attack=0.006), 0.5))
    # Lifting it: linen drawn up, and the ring above the dome giving a small, dull chink.
    n = seconds(0.4, rate)
    lift = mix(mul(bandpass(lowpass(noise(n, rng), 1600, rate), 700, 1.0, rate), soft_env(n, rate, 0.03, 0.08)),
               [0.0] * seconds(0.24, rate) + scale(soft_mode(rng, seconds(0.16, rate), rate, 1700, 20, 0.04, attack=0.004), 0.16))
    # A message stirring the cloth while muted.
    n = seconds(0.28, rate)
    rustle = mul(bandpass(lowpass(noise(n, rng), 1500, rate), 650, 1.3, rate), soft_env(n, rate, 0.014, 0.045))
    # The hem swinging past.
    n = seconds(0.22, rate)
    swish = mul(bandpass(lowpass(noise(n, rng), 2000, rate), 800, 0.8, rate),
                [math.sin(math.pi * i / n) ** 2 for i in range(n)])
    peaks = {"birdcage_chirp": (chirp, 0.36, 2400, 0.12), "birdcage_settle": (settle, SOFT_PEAK, 1800, 0.12),
             "birdcage_lift": (lift, 0.42, 2200, 0.12), "birdcage_rustle": (rustle, 0.2, 2000, 0.1),
             "birdcage_swish": (swish, 0.27, 2200, 0.1)}
    return {name: soft_finish(soft_room(soft_tilt(c, rate, cut), rate, wet), rate, peak, 0.004, 0.03)
            for name, (c, peak, cut, wet) in peaks.items()}


# ---------- Zipper ----------
# A fabric zipper heard close: the brass is small and wrapped in tape, so every sound is a muffled
# tick with a cloth body under it, never a bright click.


def zip_clack(rng, rate):
    """The pull's tab falling flat on the tape: a soft knock of brass on cloth."""
    n = seconds(0.07, rate)
    track = mix(
        soft_mode(rng, n, rate, 420, 2.2, 0.012, attack=0.006),
        scale(soft_mode(rng, n, rate, 1150, 2.6, 0.008, attack=0.006), 0.6),
        scale(soft_mode(rng, n, rate, 2300, 18, 0.035, attack=0.006), 0.4),
    )
    return soft_finish(soft_room(soft_tilt(track, rate, 2800), rate, 0.11), rate, SOFT_PEAK, 0.006, 0.03)


def zip_tick(rng, rate):
    """The tab bouncing on the tape, or the tag passing a letter: a tiny, dull tick."""
    n = seconds(0.04, rate)
    track = mix(
        soft_mode(rng, n, rate, 1500, 2.8, 0.006, attack=0.007),
        scale(soft_mode(rng, n, rate, 2500, 16, 0.016, attack=0.007), 0.3),
    )
    return soft_finish(soft_room(soft_tilt(track, rate, 2800), rate, 0.1), rate, 0.36, 0.007, 0.02)


def zip_teeth(rng, rate):
    """Teeth passing the pull: a little run of soft grains over a breath of cloth."""
    n = seconds(0.05, rate)
    track = [0.0] * n
    for i in range(4):
        grain = soft_mode(rng, seconds(0.02, rate), rate, 1500 + rng.random() * 800, 2.2, 0.004, attack=0.003)
        place(track, scale(grain, 1.0 - i * 0.12), seconds(0.0015 + i * 0.006 + rng.random() * 0.0015, rate))
    cloth = mul(bandpass(noise(n, rng), 900, 1.2, rate), soft_env(n, rate, 0.006, 0.014))
    track = mix(track, scale(cloth, 0.5))
    return soft_finish(soft_room(soft_tilt(track, rate, 2700), rate, 0.1), rate, 0.36, 0.003, 0.02)


def zip_lift(rng, rate):
    """The pull taken: its tab lifting off the tape, a scuff of cloth and a light tick of brass."""
    n = seconds(0.06, rate)
    scuff = mul(bandpass(noise(n, rng), 1200, 1.3, rate), soft_env(n, rate, 0.01, 0.022))
    track = mix(scuff, scale(soft_mode(rng, n, rate, 2000, 18, 0.025, attack=0.004), 0.3))
    return soft_finish(soft_room(soft_tilt(track, rate, 2800), rate, 0.11), rate, 0.36, 0.004, 0.025)


# ---------- Key hook ----------

def key_tink(rng, rate):
    """Small keys touching on their ring: two muffled brass rings a moment apart, never a pure tone."""
    n = seconds(0.2, rate)
    track = [0.0] * n
    place(track, mix(soft_mode(rng, n, rate, 2300, 24, 0.06, 0.004), scale(soft_mode(rng, n, rate, 3900, 20, 0.03, 0.004), 0.3)), 0)
    place(track, scale(soft_mode(rng, n, rate, 2050, 24, 0.045, 0.004), 0.55), seconds(0.022, rate))
    return soft_finish(soft_room(soft_tilt(track, rate, 3000), rate, 0.12), rate, 0.28, 0.004, 0.03)


def key_clack(rng, rate):
    """A set landing on its hook: a soft knock on the wooden board, the brass hook's short ring, a key after."""
    n = seconds(0.14, rate)
    track = [0.0] * n
    place(track, soft_mode(rng, n, rate, 1100, 2.5, 0.01, 0.004), 0)
    place(track, scale(soft_mode(rng, n, rate, 300, 2.0, 0.014, 0.005), 0.8), 0)
    place(track, scale(soft_mode(rng, n, rate, 1800, 20, 0.04, 0.004), 0.45), 0)
    place(track, scale(soft_mode(rng, n, rate, 2200, 24, 0.03, 0.004), 0.25), seconds(0.015, rate))
    return soft_finish(soft_room(soft_tilt(track, rate, 2800), rate, 0.12), rate, 0.56, 0.004, 0.03)


def key_lift(rng, rate):
    """A set taken off its hook: the ring sliding off the brass, a soft scuff with a faint ring in it."""
    n = seconds(0.08, rate)
    scuff = mul(bandpass(noise(n, rng), 1400, 1.4, rate), soft_env(n, rate, 0.01, 0.025))
    track = mix(scuff, scale(soft_mode(rng, n, rate, 2100, 20, 0.025, 0.004), 0.2))
    return soft_finish(soft_room(soft_tilt(track, rate, 3000), rate, 0.11), rate, 0.21, 0.004, 0.03)


# ---------- Paper fan ----------

def sweep_bandpass(signal, f0, f1, q, dur, rate):
    """RBJ band-pass whose centre glides exponentially from f0 to f1 over dur seconds, as WebAudio's ramp."""
    out, x1, x2, y1, y2 = [], 0.0, 0.0, 0.0, 0.0
    for i, x in enumerate(signal):
        t = min(1.0, i / (dur * rate))
        w = 2 * math.pi * f0 * (f1 / f0) ** t / rate
        alpha = math.sin(w) / (2 * q)
        a0, a1, a2 = 1 + alpha, -2 * math.cos(w), 1 - alpha
        y = (alpha * x - alpha * x2 - a1 * y1 - a2 * y2) / a0
        x2, x1, y2, y1 = x1, x, y1, y
        out.append(y)
    return out


def swell(n, rate, dur):
    """The prototype's sweep envelope: up from silence over a quarter of dur, exponentially, and back down by dur."""
    out, up = [], dur * 0.25
    for i in range(n):
        t = i / rate
        if t < up:
            out.append(0.0001 * (1 / 0.0001) ** (t / up))
        elif t < dur:
            out.append(0.0001 ** ((t - up) / (dur - up)))
        else:
            out.append(0.0)
    return out


def paper_sweep(rng, rate, f0, f1, q, dur, gain):
    """One of the prototype's sweep()s: noise through a gliding band-pass, swelled in and out."""
    n = seconds(dur + 0.02, rate)
    return scale(mul(sweep_bandpass(noise(n, rng), f0, f1, q, dur, rate), swell(n, rate, dur)), gain)


def paperfan_fwap(rng, rate):
    """The fan flicked open: a soft rush of paper through the air, and the leaf filling out with a muffled pop."""
    track = [0.0] * seconds(0.3, rate)
    place(track, paper_sweep(rng, rate, 500, 1800, 0.9, 0.17, 0.32), seconds(0.06, rate))
    n = seconds(0.08, rate)
    pop = mul(bandpass(noise(n, rng), 900, 1.3, rate), soft_env(n, rate, 0.006, 0.018))
    place(track, scale(pop, 0.12), seconds(0.11, rate))
    return soft_finish(soft_room(soft_tilt(track, rate, 2800), rate, 0.11), rate, SOFT_PEAK, 0.004, 0.04)


def paperfan_click(rng, rate):
    """The leading guard settling home: a light knock of lacquered bamboo."""
    n = seconds(0.1, rate)
    knock = mix(soft_mode(rng, n, rate, 1400, 3.0, 0.008, attack=0.004), scale(soft_mode(rng, n, rate, 700, 2.5, 0.012, attack=0.005), 0.7))
    return soft_finish(soft_room(soft_tilt(knock, rate, 2800), rate, 0.11), rate, 0.48, 0.004, 0.03)


def paperfan_swish(rng, rate):
    """The fan starting to fold: a soft, falling swish of paper."""
    swish = paper_sweep(rng, rate, 1600, 600, 0.9, 0.2, 0.16)
    return soft_finish(soft_room(soft_tilt(swish, rate, 2600), rate, 0.1), rate, 0.28, 0.006, 0.04)


def paperfan_clack(rng, rate):
    """The guards meeting as the fan shuts: two small, muffled knocks of bamboo."""
    track = [0.0] * seconds(0.12, rate)
    n = seconds(0.07, rate)
    place(track, mix(soft_mode(rng, n, rate, 1100, 2.5, 0.01, attack=0.004), scale(soft_mode(rng, n, rate, 520, 2.0, 0.014, attack=0.005), 0.6)), 0)
    place(track, scale(soft_mode(rng, n, rate, 1300, 2.5, 0.008, attack=0.004), 0.5), seconds(0.03, rate))
    return soft_finish(soft_room(soft_tilt(track, rate, 2700), rate, 0.11), rate, 0.35, 0.004, 0.03)


# ---------- Sticky notes ----------

def note_grain(rng, rate):
    """One crackle of paper coming away from its glue: a soft band of noise around 1.5 kHz and, now
    and then, a quieter fibre giving way after it."""
    n = seconds(0.05, rate)
    grain = mul(bandpass(noise(n, rng), 1500, 1.3, rate), soft_env(n, rate, 0.005, 0.007))
    fibre = [0.0] * n
    m = seconds(0.03, rate)
    place(fibre, scale(soft_mode(rng, m, rate, 2100, 2.0, 0.004, attack=0.004), 0.3), seconds(0.012, rate))
    return soft_finish(soft_room(soft_tilt(mix(grain, fibre), rate, 2800), rate, 0.08), rate, 0.25, 0.005, 0.006)


def note_tick(rng, rate):
    """The pull crossing the line where letting go takes the note off: a small papery tick, a
    fingernail on card rather than a beep."""
    n = seconds(0.07, rate)
    tick = mix(soft_mode(rng, n, rate, 1100, 6.0, 0.009, attack=0.004), scale(soft_mode(rng, n, rate, 620, 2.5, 0.007, attack=0.004), 0.6))
    return soft_finish(soft_room(soft_tilt(tick, rate, 2600), rate, 0.10), rate, 0.063, 0.004, 0.01)


def note_rip(rng, rate):
    """The note coming off its glue: six soft grains of paper parting, about 18 ms apart."""
    n = seconds(0.16, rate)
    track = [0.0] * n
    for i in range(6):
        d = rng.uniform(0.015, 0.03)
        m = seconds(d + 0.012, rate)
        grain = mul(bandpass(noise(m, rng), rng.uniform(1200, 2200), 1.2, rate), soft_env(m, rate, 0.004, d / 3))
        place(track, scale(grain, 1.0 - i * 0.08), seconds(i * 0.018 + rng.uniform(-0.003, 0.003) + 0.004, rate))
    return soft_finish(soft_room(soft_tilt(track, rate, 2800), rate, 0.10), rate, 0.22, 0.005, 0.02)


def note_stick(rng, rate):
    """The note pressed onto a pad: a soft, close thump of paper on paper, felt more than heard."""
    n = seconds(0.14, rate)
    thud = mul(lowpass(noise(n, rng), 380, rate), soft_env(n, rate, 0.006, 0.03))
    body = soft_mode(rng, n, rate, 120, 3.0, 0.035, attack=0.006)
    paper = soft_mode(rng, n, rate, 900, 2.0, 0.01, attack=0.005)
    return soft_finish(soft_room(soft_tilt(mix(scale(thud, 0.6), scale(body, 0.5), scale(paper, 0.15)), rate, 2600), rate, 0.10), rate, 0.56, 0.006, 0.02)


def note_pat(rng, rate):
    """A note pressed back flat onto the screen: a muffled pat of a fingertip on paper."""
    n = seconds(0.14, rate)
    pat = mul(lowpass(noise(n, rng), 600, rate), soft_env(n, rate, 0.006, 0.028))
    body = soft_mode(rng, n, rate, 240, 2.0, 0.025, attack=0.006)
    return soft_finish(soft_room(soft_tilt(mix(pat, scale(body, 0.4)), rate, 2600), rate, 0.10), rate, 0.35, 0.006, 0.02)


def note_lift(rng, rate):
    """A pad's top note lifting off to go back, or a tapped corner lifting: a light breath of paper."""
    n = seconds(0.07, rate)
    env = [math.sin(math.pi * i / n) ** 2 for i in range(n)]
    flick = mul(bandpass(lowpass(noise(n, rng), 3000, rate), 1300, 1.0, rate), env)
    return soft_finish(soft_room(soft_tilt(flick, rate, 3000), rate, 0.10), rate, 0.17, 0.006, 0.01)


# ---------- Paint roller ----------

def web_tone(n, rate, start, f0, f1, dur, gain, wave="sine"):
    """The prototype's tone(): a pitch gliding exponentially from f0 to f1 over dur, its level
    rising exponentially to gain in 12 ms and falling away to nothing by dur."""
    out = [0.0] * n
    phase = 0.0
    a = seconds(0.012, rate)
    m = seconds(dur, rate)
    s0 = seconds(start, rate)
    for i in range(min(m, n - s0)):
        t = i / rate
        f = f0 * (f1 / f0) ** min(1.0, t / dur)
        phase += 2 * math.pi * f / rate
        if wave == "triangle":
            x = 2 / math.pi * math.asin(math.sin(phase))
        else:
            x = math.sin(phase)
        if i < a:
            g = 0.0001 * (gain / 0.0001) ** (i / a)
        else:
            g = gain * (0.0001 / gain) ** ((i - a) / max(1, m - a))
        out[s0 + i] = x * g
    return out


def roller_dip(rng, rate):
    """The roller rolled in the tray's paint: a low wet bloop falling from 180 to 90 Hz, and a
    smaller one after it."""
    n = seconds(0.2, rate)
    clip = mix(web_tone(n, rate, 0.0, 180, 90, 0.16, 0.14), web_tone(n, rate, 0.05, 320, 140, 0.12, 0.06))
    return soft_finish(soft_room(clip, rate, 0.08), rate, 0.45, 0.003, 0.02)


def roller_set(rng, rate):
    """The roller set down in its tray: a light plastic tick over a soft low knock."""
    n = seconds(0.1, rate)
    clip = mix(web_tone(n, rate, 0.0, 900, 820, 0.05, 0.05, "triangle"), web_tone(n, rate, 0.01, 220, 180, 0.07, 0.06))
    return soft_finish(soft_room(clip, rate, 0.08), rate, 0.25, 0.002, 0.015)


def roller_roll(rng, rate):
    """A seamless loop of the wet nap rolling on the wall: brownish noise with a sticky crackle in
    it, band-passed at 700 Hz, its two ends cross-faded so it repeats cleanly."""
    n = seconds(1.6, rate)
    total = n + seconds(0.3, rate)
    raw, b = [], 0.0
    for _ in range(total):
        b = (b + rng.uniform(-1.0, 1.0) * 0.08) * 0.985
        raw.append(b * 3 + ((rng.random() - 0.5) * 0.9 if rng.random() < 0.004 else 0.0))
    raw = bandpass(raw, 700, 0.7, rate)
    x = seconds(0.3, rate)
    loop = raw[:n]
    for i in range(x):
        w = i / x
        loop[i] = raw[n + i] * (1 - w) + loop[i] * w
# ---------- French press ----------
# A small glass press on the table: the words pouring in and out are soft rushes of band-passed noise
# gliding up or down, as the prototype's WebAudio bursts, with a glassy tink as the mesh lands and
# small glugs as the summary pours. Built with the soft toolkit.

def press_envelope(n, rate, dur):
    """The prototype's burst envelope: up from silence over min(20 ms, dur / 3), exponentially, and down again by dur."""
    out, up = [], min(0.02, dur / 3)
    for i in range(n):
        t = i / rate
        if t < up:
            out.append(0.0001 * (1 / 0.0001) ** (t / up))
        elif t < dur:
            out.append(0.0001 ** ((t - up) / (dur - up)))
        else:
            out.append(0.0)
    return out


def press_burst(rng, rate, f0, q, dur, f1=None):
    """One of the prototype's bursts: noise through a band-pass at f0 (gliding to f1), in its envelope."""
    n = seconds(dur + 0.02, rate)
    signal = noise(n, rng)
    shaped = sweep_bandpass(signal, f0, f1, q, dur, rate) if f1 else bandpass(signal, f0, q, rate)
    return mul(shaped, press_envelope(n, rate, dur))


def press_tone(rate, f0, f1, dur):
    """The prototype's tone(): a sine gliding exponentially from f0 to f1, falling away over dur."""
    n = seconds(dur + 0.02, rate)
    out, phase = [], 0.0
    for i in range(n):
        t = min(1.0, i / (dur * rate))
        phase += 2 * math.pi * f0 * (f1 / f0) ** t / rate
        out.append(math.sin(phase) * (0.0001 ** t if i < dur * rate else 0.0))
    return out


def press_pour(rng, rate):
    """The words pouring into the press: a soft rush of paper and water rising from 500 to 1700 Hz."""
    rush = press_burst(rng, rate, 500, 1.4, 1.1, 1700)
    return soft_finish(soft_room(soft_tilt(rush, rate, 3000), rate, 0.1), rate, 0.5, 0.01, 0.06)


def press_rise(rng, rate):
    """The grounds stirring up and the words pouring out: the same rush, falling from 1700 to 520 Hz."""
    rush = press_burst(rng, rate, 1700, 1.4, 1.0, 520)
    return soft_finish(soft_room(soft_tilt(rush, rate, 3000), rate, 0.1), rate, 0.45, 0.01, 0.06)


def press_tick(rng, rate):
    """A few words landing in the glass: a tiny, bright tick of 20 ms."""
    tick = press_burst(rng, rate, 5600, 3.0, 0.02)
    return soft_finish(soft_room(soft_tilt(tick, rate, 6000), rate, 0.08), rate, 0.2, 0.002, 0.006)


def press_tink(rng, rate):
    """The mesh reaching the bottom: a small glassy tink and a soft knock of the plunger."""
    track = mix(scale(press_tone(rate, 2650, 2630, 0.3), 0.05), scale(press_tone(rate, 3980, 3960, 0.18), 0.025))
    n = seconds(0.11, rate)
    thud = mul(lowpass(lowpass(noise(n, rng), 260, rate), 260, rate), press_envelope(n, rate, 0.09))
    track = mix(track, scale(normalise(thud, 1.0), 0.1))
    return soft_finish(soft_room(soft_tilt(track, rate, 4200), rate, 0.12), rate, SOFT_PEAK, 0.004, 0.05)


def press_glug(rng, rate):
    """The summary pouring from the spout: a little glug, a tone rising 150 to 240 Hz with a wet grain."""
    tone = press_tone(rate, 150, 240, 0.08)
    grain = press_burst(rng, rate, 700, 4.0, 0.06)
    track = mix(scale(tone, 0.1), scale(normalise(grain, 1.0), 0.05))
    return soft_finish(soft_room(soft_tilt(track, rate, 3000), rate, 0.1), rate, 0.5, 0.004, 0.02)


def press_lift(rng, rate):
    """The plunger starting up: a short breath rising from 900 to 2200 Hz."""
    breath = press_burst(rng, rate, 900, 2.0, 0.25, 2200)
    return soft_finish(soft_room(soft_tilt(breath, rate, 3200), rate, 0.1), rate, 0.3, 0.008, 0.04)


def press_hiss(rng, rate):
    """The mesh moving through the coffee, looped: noise low-passed at 520 Hz, its ends crossfaded so it loops."""
    n, x = seconds(2.0, rate), seconds(0.2, rate)
    hiss = soft_tilt(bandpass(noise(n + x, rng), 260, 0.4, rate), rate, 520)
    loop = hiss[x:n]
    for i in range(x):
        a = i / x
        loop[i] = loop[i] * a + hiss[n + i] * (1 - a)
    return normalise(loop, 0.5)
# ---------- Photocopier ----------

def ramp_down(n, rate, gain, dur):
    """WebAudio's exponentialRampToValueAtTime(.0001, dur) from gain: the prototype's every burst and tone."""
    k = math.log(0.0001 / gain)
    m = max(1, seconds(dur, rate))
    return [gain * math.exp(k * min(1.0, i / m)) if i < m else 0.0 for i in range(n)]


def burst(rng, rate, n, freq, q, dur, gain, kind="bandpass"):
    """The prototype's burst(): noise through a filter, ramped down over dur."""
    raw = noise(n, rng)
    if kind == "lowpass":
        sig = lowpass(lowpass(raw, freq, rate), freq, rate)
    elif kind == "highpass":
        sig = highpass(raw, freq, rate)
    else:
        sig = bandpass(raw, freq, q, rate)
    return mul(sig, ramp_down(n, rate, gain, dur))


def glide_tone(rate, n, f0, f1, dur, gain, saw=False):
    """The prototype's tone(): a sine (or a soft triangle) gliding exponentially from f0 to f1, ramped down."""
    out, ph = [], 0.0
    m = max(1, seconds(dur, rate))
    env = ramp_down(n, rate, gain, dur)
    for i in range(n):
        f = f0 * (f1 / f0) ** min(1.0, i / m)
        ph += 2 * math.pi * f / rate
        v = (2 / math.pi) * math.asin(math.sin(ph)) if saw else math.sin(ph)
        out.append(v * env[i])
    return out


def motor(rng, rate, n, dur, gain):
    """The copier's motor: a low filtered rumble with a faint 96 Hz buzz under it, in and out on short ramps."""
    def env(v):
        e = []
        for i in range(n):
            t = i / rate
            if t < 0.08:
                e.append(0.0001 * (v / 0.0001) ** (t / 0.08))
            elif t < max(0.1, dur - 0.14):
                e.append(v)
            elif t < dur:
                u = (t - max(0.1, dur - 0.14)) / (dur - max(0.1, dur - 0.14))
                e.append(v * (0.0001 / v) ** u)
            else:
                e.append(0.0)
        return e
    rumble = mul(lowpass(lowpass(noise(n, rng), 260, rate), 260, rate), env(gain))
    buzz = []
    for i in range(n):
        x = (i * 96 / rate) % 1.0
        buzz.append(2 * x - 1)
    buzz = mul(lowpass(lowpass(buzz, 340, rate), 340, rate), env(gain * 0.22))
    return mix(scale(rumble, 3.0), buzz)


def copier_clip(signal, rate, peak):
    return soft_finish(soft_room(soft_tilt(signal, rate, 3400), rate, 0.08), rate, peak, 0.003, 0.02)


def copier_rise(rng, rate):
    """The copier rising into view: a low, soft push of air."""
    n = seconds(0.32, rate)
    return copier_clip(burst(rng, rate, n, 320, 0.8, 0.28, 0.05, "lowpass"), rate, 0.22)


def copier_lidup(rng, rate):
    """The lid lifting on its hinge: a small plastic scuff and a short rising creak."""
    n = seconds(0.18, rate)
    return copier_clip(mix(burst(rng, rate, n, 900, 2, 0.12, 0.05), glide_tone(rate, n, 170, 230, 0.14, 0.02, saw=True)), rate, 0.16)


def copier_thunk(rng, rate):
    """The lid shutting on the glass: a dull plastic thunk, a knock and a faint click of the latch."""
    n = seconds(0.2, rate)
    body = glide_tone(rate, n, 110, 68, 0.15, 0.22)
    knock = burst(rng, rate, n, 520, 0.9, 0.09, 0.24, "lowpass")
    click = burst(rng, rate, n, 2300, 3, 0.03, 0.06)
    return copier_clip(mix(body, knock, click), rate, 0.56)


def copier_slap(rng, rate):
    """The card landing face down on the glass: a light slap of card stock."""
    n = seconds(0.09, rate)
    return copier_clip(mix(burst(rng, rate, n, 1500, 1.4, 0.06, 0.12), burst(rng, rate, n, 420, 1, 0.05, 0.08, "lowpass")), rate, 0.3)


def copier_beep(rng, rate):
    """Start pressed: one short, quiet beep at G6."""
    n = seconds(0.11, rate)
    return copier_clip(glide_tone(rate, n, 1568, 1568, 0.09, 0.06), rate, 0.12)


def copier_hum(rng, rate):
    """The scan: the motor running for 1.2 s, with the lamp's faint hiss over it."""
    n = seconds(1.26, rate)
    hiss = [0.0] * n
    place(hiss, burst(rng, rate, n, 2600, 0.7, 1.08, 0.018), seconds(0.05, rate))
    return copier_clip(mix(motor(rng, rate, n, 1.2, 0.12), hiss), rate, 0.3)


def copier_paper(rng, rate):
    """The copy sliding out of the slot: a dry paper hiss, a brighter one after it."""
    n = seconds(0.48, rate)
    second = [0.0] * n
    place(second, burst(rng, rate, n, 5200, 0.8, 0.3, 0.03, "highpass"), seconds(0.12, rate))
    return copier_clip(mix(burst(rng, rate, n, 2800, 0.6, 0.45, 0.06, "highpass"), second), rate, 0.18)


def copier_feed(rng, rate):
    """The rollers taking a copy back in: the motor for .9 s and the paper hissing through."""
    n = seconds(0.96, rate)
    hiss = [0.0] * n
    place(hiss, burst(rng, rate, n, 2600, 0.6, 0.7, 0.05, "highpass"), seconds(0.1, rate))
    return copier_clip(mix(motor(rng, rate, n, 0.9, 0.09), hiss), rate, 0.28)


def copier_lift(rng, rate):
    """A card lifted off the list or the glass: a tiny brush of air."""
    n = seconds(0.12, rate)
    return copier_clip(burst(rng, rate, n, 3200, 1, 0.1, 0.03, "highpass"), rate, 0.09)


def copier_settle(rng, rate):
    """A card settling into its place: a small, soft tap."""
    n = seconds(0.08, rate)
    return copier_clip(mix(burst(rng, rate, n, 1800, 1.2, 0.05, 0.07), burst(rng, rate, n, 600, 1, 0.04, 0.05, "lowpass")), rate, 0.22)


def copier_tick(rng, rate):
    """The menu opening: a tiny tick."""
    n = seconds(0.04, rate)
    return copier_clip(burst(rng, rate, n, 3200, 4, 0.02, 0.06), rate, 0.1)
# ---------- Pigeonhole rack ----------

def swept_bandpass(signal, f0, f1, q, rate):
    """An RBJ band-pass whose centre glides exponentially from f0 to f1 over the signal."""
    out, x1, x2, y1, y2, n = [], 0.0, 0.0, 0.0, 0.0, max(1, len(signal) - 1)
    for i, x in enumerate(signal):
        w = 2 * math.pi * f0 * (f1 / f0) ** (i / n) / rate
        alpha = math.sin(w) / (2 * q)
        a0, a1, a2 = 1 + alpha, -2 * math.cos(w), 1 - alpha
        y = (alpha * x - alpha * x2 - a1 * y1 - a2 * y2) / a0
        x2, x1, y2, y1 = x1, x, y1, y
        out.append(y)
    return out


def pigeonhole_lift(rng, rate):
    """A message lifted off the list: a short breath of air off the paper."""
    n = seconds(0.08, rate)
    env = [math.sin(math.pi * i / n) ** 2 for i in range(n)]
    air = mul(bandpass(highpass(noise(n, rng), 1800, rate), 3000, 0.9, rate), env)
    return soft_finish(soft_room(soft_tilt(air, rate, 3600), rate, 0.10), rate, 0.14, 0.006, 0.01)


def pigeonhole_fold(rng, rate):
    """The letter folding in thirds: two soft paper creases, the second a little lower."""
    track = [0.0] * seconds(0.12, rate)
    n = seconds(0.05, rate)
    place(track, soft_mode(rng, n, rate, 2300, 2.2, 0.008, attack=0.003), 0)
    place(track, scale(soft_mode(rng, n, rate, 1600, 2.2, 0.01, attack=0.003), 0.8), seconds(0.045, rate))
    return soft_finish(soft_room(soft_tilt(track, rate, 3000), rate, 0.10), rate, 0.22, 0.003, 0.02)


def pigeonhole_slide(rng, rate):
    """A letter sliding into a cubby over oak: paper on wood, its band falling as it goes in."""
    n = seconds(0.24, rate)
    env = soft_env(n, rate, 0.02, 0.08)
    hiss = mul(swept_bandpass(lowpass(noise(n, rng), 4000, rate), 2200, 900, 0.9, rate), env)
    return soft_finish(soft_room(soft_tilt(hiss, rate, 3000), rate, 0.10), rate, 0.24, 0.01, 0.04)


def pigeonhole_tock(rng, rate):
    """The letter coming to rest in its cubby: a muffled knock on oak and its low wooden note."""
    n = seconds(0.16, rate)
    knock = mul(lowpass(noise(n, rng), 650, rate), soft_env(n, rate, 0.003, 0.018))
    body = soft_mode(rng, n, rate, 210, 4.0, 0.04, attack=0.004)
    return soft_finish(soft_room(soft_tilt(mix(knock, scale(body, 0.7)), rate, 2400), rate, 0.12), rate, 0.5, 0.004, 0.03)


def pigeonhole_rack(rng, rate):
    """The rack sliding up from below: a low rumble of a wooden cabinet on its runners."""
    n = seconds(0.32, rate)
    rumble = mul(lowpass(lowpass(noise(n, rng), 320, rate), 260, rate), soft_env(n, rate, 0.04, 0.12))
    hum = mul(chirp(n, rate, 95, 72, 0.28, 0.12), soft_env(n, rate, 0.03, 0.12))
    return soft_finish(soft_room(mix(rumble, scale(hum, 0.2)), rate, 0.12), rate, 0.36, 0.02, 0.06)


def pigeonhole_tick(rng, rate):
    """A cubby lighting up under the letter: a tiny bright tick, a fingernail on the brass plate."""
    n = seconds(0.04, rate)
    tick = soft_mode(rng, n, rate, 3400, 5.0, 0.004, attack=0.002)
    return soft_finish(soft_room(soft_tilt(tick, rate, 4200), rate, 0.08), rate, 0.07, 0.002, 0.008)
# ---------- CRT picture-in-picture ----------
# An old portable TV, from the prototype's WebAudio: oscillators and noise bursts whose gain falls
# exponentially to nothing over each one's length, as `exponentialRampToValueAtTime(.0001, …)`.


def crt_ramp(n, rate, gain, dur, start=0.0):
    """A gain set at start and ramped exponentially down to .0001 over dur seconds, then silent."""
    s0, m = seconds(start, rate), seconds(dur, rate)
    return [gain * (0.0001 / gain) ** ((i - s0) / m) if s0 <= i < s0 + m else 0.0 for i in range(n)]


def crt_tone(n, rate, f0, f1, dur, gain, shape="sine", start=0.0):
    """An oscillator gliding exponentially from f0 to f1 over dur, under its falling gain."""
    out, phase, s0, m = [0.0] * n, 0.0, seconds(start, rate), seconds(dur, rate)
    env = crt_ramp(n, rate, gain, dur, start)
    for i in range(s0, min(n, s0 + m)):
        f = f0 * (f1 / f0) ** ((i - s0) / m)
        phase += f / rate
        p = phase % 1.0
        v = math.sin(2 * math.pi * p) if shape == "sine" else 4 * abs(p - 0.5) - 1
        out[i] = v * env[i]
    return out


def crt_burst(rng, n, rate, freq, q, dur, gain, kind="bandpass", start=0.0):
    """Filtered noise under a falling gain, as the prototype's burst()."""
    src = noise(n, rng)
    if kind == "lowpass":
        shaped = lowpass(lowpass(src, freq, rate), freq, rate)
    elif kind == "highpass":
        shaped = highpass(highpass(src, freq, rate), freq, rate)
    else:
        shaped = bandpass(src, freq, q, rate)
    return mul(shaped, crt_ramp(n, rate, gain, dur, start))


def crt_off(rng, rate):
    """The tube switching off: the flyback whine cutting out and the soft thump of it letting go."""
    n = seconds(0.26, rate)
    hit = mix(
        crt_tone(n, rate, 7800, 7600, 0.16, 0.012),
        crt_tone(n, rate, 150, 42, 0.22, 0.16),
        crt_burst(rng, n, rate, 420, 0.8, 0.09, 0.22, "lowpass"),
    )
    return soft_finish(soft_room(hit, rate, 0.1), rate, 0.5, 0.003, 0.03)


def crt_fly(rng, rate):
    """The dot in flight: a soft band of air and a faint rising tone."""
    n = seconds(0.36, rate)
    air = mix(crt_burst(rng, n, rate, 1600, 1.2, 0.34, 0.035), crt_tone(n, rate, 900, 1400, 0.3, 0.008))
    return soft_finish(soft_room(soft_tilt(air, rate, 3600), rate, 0.1), rate, 0.2, 0.01, 0.04)


def crt_on(rng, rate):
    """The set warming up: a crackle of static, the low hum of it coming on and a small tick."""
    n = seconds(0.34, rate)
    warm = mix(
        crt_burst(rng, n, rate, 4200, 0.6, 0.26, 0.09, "highpass"),
        crt_tone(n, rate, 118, 122, 0.32, 0.05, "triangle"),
        crt_burst(rng, n, rate, 900, 2.0, 0.05, 0.08, start=0.02),
    )
    return soft_finish(soft_room(warm, rate, 0.1), rate, 0.36, 0.003, 0.03)
# ---------- Rubber band ----------

def band_tone(rate, f0, f1, dur, kind="triangle", vib=0.0):
    """The prototype's tone(): a pitch gliding from f0 to f1 (exponentially) over dur, a 6 ms rise and an
    exponential fall, through a 1.8 kHz low-pass; vib wobbles the pitch by 4 % at that rate, as a band does."""
    n = seconds(dur + 0.02, rate)
    out, phase = [], 0.0
    for i in range(n):
        t = i / rate
        f = f0 * (f1 / f0) ** min(1.0, t / dur)
        if vib:
            f += f0 * 0.04 * math.sin(2 * math.pi * vib * t)
        phase += f / rate
        x = phase % 1.0
        v = 4 * abs(x - 0.5) - 1 if kind == "triangle" else math.sin(2 * math.pi * phase)
        env = (t / 0.006) if t < 0.006 else math.exp(-(t - 0.006) / (dur / 9.2))
        out.append(v * env)
    return lowpass(out, 1800, rate)


def band_burst(rng, rate, freq, q, dur, kind="bandpass"):
    """The prototype's burst(): noise through a filter, falling to nothing over dur."""
    n = seconds(dur + 0.02, rate)
    src = noise(n, rng)
    if kind == "highpass":
        src = highpass(src, freq, rate)
    elif kind == "lowpass":
        src = lowpass(lowpass(src, freq, rate), freq, rate)
    else:
        src = bandpass(src, freq, q, rate)
    return mul(src, soft_env(n, rate, 0.003, dur / 9.2))


def band_mix(*parts):
    """Mixes (gain, track) pairs, laid from the start."""
    n = max(len(t) for _, t in parts)
    out = [0.0] * n
    for g, t in parts:
        for i, x in enumerate(t):
            out[i] += g * x
    return out


def band_clips(rng, rate):
    """The rubber band's clips, from the prototype's snd() recipes, softened like the rest."""
    clips = {
        # A soft whoosh off the field and a small rising chirp.
        "band_send": (band_mix((0.05, band_burst(rng, rate, 1800, 0.8, 0.14, "highpass")), (0.035, band_tone(rate, 620, 880, 0.09))), 0.22),
        # The finger taking hold of the latex: a dull pat.
        "band_grab": (band_burst(rng, rate, 500, 1.5, 0.05, "lowpass"), 0.3),
        # Pulled far enough: a short low creak and a tick of stretched rubber.
        "band_taut": (band_mix((0.05, band_tone(rate, 210, 230, 0.12)), (0.04, band_burst(rng, rate, 2600, 6, 0.03))), 0.22),
        # Let go short: the band boings back, wobbling.
        "band_boing": (band_tone(rate, 150, 120, 0.44, vib=22), 0.4),
        # Yanked home: a thump, a snap and a falling twang.
        "band_yank": (band_mix((0.3, band_burst(rng, rate, 260, 1, 0.09, "lowpass")), (0.14, band_burst(rng, rate, 1400, 2, 0.05)), (0.07, band_tone(rate, 260, 90, 0.16))), SOFT_PEAK),
        # The bubble bursting into its letters: a short airy pop.
        "band_burst": (band_burst(rng, rate, 3200, 0.9, 0.12, "highpass"), 0.24),
        # A letter landing in the field.
        "band_tick": (band_burst(rng, rate, 2400, 4, 0.025), 0.2),
        # The window ran out: a snap and a wobbling twang that falls.
        "band_twang": (band_mix((0.18, band_burst(rng, rate, 3000, 3, 0.03)), (0.1, band_tone(rate, 330, 170, 0.42, vib=26)), (0.05, band_tone(rate, 165, 120, 0.3, "sine"))), 0.45),
    }
    return {name: soft_finish(soft_room(soft_tilt(clip, rate, 3000), rate, 0.1), rate, peak, 0.004, 0.03)
            for name, (clip, peak) in clips.items()}
# ---------- Coin sorter ----------
# Small coins in a chrome tray heard close by: a nickel ring as one lands in its tube, dry metal
# ticks over the slots and a rattle in the funnel, a felt pat as a row tips, and a soft card knock
# as one opens back onto its line. Built with the soft toolkit.


def coin_clink(rng, rate):
    """A coin landing on the others in a tube: a small tick and a bright, short nickel ring."""
    n = seconds(0.26, rate)
    tick = mul(bandpass(noise(n, rng), 5200, 4.0, rate), soft_env(n, rate, 0.002, 0.007))
    ring = mix(soft_mode(rng, n, rate, 4300, 60.0, 0.08, attack=0.003), scale(soft_mode(rng, n, rate, 6900, 50.0, 0.045, attack=0.003), 0.45))
    after = [0.0] * n
    place(after, scale(mul(bandpass(noise(n, rng), 4800, 6.0, rate), soft_env(n, rate, 0.002, 0.005)), 0.3), seconds(0.05, rate))
    hit = mix(scale(tick, 0.8), scale(ring, 0.9), after)
    return soft_finish(soft_room(soft_tilt(hit, rate, 5200), rate, 0.12), rate, 0.42, 0.003, 0.05)


def coin_rattle(rng, rate):
    """A coin dropping into the funnel heap: two quick metal ticks against the others and a faint ring."""
    n = seconds(0.11, rate)
    a = mul(bandpass(noise(n, rng), 3000, 5.0, rate), soft_env(n, rate, 0.002, 0.01))
    b = [0.0] * n
    place(b, scale(mul(bandpass(noise(n, rng), 3600, 6.0, rate), soft_env(n, rate, 0.002, 0.007)), 0.6), seconds(0.035, rate))
    ring = soft_mode(rng, n, rate, 3100, 40.0, 0.03, attack=0.003)
    return soft_finish(soft_room(soft_tilt(mix(a, b, scale(ring, 0.3)), rate, 4200), rate, 0.1), rate, 0.3, 0.003, 0.03)


def coin_tick(rng, rate):
    """A coin rolling over a slot too small for it: a tiny dry tick off the rail."""
    n = seconds(0.04, rate)
    tick = mul(bandpass(noise(n, rng), 2600, 8.0, rate), soft_env(n, rate, 0.0015, 0.005))
    return soft_finish(soft_room(soft_tilt(tick, rate, 4000), rate, 0.08), rate, 0.16, 0.002, 0.015)


def coin_tip(rng, rate):
    """A row tipping up on its edge: a soft, low pat."""
    n = seconds(0.09, rate)
    pat = mul(lowpass(noise(n, rng), 900, rate), soft_env(n, rate, 0.004, 0.02))
    return soft_finish(soft_room(soft_tilt(pat, rate, 2000), rate, 0.08), rate, 0.14, 0.004, 0.03)


def coin_out(rng, rate):
    """A coin slipping out of the bottom of its tube: a short brush of metal on metal."""
    n = seconds(0.06, rate)
    brush = mul(bandpass(noise(n, rng), 2200, 3.0, rate), soft_env(n, rate, 0.003, 0.01))
    return soft_finish(soft_room(soft_tilt(brush, rate, 3600), rate, 0.08), rate, 0.14, 0.003, 0.02)


def coin_thunk(rng, rate):
    """A row opening back onto its line: a muffled card knock with a light tap on top."""
    n = seconds(0.14, rate)
    knock = mul(lowpass(lowpass(noise(n, rng), 320, rate), 320, rate), soft_env(n, rate, 0.004, 0.03))
    tap = mul(bandpass(noise(n, rng), 1700, 2.0, rate), soft_env(n, rate, 0.002, 0.006))
    return soft_finish(soft_room(soft_tilt(mix(knock, scale(tap, 0.15)), rate, 2400), rate, 0.1), rate, SOFT_PEAK, 0.004, 0.04)
# ---------- Decoder ring ----------

def decoder_tick(rng, rate):
    """A letter of the brass dial passing the pointer: a tiny, bright ratchet tick with a faint ring."""
    n = seconds(0.04, rate)
    tick = mul(bandpass(noise(n, rng), 4200, 6, rate), soft_env(n, rate, 0.002, 0.006))
    ring = scale(soft_mode(rng, n, rate, 2900, 30, 0.012, attack=0.002), 0.25)
    return soft_finish(soft_room(soft_tilt(mix(tick, ring), rate, 5200), rate, 0.08), rate, 0.2, 0.002, 0.01)


def decoder_settle(rng, rate):
    """A letter clicking down into its slot: a soft card-reel click."""
    n = seconds(0.04, rate)
    click = mul(bandpass(noise(n, rng), 3000, 4, rate), soft_env(n, rate, 0.002, 0.007))
    return soft_finish(soft_room(soft_tilt(click, rate, 4800), rate, 0.08), rate, 0.16, 0.002, 0.01)


def decoder_done(rng, rate):
    """Every letter in English: a small brass chime, two partials over a soft click, never a pure tone."""
    track = [0.0] * seconds(0.6, rate)
    n = seconds(0.55, rate)
    place(track, mix(soft_mode(rng, n, rate, 1568, 60, 0.16, 0.004), scale(damped_sine(n, rate, 1564, 0.14), 0.04)), 0)
    place(track, scale(soft_mode(rng, n, rate, 2349, 60, 0.11, 0.004), 0.5), seconds(0.01, rate))
    m = seconds(0.04, rate)
    place(track, scale(mul(bandpass(noise(m, rng), 3000, 3, rate), soft_env(m, rate, 0.002, 0.008)), 0.6), 0)
    return soft_finish(soft_room(soft_tilt(track, rate, 4200), rate, 0.12), rate, 0.36, 0.004, 0.08)


def decoder_back(rng, rate):
    """Back in Spanish: one lower brass note over a soft click."""
    track = [0.0] * seconds(0.42, rate)
    n = seconds(0.4, rate)
    place(track, mix(soft_mode(rng, n, rate, 1175, 60, 0.11, 0.004), scale(damped_sine(n, rate, 1172, 0.1), 0.04)), 0)
    m = seconds(0.04, rate)
    place(track, scale(mul(bandpass(noise(m, rng), 2400, 3, rate), soft_env(m, rate, 0.002, 0.008)), 0.6), 0)
    return soft_finish(soft_room(soft_tilt(track, rate, 3800), rate, 0.12), rate, 0.3, 0.004, 0.06)


# ---------- Grave undo ----------
# A moonlit graveyard in a snackbar, heard close and soft: a dry tick as the swipe arms, a hushed
# whoosh as the row flies off, a muffled thud into the soil, a crumble of dirt and a faint low "ooo"
# as the hand comes up, and two knuckle clicks and a soft lift as it hands the task back. The
# prototype's WebAudio shots: filtered noise whose cutoff glides, and gliding sines, through the
# soft toolkit.


def grave_shot(rng, rate, mode, f0, f1, gain, attack, dur, q=0.7, delay=0.0):
    """The prototype's noiseShot(): noise through a filter gliding from f0 to f1 (exponentially) over
    dur, with an exponential rise to gain over attack and a fall to nothing at dur."""
    n = seconds(delay + dur + 0.05, rate)
    src = noise(n, rng)
    low = band = 0.0
    out = []
    for i, x in enumerate(src):
        t = i / rate - delay
        if t < 0:
            out.append(0.0)
            continue
        f = f0 * (f1 / f0) ** min(1.0, t / dur)
        k = 2 * math.sin(math.pi * min(f, rate / 6) / rate)
        high = x - low - band / q
        band += k * high
        low += k * band
        v = low if mode == "lowpass" else band
        if t < attack:
            env = 0.0001 * (gain / 0.0001) ** (t / attack)
        elif t < dur:
            env = gain * (0.0001 / gain) ** ((t - attack) / (dur - attack))
        else:
            env = 0.0
        out.append(v * env)
    return out


def grave_tone(rate, f0, f1, gain, attack, dur, delay=0.0, vib=0.0):
    """The prototype's tone(): a sine gliding from f0 to f1 over dur, a vib Hz-deep wobble at 5 Hz,
    rising to gain over attack and falling to nothing at dur."""
    n = seconds(delay + dur + 0.05, rate)
    out, phase = [], 0.0
    for i in range(n):
        t = i / rate - delay
        if t < 0:
            out.append(0.0)
            continue
        f = f0 * (f1 / f0) ** min(1.0, t / dur) + vib * math.sin(2 * math.pi * 5 * t)
        phase += f / rate
        if t < attack:
            env = 0.0001 * (gain / 0.0001) ** (t / attack)
        elif t < dur:
            env = gain * (0.0001 / gain) ** ((t - attack) / (dur - attack))
        else:
            env = 0.0
        out.append(math.sin(2 * math.pi * phase) * env)
    return out


def grave_clips(rng, rate):
    """The grave's clips, from the prototype's sTick/sSwipe/sThud/sCrumble/sOoo/sClick/sLift."""
    crumble = band_mix(*[(1.0, grave_shot(rng, rate, "bandpass", 900 + rng.random() * 900, 600, 0.03 + rng.random() * 0.03, 0.003, 0.05 + rng.random() * 0.05, 2.5, i * 0.07 + rng.random() * 0.04)) for i in range(9)])
    clips = {
        # The swipe passing the line: a tiny dry tick.
        "grave_tick": (grave_shot(rng, rate, "bandpass", 3200, 2400, 0.035, 0.002, 0.03, 3), 0.16),
        # The row flying off: a hushed whoosh that darkens.
        "grave_swipe": (grave_shot(rng, rate, "lowpass", 1600, 300, 0.05, 0.04, 0.45), 0.2),
        # Into the soil: a muffled thud with a low body under it.
        "grave_thud": (band_mix((1.0, grave_shot(rng, rate, "lowpass", 320, 110, 0.14, 0.006, 0.3)), (1.0, grave_tone(rate, 88, 52, 0.09, 0.006, 0.32))), SOFT_PEAK),
        # The hand pushing up: dirt crumbling.
        "grave_crumble": (crumble, 0.22),
        # And under it, a faint low "ooo", a quarter second in.
        "grave_ooo": (band_mix((1.0, grave_tone(rate, 300, 270, 0.018, 0.35, 1.4, 0.25, 6)), (1.0, grave_tone(rate, 450, 405, 0.008, 0.4, 1.3, 0.3, 6))), 0.12),
        # Two knuckle clicks as the hand lets go.
        "grave_click": (band_mix((1.0, grave_shot(rng, rate, "bandpass", 2600, 2200, 0.06, 0.001, 0.025, 4)), (1.0, grave_shot(rng, rate, "bandpass", 3000, 2500, 0.05, 0.001, 0.025, 4, 0.075))), 0.2),
        # The task lifting home: a soft rising breath.
        "grave_lift": (grave_shot(rng, rate, "bandpass", 500, 2200, 0.025, 0.2, 0.7, 1.2), 0.14),
    }
    return {name: soft_finish(soft_room(soft_tilt(clip, rate, 3200), rate, 0.1), rate, peak, 0.002, 0.03)
            for name, (clip, peak) in clips.items()}


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
    tin = random.Random(1021)
    write("tin_tunk", tin_tunk(tin, fast), fast)
    write("flag_twang", flag_twang(tin, fast), fast)
    write("door_creak", door_creak(tin, fast), fast)
    write("envelope_slip", envelope_slip(tin, fast), fast)
    write("mail_tick", mail_tick(fast), fast)
    lock = random.Random(1021)
    write("padlock_clunk", padlock_clunk(lock, fast), fast)
    write("padlock_pop", padlock_pop(lock, fast), fast)
    write("padlock_rattle", padlock_rattle(lock, fast), fast)
    write("padlock_swing", padlock_swing(lock, fast), fast)
    cup = random.Random(1022)
    write("pencil_clink", pencil_clink(cup, fast), fast)
    write("pencil_lift", pencil_lift(cup, fast), fast)
    write("pencil_pick", pencil_pick(cup, fast), fast)
    locket = random.Random(1022)
    write("locket_open", locket_open(locket, fast), fast)
    write("locket_snap", locket_snap(locket, fast), fast)
    write("locket_place", locket_place(locket, fast), fast)
    write("locket_lift", locket_lift(locket, fast), fast)
    write("locket_back", locket_back(locket, fast), fast)
    write("locket_chain", locket_chain(locket, fast), fast)
    for name, clip in birdcage_clips(random.Random(1022), fast).items():
        write(name, clip, fast)
    zipper = random.Random(1022)
    write("zip_clack", zip_clack(zipper, fast), fast)
    write("zip_tick", zip_tick(zipper, fast), fast)
    write("zip_teeth", zip_teeth(zipper, fast), fast)
    write("zip_lift", zip_lift(zipper, fast), fast)
    keys = random.Random(1022)
    write("key_tink", key_tink(keys, fast), fast)
    write("key_clack", key_clack(keys, fast), fast)
    write("key_lift", key_lift(keys, fast), fast)
    fan = random.Random(1022)
    write("paperfan_fwap", paperfan_fwap(fan, fast), fast)
    write("paperfan_click", paperfan_click(fan, fast), fast)
    write("paperfan_swish", paperfan_swish(fan, fast), fast)
    write("paperfan_clack", paperfan_clack(fan, fast), fast)
    note = random.Random(1022)
    write("note_grain", note_grain(note, fast), fast)
    write("note_tick", note_tick(note, fast), fast)
    write("note_rip", note_rip(note, fast), fast)
    write("note_stick", note_stick(note, fast), fast)
    write("note_pat", note_pat(note, fast), fast)
    write("note_lift", note_lift(note, fast), fast)
    roller = random.Random(1027)
    write("roller_dip", roller_dip(roller, fast), fast)
    write("roller_set", roller_set(roller, fast), fast)
    write("roller_roll", roller_roll(roller, fast), fast)
    press = random.Random(1027)
    write("press_pour", press_pour(press, fast), fast)
    write("press_rise", press_rise(press, fast), fast)
    write("press_tick", press_tick(press, fast), fast)
    write("press_tink", press_tink(press, fast), fast)
    write("press_glug", press_glug(press, fast), fast)
    write("press_lift", press_lift(press, fast), fast)
    write("press_hiss", press_hiss(press, soft), soft)
    copier = random.Random(1027)
    write("copier_rise", copier_rise(copier, fast), fast)
    write("copier_lidup", copier_lidup(copier, fast), fast)
    write("copier_thunk", copier_thunk(copier, fast), fast)
    write("copier_slap", copier_slap(copier, fast), fast)
    write("copier_beep", copier_beep(copier, fast), fast)
    write("copier_hum", copier_hum(copier, fast), fast)
    write("copier_paper", copier_paper(copier, fast), fast)
    write("copier_feed", copier_feed(copier, fast), fast)
    write("copier_lift", copier_lift(copier, fast), fast)
    write("copier_settle", copier_settle(copier, fast), fast)
    write("copier_tick", copier_tick(copier, fast), fast)
    rack = random.Random(1027)
    write("pigeonhole_lift", pigeonhole_lift(rack, fast), fast)
    write("pigeonhole_fold", pigeonhole_fold(rack, fast), fast)
    write("pigeonhole_slide", pigeonhole_slide(rack, fast), fast)
    write("pigeonhole_tock", pigeonhole_tock(rack, fast), fast)
    write("pigeonhole_rack", pigeonhole_rack(rack, fast), fast)
    write("pigeonhole_tick", pigeonhole_tick(rack, fast), fast)
    crt = random.Random(1027)
    write("crt_off", crt_off(crt, fast), fast)
    write("crt_fly", crt_fly(crt, fast), fast)
    write("crt_on", crt_on(crt, fast), fast)
    for name, clip in band_clips(random.Random(1027), fast).items():
        write(name, clip, fast)
    coins = random.Random(1027)
    write("coin_clink", coin_clink(coins, fast), fast)
    write("coin_rattle", coin_rattle(coins, fast), fast)
    write("coin_tick", coin_tick(coins, fast), fast)
    write("coin_tip", coin_tip(coins, fast), fast)
    write("coin_out", coin_out(coins, fast), fast)
    write("coin_thunk", coin_thunk(coins, fast), fast)
    ring = random.Random(1027)
    write("decoder_tick", decoder_tick(ring, fast), fast)
    write("decoder_settle", decoder_settle(ring, fast), fast)
    write("decoder_done", decoder_done(ring, fast), fast)
    write("decoder_back", decoder_back(ring, fast), fast)
    for name, clip in grave_clips(random.Random(1028), fast).items():
        write(name, clip, fast)


if __name__ == "__main__":
    main()
