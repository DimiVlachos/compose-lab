# Breath frost: blow on the screen to fog the glass again

Date: 2026-09-30
Status: design approved in conversation, awaiting spec review
Builds on: the fogged mirror demo (`frost.window`), branch `feat/fogged-mirror`

## Goal

In the fogged mirror demo, blowing on the phone fogs the glass back over: a soft cloud rises from the
bottom edge, where the mouth and microphone are, for as long as the user blows. The recorded clip shows the
same moment with a scripted breath, which also closes the clip's loop without a visible reset.

A phone cannot sense warm air. The blow is detected by its sound: breath reaches the microphone as loud,
broadband, noise-like audio.

## Decisions

| Question | Decision |
|---|---|
| Who it is for | Both the recorded clip and people holding the phone |
| Platforms | Real mic detection on Android first; iOS gets the effect, the scripted breath and hold-to-breathe, with a stub microphone until a follow-up |
| Fog look | Blooms from the bottom edge and rolls up; a short puff fogs the lower part, a long breath covers everything; the front stays where it stopped |
| Mic permission | Requested when the frost demo opens (never in record mode); if denied, the demo falls back to hold-to-breathe |
| Frost model | Approach B: an ordered list of marks (wipes and breaths) replayed inside the existing offscreen frost layer. A frost mask bitmap (approach C) is deferred until droplets |

Rejected: a separate fog layer above the frost (approach A), because a later wipe could not clear fresh fog.

## 1. The glass: `core/presentation/components/frost/`

### State

`FrostState` holds an ordered list of marks, each a wipe or a breath.

- `beginStroke(at): WipeStroke` and `extendStroke(stroke, to)` are unchanged; a stroke is now a wipe mark.
- `beginBreath(): Breath` adds a breath mark and returns its handle.
- `setBreathLevel(breath, level)` sets the breath's fog front, in fractions of the window height, from 0 (bottom
  edge) to 1 (top). Levels only rise; a lower value is ignored.
- When a breath's front covers the whole window, including its soft edge, the state drops that breath and every
  mark before it. The glass is then indistinguishable from fresh frost and the list stays short.
- Setting the level of a breath that has been dropped, by `clear()` or by a later full breath, does nothing.
- `clear()` is unchanged: all marks go.

The component knows only how high each breath's fog is. How fast it rises is decided by the caller.

### Drawing

Inside the existing offscreen layer, after drawing the frost:

- A wipe mark draws its soft dabs with `BlendMode.DstOut`, as today.
- A breath mark draws the frost texture again through a fog mask:
  `saveLayer` → draw the mask shapes → nested `saveLayer` with `BlendMode.SrcIn` → `drawContent()` → restore both.
- The fog mask (`FogMask.kt`) is the union of:
  - a solid area below the front;
  - a vertical gradient band at the front, about 12% of the window height, so the edge is soft;
  - a row of soft radial puffs along the front, of varying sizes at fixed positions, so the edge billows like a
    cloud rather than a straight line.
- Fog uses the same texture as the frost, so fog that has settled looks exactly like frost.

### Hold gesture

`FoggedMirror` gains an optional `onHoldChange: ((Boolean) -> Unit)?`. When it is set, a press that stays
within touch slop for 400 ms reports `true`, and releasing reports `false`; that press does not wipe. A press
that moves beyond slop before 400 ms wipes as today. When the callback is null, behaviour is unchanged.

## 2. Detecting the blow

### `core/audio/BlowDetector` (common code)

- Input: frames of 512 mono samples at 16 kHz (about 32 ms), as floats in -1..1.
- Output: blow strength in 0..1; 0 means no blow.
- Per frame:
  - loudness (RMS, in dBFS);
  - an adaptive background level: a slow moving average of loudness, updated only while not blowing;
  - spectral flatness over roughly 100–4000 Hz, from a 512-point FFT (`Fft.kt`): high for noise-like breath,
    low for tonal sound such as voiced speech or music.
- A frame counts as blowing when loudness is well above the background level (starting point: 15 dB) and
  flatness is high (starting point: 0.35). The starting values are tuned on the device.
- Onset after about 100 ms (3 frames) of blowing frames; release after about 130 ms (4 frames) without. The
  hysteresis stops flicker mid-breath.
- Strength rises with loudness above the threshold, reaching 1 at a firm blow.
- Known limit: "sss" and "shh" are noise-like and count as blows.

### Microphone source

- Common interface: a microphone is a cold `Flow<FloatArray>` of frames; collecting starts recording and
  cancelling stops it.
- Android (`androidMain`): `AudioRecord`, 16 kHz, mono, 16-bit PCM, on `Dispatchers.IO`. The source is
  `MediaRecorder.AudioSource.UNPROCESSED` where the device reports support, else `MIC`. The default voice
  sources apply noise suppression and gain control that remove the hiss a blow makes.
- iOS (`iosMain`): a stub that reports no microphone.
- Recording runs only while the frost demo is on screen and the app is at least resumed, so the system mic
  indicator shows only then.

### Permission and fallback

