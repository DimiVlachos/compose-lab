# Running drips on the fogged mirror

## Why

The fogged mirror's condensation is a still photo texture. A real steamed-up mirror is never still: now
and then a drop gathers, grows heavy, and runs down, leaving a clear streak through the fog. Adding that
motion is the biggest step towards the mirror looking real.

## What the user decided

- Only **running drips**: no drips gathering at wipe edges, no slow re-fogging, no shimmer.
- **Occasional**: one every 4–8 s at a random spot.
- In the clip, drips are **scripted**, one or two at fixed times and spots that never cross the heart, so
  every recording is the same.

## Behaviour

- **Where a drip starts:**
  - on fogged glass only, never on a wiped patch or on glass a breath has not covered
  - in the top two thirds of the glass, away from the side edges (at least 8% in)
- **How often:** the next drip starts 4–8 s (uniform random) after the previous one started. At most two
  drips are live at once. If there's no fogged spot to start on, the drip is skipped until the next turn.
- **How it moves:**
  - it grows in place for about 0.6 s: the bead swells, and nothing moves yet
  - it slides in 2–4 bursts, each speeding up to about 150 dp/s, with 0.2–0.8 s stuck between bursts
  - it drifts less than 3 dp sideways over the whole run, a gentle wave
  - it stops after 15–40% of the glass height, or at the bottom edge
- **Once it stops:** the bead stays as a small resting drop at the streak's end.
- **Breath:** a full breath fogs over streaks and resting drops, because they are fog marks like wipes. A
  drip that's still moving carries on through the fresh fog.
- **Pausing:** drips run only while the demo is on screen (RESUMED). Leaving pauses them where they are,
  and they carry on on return.
- **Recording:** no random drips, only the script's `drip` calls. They also run on iOS and without a
  camera, because they are part of the fog.

## Look

- **Streak:**
  - a thin wipe about 4 dp in radius (a finger is 36 dp), clearing about 85% of the fog, not all of it,
    so it looks wet rather than wiped
  - narrower at the top, where the drop started, and slightly wavy
- **Bead:** a drop 5–7 dp across at the head of the streak, drawn over the photo or the camera:
  - inside, the glass behind shows slightly darker
  - a thin darker rim along its lower edge
  - one small bright highlight near its top
- **Resting bead:** shrinks to about 4 dp once the drip stops.

## Design

- **Stroke clarity:** a `WipeStroke` gains a `clarity` (0..1, default 1), how much of the fog it clears.
  `drawWipe` scales its dabs' alpha by it. Nothing else in the fog changes.
- **`DripDriver`** (core fog, pure logic):
  - built with a `FogState`, a `Random` and the glass size
  - `advance(seconds)` starts drips on schedule, moves the live ones, and extends each drip's thin stroke
    (radius 4 dp, clarity 0.85) on the fog
  - `drip(at, length)` starts one at a given spot; the script uses it
  - `beads` lists each live or resting drop's position, size and whether it has stopped, as snapshot state
  - "fogged at a point" is judged from the fog's marks since the last full breath (or since the start,
    if there has been none):
    - no evaporation among them
    - no wipe stroke passes within its radius of the point
    - partial breaths are not counted, which errs towards skipping a drip, never towards starting one
      on clear glass
- **`FoggedMirror`:** a new optional `beads: List<Bead>` parameter, drawn on top of the glass. Without
  it, nothing changes.
- **`FogDemo`:**
  - owns the driver
  - runs a `withFrameNanos` loop under `repeatOnLifecycle(RESUMED)` when not recording
  - random starts are switched off in the recording and in Replay, so the script's drips play alone
- **Script:**
  - `DemoController` gains `drip(at: Offset, length: Float)`, where length is a fraction of the glass
    height; `DemoState` routes it to a handler, like `wipe` and `breathe`
  - the clip calls it once or twice at fixed times, at spots clear of the heart's bounds, before the
    closing breath

## Testing

- **`DripDriver`** (common tests, seeded `Random`, stepped time):
  - drips start 4–8 s apart
  - a drip only moves down and stays inside the glass
  - it stops within its length
  - it never starts on a wiped patch or before a breath has fogged that spot
  - no more than two are live at once
  - its streak is a stroke of radius 4 dp and clarity 0.85
- **`drawWipe`:** a stroke with clarity 0.85 leaves about 15% of the fog (pixels).
- **`FogDemosTest`:** the clip's drips start after the heart and never come within the heart's bounds;
  the clip still ends fogged over.
- **UI:**
  - a drip clears a thin line through the fog (pixels)
  - a full breath fogs it over
  - the recording starts no random drips
  - the loop runs only while RESUMED
  - the existing wipe, multi-finger, card and camera tests pass unchanged

## Out of scope

- drips gathering along wipe edges
- slow re-fogging of wiped glass
- shimmering droplets
- drops merging with each other