- `RECORD_AUDIO` is declared in the manifest. `composeApp` androidMain adds `activity-compose` for
  `rememberLauncherForActivityResult`.
- The frost demo requests the permission when it opens, except in record mode.
- Granted: a hint, "Blow on the screen", shows over the glass and fades after the first breath.
- Denied, no microphone (iOS), the recorder fails to start, or the permission is revoked while the demo is open:
  the hint reads "Hold to breathe", `onHoldChange` is wired, and a hold breathes at a steady strength (0.7) until
  release. Failures are logged with Kermit.

### `frostdemo/BreathDriver` (common code)

- Turns strength over time into a fog level: each frame the level rises by strength × speed × frame time, with
  speed chosen so a steady full-strength blow covers the glass in about 1.2 s; a typical blow, around
  strength 0.7, takes about 1.7 s.
- At strength 0 the level holds.
- The next blow continues the current breath mark, unless a wipe has begun since; then it begins a new breath,
  which rises from the bottom and covers that wipe.
- The microphone, the hold gesture and the script all drive the same `BreathDriver`.

## 3. Script and clip

- `DemoController` gains `suspend fun breathe(duration: Duration, strength: Float)`; `DemoState` forwards it to
  a handler set by the demo, like `wipe`; `FakeController` records it.
- The demo plays a scripted breath as a swell and fade: strength follows a half-sine over the duration, peaking
  at the given strength.
- `DemoState` gets a `recording` flag, set by `DemoScreen`, so the demo skips the permission request and hints.
- Clip timeline (about 11 s). The half-sine averages about 0.64 of its peak, so the breath is worth about
  1.5 s of full-strength blowing, enough to carry the front and its soft edge past the top:

  | Time | Step |
  |---|---|
  | 0 s | `select(1)` |
  | 0.4–6.0 s | the oval scrub, unchanged |
  | 6.0–7.4 s | hold on the clear oval |
  | 7.4–9.8 s | `breathe(2.4.seconds, strength = 1f)`: fog covers the glass |
  | 10.2 s | `select(0)`: the loop point; the glass is already fully frosted |

- `select(0)` still clears the frost as a safety net; in the clip it changes nothing on screen.

## Testing

Common unit tests:

- `FrostState`: marks replay in order; a breath reaching the top drops it and every earlier mark; a wipe begun
  after a breath is a later mark; levels never fall; setting a dropped breath's level does nothing.
- `BlowDetector`: loud white noise becomes a blow after about 100 ms; a loud harmonic tone does not; silence
  does not; noise at the adaptive background level does not; release follows about 130 ms of quiet.
- `Fft` through flatness: noise scores high, a pure tone scores low.
- `BreathDriver`: the rise is proportional to strength; the level holds at strength 0; the next blow continues the
  breath; a wipe in between starts a new breath from the bottom.
- `CatalogTest`: the clip's scripted breath, played through `BreathDriver` with its swell and fade, reaches the
  top, so the loop closes without a visible reset. The existing length and loop rules still hold.

UI tests:

- `FoggedMirror`: a still hold reports `onHoldChange(true)` then `false` and adds no wipe; a moving drag still
  wipes.
- `FrostDemo`: a scripted breath adds a fog mark and, on reaching the top, leaves no marks.

On the device (not automated):

- Blowing makes the fog rise; talking, music and a quiet room do not.
- Denying the permission shows "Hold to breathe", and holding fogs the glass.
- Record the clip and review it.

## Out of scope

- Real microphone input on iOS (the stub stays until a follow-up).
- A frost mask bitmap, droplets, slow refrosting over time, and sound output.
- Brighter "fresh fog" that settles into frost.

## Amendment (2026-09-30, after testing on the phone)

Testing on a real phone changed three things. Where this section and the sections above disagree,
this section wins.

- **The glass starts clear, and a breath frosts it.** Blowing on glass that is already frosted
  shows nothing, so the first version looked broken. `FrostState(startClear = true)` begins with a
  `Thaw` mark at full strength; a breath reaching the top drops it, and the glass is frosted.
- **The frost demo does not play its script on the phone.** `Demo.autoplay = false`: the glass is
  there to breathe on and wipe, and Replay plays the clip. Recording always plays it.
- **The clip is breath → oval scrub → pause → evaporate.** On the return to `select(0)` the frost
  melts away over one second (`beginThaw` / `setThawAmount`), back to the clear glass the loop
  starts from:

  | Time | Step |
  |---|---|
  | 0 s | `select(1)` |
  | 0.4–2.8 s | `breathe(2.4.seconds, strength = 1f)`: the glass frosts from the bottom |
  | 3.2–8.8 s | the oval scrub |
  | 10.8 s | `select(0)`: the frost evaporates |

- **Blow detection** tells breath from voice by periodicity (autocorrelation after pre-emphasis),
  not spectral flatness: on the phone a blow is a steep low rumble, which flatness scored as tonal.
  A blow must also reach -30 dBFS, the room's level is learned only from quiet frames, and the
  recorder's digitally silent first buffers are ignored.
