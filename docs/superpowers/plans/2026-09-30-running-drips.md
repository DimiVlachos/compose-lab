# Running Drips Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Occasional drops of condensation grow on the fogged mirror, run down in fits and starts, and
leave a thin wet streak, with scripted drips in the clip.

**Architecture:**
- A drip's streak is a thin `WipeStroke` with a new `clarity` (85% clear), so the existing fog
  pipeline, breaths included, handles it unchanged.
- A pure `DripDriver` in the core fog package owns timing, placement and motion. It exposes the beads,
  which `FoggedMirror` draws on top of the glass, read in the draw phase.
- `FogDemo` drives it: it waits with `delay` while nothing moves and uses frames only while a drop moves.
- The clip gets a new `drip(at, length)` script call.

**Tech Stack:** Kotlin Multiplatform, Compose Multiplatform 1.11.1, kotlin.test, compose ui-test (v2
`runComposeUiTest`).

**Spec:** `docs/superpowers/specs/2026-09-30-running-drips-design.md`

## Global Constraints

- **Timing:** drips start 4–8 s apart (uniform), with at most two growing or running at once.
- **Placement:** a drip starts only on fogged glass, in the top two thirds of the glass, at least 8% in
  from the sides.
- **Motion:**
  - it grows for about 0.6 s, then slides in 2–4 bursts, each speeding up to about 150 dp/s
  - it sticks for 0.2–0.8 s between bursts
  - it drifts less than 3 dp sideways and stops after 15–40% of the glass height, or at the bottom
- **Streak:** about 4 dp radius (3 dp for the first burst, so it's narrower at the top), clarity 0.85.
- **Bead:**
  - 5–7 dp across while running, 4 dp once resting
  - a darker inside, a darker lower rim and one bright highlight near the top
- **Clip:** no random drips in the recording or Replay; the clip's scripted drips never come near the
  heart.
- **Project rules:**
  - no `Color(0x` outside `core.presentation.ui`, no `println` or `android.util.Log`, no wildcard
    imports
  - core never imports feature packages (`fogdemo`, …)
  - previews are private
  - run `./gradlew spotlessApply` before every commit
- **Commands:**
  - tests: `./gradlew :composeApp:iosSimulatorArm64Test`
  - Konsist: `./gradlew :androidApp:testDebugUnitTest --rerun`
  - build: `./gradlew :androidApp:assembleDebug`
- **Commit trailer:**
  ```
  Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
  Claude-Session: https://claude.ai/code/session_01TkrycYKRAwVDjLBuvHiu6E
  ```

## Review Focus

1. **A full breath while a drop is running.** It keeps running through the fresh fog with a new streak,
   and its old streak goes. The test is in Task 2.
2. **A long gap between frames.** A resume, a stall, or a scripted `advance` of several seconds must not
   teleport a running drop; each step moves it at most 0.1 s worth. Task 2.
3. **A long session.** Resting beads must not pile up without limit: at most 12, oldest dropped first.
   Task 2.
4. **The glass changing size mid-drip.** Rotation or split screen: positions stay in fractions and the
   drop stays inside the glass. Task 2.
5. **Leaving mid-drip and coming back.** The drop carries on from where it was, rather than restarting
   or vanishing. Task 5.

---

## File Structure

- **Modify** `composeApp/src/commonMain/kotlin/dev/dimvlachos/lab/core/presentation/components/fog/FogState.kt`:
  `WipeStroke.clarity`, and `beginStroke(at, radius, clarity)`.
- **Modify** `.../core/presentation/components/fog/FoggedMirror.kt`:
  - `drawWipe` honours clarity
  - a new `beads: () -> List<Bead>` parameter, with `drawBead`
- **Modify** `.../core/presentation/components/fog/FogDimens.kt`: bead shade, rim and highlight alphas.
- **Create** `.../core/presentation/components/fog/DripDriver.kt`: `Bead`, `DripDriver`,
  `FogState.isFoggedAt`, `WipeStroke.covers`.
- **Modify** `.../core/demo/DemoController.kt` and `.../core/demo/DemoState.kt`: `drip(at, length)` and
  `setDripHandler`.
- **Modify** `composeApp/src/commonTest/.../core/demo/FakeController.kt`: record drips.
- **Modify** `.../fogdemo/presentation/components/FogDemo.kt`: own and drive a `DripDriver`, pass
  `beads`, and handle the script's drips.
- **Modify** `.../fogdemo/FogDemos.kt`: two scripted drips, and a later closing breath.
- **Tests:**
  - modify `FogStateTest.kt` and `FogLayersUiTest.kt`
  - create `DripDriverTest.kt`, `BeadUiTest.kt` and `FogDemoDripUiTest.kt`
  - modify `DemoStateTest.kt` and `FogDemosTest.kt`

Paths below abbreviate `composeApp/src/commonMain/kotlin/dev/dimvlachos/lab` as `main/` and
`composeApp/src/commonTest/kotlin/dev/dimvlachos/lab` as `test/`.

---

### Task 1: A stroke can clear the fog only part of the way

**Files:**
- Modify: `main/core/presentation/components/fog/FogState.kt`, for `beginStroke` and `WipeStroke`
- Modify: `main/core/presentation/components/fog/FoggedMirror.kt`, for the mark loop's `WipeStroke`
  branch and `drawWipe`
- Test: `test/core/presentation/components/fog/FogStateTest.kt` and `test/core/presentation/components/fog/FogLayersUiTest.kt`

**Interfaces:**
- Produces:
  - `FogState.beginStroke(at: Offset, radius: Dp? = null, clarity: Float = 1f): WipeStroke`
  - `WipeStroke.clarity: Float`, which is 1 for a full wipe

- [ ] **Step 1: Write the failing tests**

Append to `FogStateTest` (the class already exists):

```kotlin
    @Test
    fun aStrokeRemembersHowClearItWipes() {
        val fog = FogState()
        assertEquals(1f, fog.beginStroke(Offset(0.5f, 0.5f)).clarity)
        assertEquals(0.85f, fog.beginStroke(Offset(0.5f, 0.5f), clarity = 0.85f).clarity)
    }
```

Append to `FogLayersUiTest`:

```kotlin
    @Test
    fun aPartClearStrokeLeavesSomeOfTheFog() = runComposeUiTest {
        val fog = FogState()
        fog.beginStroke(Offset(0.1f, 0.3f)).also { fog.extendStroke(it, Offset(0.9f, 0.3f)) }
        fog.beginStroke(Offset(0.1f, 0.7f), clarity = 0.85f).also {
            fog.extendStroke(it, Offset(0.9f, 0.7f))
        }
        setContent {
            FoggedMirror(
                photo = ColorPainter(Color.Red),
                state = fog,
                modifier = Modifier.size(200.dp).testTag("window"),
            )
        }
        val pixels = onNodeWithTag("window").captureToImage().toPixelMap()
        fun greenAt(y: Float) = pixels[pixels.width / 2, (y * (pixels.height - 1)).toInt()].green

        val clear = greenAt(0.3f)
        val fogged = greenAt(0.5f)
        val wet = greenAt(0.7f)
        assertTrue(wet > clear + 0.01f, "some fog is left: $wet over clear $clear")
        assertTrue(wet < fogged * 0.4f, "most of it is gone: $wet under fogged $fogged")
    }
```

- [ ] **Step 2: Run the tests to verify they fail**

Run: `./gradlew :composeApp:iosSimulatorArm64Test --tests '*FogStateTest*' --tests '*FogLayersUiTest*'`
Expected: compilation FAILS with "No parameter with name 'clarity' found" and "Unresolved reference 'clarity'".

- [ ] **Step 3: Implement**

In `FogState.kt`, replace `beginStroke` and the `WipeStroke` class:

```kotlin
    /**
     * Starts a stroke and returns it. Each finger extends only its own, so a real finger and the
     * script's can wipe at once without joining up. [clarity] is how much of the fog it clears: 1
     * wipes it away, less leaves a wet film, as a running drop does.
     */
    fun beginStroke(at: Offset, radius: Dp? = null, clarity: Float = 1f): WipeStroke {
        val stroke = WipeStroke(mutableStateListOf(at), radius, clarity)
        _marks += stroke
        return stroke
    }
```

```kotlin
/**
 * One finger's stroke on a [FogState], from [FogState.beginStroke]; [radius] its own brush, a
 * fingertip say, or null for the window's; [clarity] how much of the fog it clears.
 */
class WipeStroke
internal constructor(
    internal val points: SnapshotStateList<Offset>,
    val radius: Dp? = null,
    val clarity: Float = 1f,
) : FogMark
```

In `FoggedMirror.kt`, change the `WipeStroke` branch of the mark loop to pass clarity:

```kotlin
                            is WipeStroke ->
                                when (val own = mark.radius?.toPx()) {
                                    null -> drawWipe(mark.points, radius, brush, mark.clarity)
                                    else -> drawWipe(mark.points, own, softBrush(own), mark.clarity)
                                }
```

Replace `drawWipe`:

```kotlin
private fun DrawScope.drawWipe(stroke: List<Offset>, radius: Float, brush: Brush, clarity: Float) {
    val pixels = stroke.map { Offset(it.x * size.width, it.y * size.height) }
    val dabs = wipeDabs(pixels, radius * FogDimens.DabSpacingRatio)
    if (clarity >= 1f) {
        for (dab in dabs) {
            translate(dab.x, dab.y) {
                drawCircle(brush, radius, Offset.Zero, blendMode = BlendMode.DstOut)
            }
        }
        return
    }
    // Part of the way: the dabs join up in a layer of their own, which then clears the fog only
    // [clarity] of the way, so overlapping dabs cannot compound past it.
    drawIntoCanvas { canvas ->
        canvas.saveLayer(
            Rect(Offset.Zero, size),
            Paint().apply {
                blendMode = BlendMode.DstOut
                alpha = clarity
            },
        )
        for (dab in dabs) translate(dab.x, dab.y) { drawCircle(brush, radius, Offset.Zero) }
        canvas.restore()
    }
}
```

`Rect`, `Paint` and `drawIntoCanvas` are already imported in `FoggedMirror.kt`, because `drawFog` uses
them.

- [ ] **Step 4: Run the tests to verify they pass**

Run: `./gradlew spotlessApply :composeApp:iosSimulatorArm64Test`
Expected: BUILD SUCCESSFUL. All tests pass, including the existing fog layer tests, since a full
stroke draws exactly as before.

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "feat: a wipe can clear the fog only part of the way, as a wet streak does"
```

---

### Task 2: DripDriver, drops that gather, run and rest

**Files:**
- Create: `main/core/presentation/components/fog/DripDriver.kt`
- Test: `test/core/presentation/components/fog/DripDriverTest.kt`

**Interfaces:**
- Consumes:
  - `FogState.beginStroke(at, radius, clarity)` and `WipeStroke.clarity` (Task 1)
  - `FogState.marks`, and `WipeStroke.points`, which is internal to the same module
- Produces:
  - `data class Bead(val at: Offset, val radius: Dp, val resting: Boolean)`
  - `class DripDriver(fog: FogState, random: Random = Random.Default, wipeRadius: Dp = FogDimens.BrushRadius)`, with:
    - `var glass: DpSize`
    - `var randomStarts: Boolean`
    - `val beads: List<Bead>` (a snapshot read)
    - `val moving: Boolean` (snapshot state)
    - `val secondsToNextStart: Float?`
    - `fun drip(at: Offset, length: Float)`
    - `fun advance(seconds: Float)`
  - `internal fun FogState.isFoggedAt(point: Offset, glass: DpSize, wipeRadius: Dp): Boolean`

- [ ] **Step 1: Write the failing tests**

Create `test/core/presentation/components/fog/DripDriverTest.kt`:

```kotlin
package dev.dimvlachos.lab.core.presentation.components.fog

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

// Condensation on a 400 × 800 dp mirror, stepped at 60 frames a second.
class DripDriverTest {
    private val step = 1 / 60f

    private fun driver(fog: FogState = FogState(), seed: Int = 1, randomStarts: Boolean = false) =
        DripDriver(fog, Random(seed)).apply {
            glass = DpSize(400.dp, 800.dp)
            this.randomStarts = randomStarts
        }

    private fun DripDriver.run(seconds: Float, each: (Float) -> Unit = {}) {
        var time = 0f
        while (time < seconds) {
            advance(step)
            time += step
            each(time)
        }
    }

    private fun FogState.streaks() = marks.filterIsInstance<WipeStroke>().filter { it.clarity < 1f }

    @Test
    fun dropsStartByThemselvesFourToEightSecondsApart() {
        val drips = driver(randomStarts = true)
        val starts = mutableListOf<Float>()
        var running = 0
        drips.run(60f) { time ->
            val now = drips.beads.count { !it.resting }
            if (now > running) starts += time
            running = now
        }
        assertTrue(starts.size >= 6, "starts: $starts")
        for ((a, b) in starts.zipWithNext()) {
            assertTrue(b - a in 3.95f..8.05f, "a gap of ${b - a} s in $starts")
        }
    }

    @Test
    fun aDropGrowsInPlaceBeforeItRuns() {
        val fog = FogState()
        val drips = driver(fog)
        drips.drip(Offset(0.5f, 0.2f), 0.3f)
        drips.run(0.4f)

        val bead = drips.beads.single()
        assertEquals(Offset(0.5f, 0.2f), bead.at)
        assertTrue(bead.radius < 3.5.dp, "still swelling: ${bead.radius}")
        assertTrue(fog.streaks().isEmpty())
    }

    @Test
    fun aDropOnlyRunsDownAndStaysOnItsLine() {
        val drips = driver()
        drips.drip(Offset(0.5f, 0.2f), 0.3f)
        var lastY = 0.2f
        drips.run(12f) {
            val at = drips.beads.single().at
            assertTrue(at.y >= lastY, "it never climbs: ${at.y} after $lastY")
            assertTrue(abs(at.x - 0.5f) * 400 < 3f, "it drifts under 3 dp: ${at.x}")
            lastY = at.y
        }
        val bead = drips.beads.single()
        assertTrue(bead.resting)
        assertTrue(abs(bead.at.y - 0.5f) < 0.001f, "it runs its whole length: ${bead.at.y}")
        assertEquals(2.dp, bead.radius)
    }

    @Test
    fun aDropStopsAtTheBottomEdge() {
        val drips = driver()
        drips.drip(Offset(0.5f, 0.9f), 0.4f)
        drips.run(12f)
        assertTrue(drips.beads.single().at.y <= 1f)
    }

    @Test
    fun itsStreakIsAThinWetStrokeNarrowerAtTheTop() {
        val fog = FogState()
        val drips = driver(fog)
        drips.drip(Offset(0.5f, 0.2f), 0.3f)
        drips.run(12f)

        val streaks = fog.streaks()
        assertTrue(streaks.size >= 2, "one stroke per burst: ${streaks.size}")
        assertTrue(streaks.all { it.clarity == 0.85f })
        assertEquals(3.dp, streaks.first().radius)
        assertTrue(streaks.drop(1).all { it.radius == 4.dp })
    }

    @Test
    fun noDropStartsOnClearGlass() {
        val drips = driver(FogState(startClear = true), randomStarts = true)
        drips.run(30f)
        assertTrue(drips.beads.isEmpty())
    }

    @Test
    fun noDropStartsOnAWipedPatch() {
        val fog = FogState()
        // One enormous wipe over the whole mirror.
        fog.beginStroke(Offset(0.5f, 0.5f), radius = 2000.dp)
        val drips = driver(fog, randomStarts = true)
        drips.run(30f)
        assertTrue(drips.beads.isEmpty())
    }

    @Test
    fun atMostTwoDropsRunAtOnce() {
        // A tall mirror, so two long drops are still running when the next start comes due.
        val drips = driver(randomStarts = true).apply { glass = DpSize(400.dp, 8000.dp) }
        drips.drip(Offset(0.3f, 0.1f), 0.4f)
        drips.drip(Offset(0.7f, 0.1f), 0.4f)
        drips.run(9f) {
            assertTrue(drips.beads.count { !it.resting } <= 2, "${drips.beads}")
        }
    }

    @Test
    fun aFullBreathFogsOverARestingDrop() {
        val fog = FogState()
        val drips = driver(fog)
        drips.drip(Offset(0.5f, 0.2f), 0.2f)
        drips.run(10f)
        assertTrue(drips.beads.single().resting)

        fog.setBreathLevel(fog.beginBreath(), 1f)

        assertTrue(drips.beads.isEmpty(), "gone at once, before the next step")
    }

    @Test
    fun aWipeOverARestingDropClearsIt() {
        val fog = FogState()
        val drips = driver(fog)
        drips.drip(Offset(0.5f, 0.2f), 0.2f)
        drips.run(10f)
        val at = drips.beads.single().at

        fog.beginStroke(at).also { fog.extendStroke(it, at + Offset(0.01f, 0f)) }

        assertTrue(drips.beads.isEmpty())
    }

    @Test
    fun aDropRunningThroughAFullBreathCarriesOnWithAFreshStreak() {
        val fog = FogState()
        val drips = driver(fog)
        drips.drip(Offset(0.5f, 0.1f), 0.4f)
        drips.run(1.2f)
        val before = drips.beads.single().at.y

        fog.setBreathLevel(fog.beginBreath(), 1f)
        drips.run(10f)

        assertTrue(drips.beads.single().at.y > before, "still running")
        assertTrue(fog.streaks().isNotEmpty(), "a fresh streak through the new fog")
    }

    @Test
    fun aLongGapBetweenFramesDoesNotTeleportADrop() {
        val drips = driver()
        drips.drip(Offset(0.5f, 0.1f), 0.4f)
        drips.run(0.7f) // grown, just starting to run
        val before = drips.beads.single().at.y

        drips.advance(5f)

        // 0.1 s at most, at no more than 150 dp/s, on an 800 dp mirror.
        assertTrue(drips.beads.single().at.y - before <= 15f / 800f + 1e-4f)
    }

    @Test
    fun restingDropsAreCappedAtTwelve() {
        val drips = driver()
        repeat(20) { drips.drip(Offset(0.05f + it * 0.045f, 0.1f), 0.15f) }
        drips.run(15f)
        assertEquals(12, drips.beads.count { it.resting })
    }

    @Test
    fun resizingTheGlassMidRunKeepsTheDropOnIt() {
        val drips = driver()
        drips.drip(Offset(0.5f, 0.6f), 0.4f)
        drips.run(1.5f)
        drips.glass = DpSize(800.dp, 400.dp)
        drips.run(12f) {
            val at = drips.beads.single().at
            assertTrue(at.x in 0f..1f && at.y in 0f..1f, "$at")
        }
    }

    @Test
    fun nothingDripsBeforeTheGlassHasASize() {
        val drips = DripDriver(FogState(), Random(1))
        drips.run(20f)
        assertTrue(drips.beads.isEmpty())
        assertFalse(drips.moving)
    }

    @Test
    fun itKnowsWhenTheNextDropIsDue() {
        val drips = driver(randomStarts = true)
        assertTrue(drips.secondsToNextStart!! in 4f..8f)
        drips.randomStarts = false
        assertEquals(null, drips.secondsToNextStart)
    }
}
```

- [ ] **Step 2: Run the tests to verify they fail**

Run: `./gradlew :composeApp:iosSimulatorArm64Test --tests '*DripDriverTest*'`
Expected: compilation FAILS with "Unresolved reference 'DripDriver'".

- [ ] **Step 3: Implement**

Create `main/core/presentation/components/fog/DripDriver.kt`:

```kotlin
package dev.dimvlachos.lab.core.presentation.components.fog

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

/** A drop of water on the glass: [at] as a fraction of it, and [resting] once it has stopped. */
@Immutable
data class Bead(val at: Offset, val radius: Dp, val resting: Boolean)

/**
 * Condensation running down a [FogState]: every few seconds a drop gathers somewhere on the fogged
 * glass, grows heavy, and runs down in fits and starts, leaving a thin wet streak. Stopped, it rests
 * at the streak's end until a breath fogs it over or a wipe clears it. [advance] moves it all on;
 * [drip] starts a drop at a given spot, for a script.
 */
@Stable
class DripDriver(
    private val fog: FogState,
    private val random: Random = Random.Default,
    private val wipeRadius: Dp = FogDimens.BrushRadius,
) {
    /** The glass's size; nothing drips until it has one. */
    var glass: DpSize = DpSize.Zero

    /** Whether drops start by themselves every few seconds; off, only [drip] starts one. */
    var randomStarts: Boolean = true

    /** Whether a drop is growing or running, so the glass needs frames. */
    var moving: Boolean by mutableStateOf(false)
        private set

    /** Seconds until a drop starts by itself, or null while [randomStarts] is off. */
    val secondsToNextStart: Float?
        get() = if (randomStarts) untilNext else null

    // Bumped at every change, so a reader of [beads] redraws when the drops move.
    private var version by mutableIntStateOf(0)
    private val running = mutableListOf<Drip>()
    private val resting = mutableListOf<Drip>()
    private val streaks = mutableSetOf<WipeStroke>()
    private var untilNext = nextGap()

    /**
     * The drops to draw. A resting drop that a breath has fogged over or a wipe has cleared is
     * left out at once, as the fog changes, not at the next step.
     */
    val beads: List<Bead>
        get() {
            version
            return resting.filter { it.stillShows() }.map { it.bead() } + running.map { it.bead() }
        }

    /** Starts a drop at [at], to run [length] of the glass's height. */
    fun drip(at: Offset, length: Float) {
        running += Drip(at, length)
        changed()
    }

    fun advance(seconds: Float) {
        if (seconds <= 0f || glass.width <= 0.dp || glass.height <= 0.dp) return
        if (randomStarts) {
            untilNext -= seconds
            if (untilNext <= 0f) {
                untilNext = nextGap()
                if (running.size < MaxRunning) {
                    spot()?.let { running += Drip(it, random.between(MinLength, MaxLength)) }
                }
            }
        }
        // However long since the last step, a drop moves at most a short step's worth.
        val stepSeconds = min(seconds, MaxStepSeconds)
        for (drip in running.toList()) {
            drip.advance(stepSeconds)
            if (drip.stopped) {
                running -= drip
                resting += drip
            }
        }
        resting.removeAll { !it.stillShows() }
        while (resting.size > MaxResting) resting.removeAt(0)
        changed()
    }

    private fun changed() {
        version++
        moving = running.isNotEmpty()
    }

    private fun nextGap() = random.between(MinGapSeconds, MaxGapSeconds)

    // A fogged spot in the top two thirds, away from the sides; none after a few tries skips a turn.
    private fun spot(): Offset? =
        (1..SpotTries)
            .asSequence()
            .map {
                Offset(
                    random.between(SideMargin, 1f - SideMargin),
                    random.between(TopMargin, HighestStart),
                )
            }
            .firstOrNull { fog.isFoggedAt(it, glass, wipeRadius) }

    private inner class Drip(private val start: Offset, length: Float) {
        private val diameter = random.between(MinBeadDp, MaxBeadDp)
        private val wobble = random.between(0.5f, MaxWobbleDp)
        private val phase = random.between(0f, 2 * PI.toFloat())
        // Where each burst ends, down to the drop's length or the bottom edge.
        private val stops: List<Float> = run {
            val end = min(start.y + length, 1f)
            val cuts = List(random.nextInt(MinBursts, MaxBursts + 1) - 1) { random.nextFloat() }
            (cuts.sorted() + 1f).map { start.y + (end - start.y) * it }
        }
        private var grown = 0f
        private var burst = 0
        private var speed = 0f
        private var stuck = 0f
        private var streak: WipeStroke? = null
        private var lastStreak: WipeStroke? = null
        private var head = start
        var stopped = false
            private set

        fun advance(seconds: Float) {
            if (grown < GrowSeconds) {
                grown += seconds
                return
            }
            if (stuck > 0f) {
                stuck -= seconds
                return
            }
            // A breath may have fogged over the streak so far: carry on with a fresh one.
            val current =
                streak?.takeIf { s -> fog.marks.any { it === s } }
                    ?: fog.beginStroke(head, streakRadius(), StreakClarity).also {
                        streak = it
                        lastStreak = it
                        streaks += it
                    }
            speed = min(MaxSpeedDp, speed + AccelerationDp * seconds)
            val y = min(head.y + speed * seconds / glass.height.value, stops[burst])
            // A gentle wave about the line it started on, never more than 2 × wobble off it.
            val travelled = (y - start.y) * glass.height.value
            val x =
                start.x +
                    (sin(phase + travelled / WaveLengthDp * 2 * PI.toFloat()) - sin(phase)) *
                        wobble / glass.width.value
            head = Offset(x.coerceIn(0f, 1f), y)
            fog.extendStroke(current, head)
            if (y >= stops[burst]) {
                burst++
                speed = 0f
                streak = null
                if (burst == stops.size) stopped = true
                else stuck = random.between(MinStickSeconds, MaxStickSeconds)
            }
        }

        private fun streakRadius() = if (burst == 0) TopStreakRadius else StreakRadius

        fun bead() =
            Bead(
                head,
                radius =
                    when {
                        stopped -> (RestingBeadDp / 2).dp
                        grown < GrowSeconds -> (diameter / 2 * (grown / GrowSeconds)).dp
                        else -> (diameter / 2).dp
                    },
                resting = stopped,
            )

        // Shows until a breath drops its streak or a real wipe after it passes over the drop.
        fun stillShows(): Boolean {
            val last = lastStreak ?: return true
            val index = fog.marks.indexOfFirst { it === last }
            if (index < 0) return false
            return fog.marks.drop(index + 1).none {
                it is WipeStroke && it !in streaks && it.covers(head, glass, wipeRadius)
            }
        }
    }
}

/**
 * Whether [point] is surely under fog: nothing has evaporated since the glass was last fogged
 * over, and no wipe passes over it. Partial breaths are not counted, which errs towards skipping a
 * drop, never towards one on clear glass.
 */
internal fun FogState.isFoggedAt(point: Offset, glass: DpSize, wipeRadius: Dp): Boolean =
    marks.none { it is Evaporation } &&
        marks.none { it is WipeStroke && it.covers(point, glass, wipeRadius) }

internal fun WipeStroke.covers(point: Offset, glass: DpSize, wipeRadius: Dp): Boolean {
    val reach = (radius ?: wipeRadius).value
    return points.any {
        hypot((it.x - point.x) * glass.width.value, (it.y - point.y) * glass.height.value) <= reach
    }
}

private fun Random.between(from: Float, until: Float) = from + nextFloat() * (until - from)

private const val MinGapSeconds = 4f
private const val MaxGapSeconds = 8f
private const val MaxRunning = 2
private const val MaxResting = 12
private const val SpotTries = 8
private const val SideMargin = 0.08f
private const val TopMargin = 0.05f
private const val HighestStart = 2f / 3f
private const val MinLength = 0.15f
private const val MaxLength = 0.4f
private const val MinBursts = 2
private const val MaxBursts = 4
private const val GrowSeconds = 0.6f
private const val MinStickSeconds = 0.2f
private const val MaxStickSeconds = 0.8f
private const val MaxStepSeconds = 0.1f
private const val MaxSpeedDp = 150f
private const val AccelerationDp = 600f
private const val MinBeadDp = 5f
private const val MaxBeadDp = 7f
private const val RestingBeadDp = 4f
private const val MaxWobbleDp = 1.4f
private const val WaveLengthDp = 60f
private const val StreakClarity = 0.85f
private val StreakRadius = 4.dp
private val TopStreakRadius = 3.dp
```

- [ ] **Step 4: Run the tests to verify they pass**

Run: `./gradlew spotlessApply :composeApp:iosSimulatorArm64Test --tests '*DripDriverTest*'`
Expected: all 16 tests PASS.

- [ ] **Step 5: Run the whole suite and Konsist**

Run: `./gradlew :composeApp:iosSimulatorArm64Test :androidApp:testDebugUnitTest --rerun`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 6: Commit**

```bash
git add -A
git commit -m "feat: drops of condensation gather, run down in fits and starts and rest"
```

---

### Task 3: The glass draws its drops

**Files:**
- Modify: `main/core/presentation/components/fog/FoggedMirror.kt`, for the signature, a layer after
  the fog `Box`, and a new `drawBead`
- Modify: `main/core/presentation/components/fog/FogDimens.kt`
- Test: `test/core/presentation/components/fog/BeadUiTest.kt`

**Interfaces:**
- Consumes: `Bead` (Task 2).
- Produces: a `FoggedMirror(..., onHoldChange = ..., beads: () -> List<Bead> = { emptyList() })` last
  parameter, read in the draw phase only.

- [ ] **Step 1: Write the failing test**

Create `test/core/presentation/components/fog/BeadUiTest.kt`:

```kotlin
package dev.dimvlachos.lab.core.presentation.components.fog

import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertTrue

// A drop, drawn big on clear red glass: darker inside, with a bright highlight near its top.
@OptIn(ExperimentalTestApi::class)
class BeadUiTest {
    @Test
    fun aDropIsDarkerInsideWithABrightHighlight() = runComposeUiTest {
        setContent {
            FoggedMirror(
                photo = ColorPainter(Color.Red),
                state = FogState(startClear = true),
                modifier = Modifier.size(200.dp).testTag("window"),
                beads = { listOf(Bead(Offset(0.5f, 0.5f), 40.dp, resting = false)) },
            )
        }
        val pixels = onNodeWithTag("window").captureToImage().toPixelMap()
        val centre = Offset(pixels.width / 2f, pixels.height / 2f)
        val radius = pixels.width * 40f / 200f
        fun at(offset: Offset) = pixels[(centre.x + offset.x).toInt(), (centre.y + offset.y).toInt()]

        val inside = at(Offset(radius * 0.3f, radius * 0.2f))
        val outside = at(Offset(radius * 1.6f, 0f))
        val highlight = at(Offset(-radius * 0.3f, -radius * 0.35f))
        assertTrue(inside.red < outside.red - 0.05f, "inside ${inside.red}, outside ${outside.red}")
        assertTrue(highlight.green > 0.5f, "a white glint on red: ${highlight.green}")
    }

    @Test
    fun withoutDropsTheGlassIsUnchanged() = runComposeUiTest {
        setContent {
            FoggedMirror(
                photo = ColorPainter(Color.Red),
                state = FogState(startClear = true),
                modifier = Modifier.size(200.dp).testTag("window"),
            )
        }
        val pixels = onNodeWithTag("window").captureToImage().toPixelMap()
        val middle = pixels[pixels.width / 2, pixels.height / 2]
        assertTrue(middle.red > 0.9f && middle.green < 0.1f, "$middle")
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `./gradlew :composeApp:iosSimulatorArm64Test --tests '*BeadUiTest*'`
Expected: compilation FAILS with "No parameter with name 'beads' found".

- [ ] **Step 3: Implement**

Append to `FogDimens` (inside the object, beside the other alphas):

```kotlin
    /** How much a drop darkens the glass behind it. */
    const val BeadShade = 0.14f

    /** The darker rim along a drop's lower edge. */
    const val BeadRim = 0.3f

    /** The bright glint near a drop's top. */
    const val BeadHighlight = 0.75f
```

In `FoggedMirror.kt`, add the parameter after `onHoldChange` and document it in the KDoc:

```kotlin
    onHoldChange: ((Boolean) -> Unit)? = null,
    beads: () -> List<Bead> = { emptyList() },
) {
```

KDoc line to add to the function's comment: ` * [beads] are drops of water on the glass, read while drawing, so a moving drop only redraws.`

At the end of the outer `Box`'s content, after the fog `Box { ... }`, add:

```kotlin
        // Drops sit on the glass, over the fog and the clear patches alike.
        Box(Modifier.matchParentSize().drawBehind { for (bead in beads()) drawBead(bead) })
```

Add below `softBrush`:

```kotlin
// A drop of water on glass: the scene behind it a little darker, a darker rim along its lower
// edge, where it gathers, and one bright glint near its top.
private fun DrawScope.drawBead(bead: Bead) {
    val radius = bead.radius.toPx()
    if (radius <= 0f) return
    val centre = Offset(bead.at.x * size.width, bead.at.y * size.height)
    drawCircle(Color.Black.copy(alpha = FogDimens.BeadShade), radius, centre)
    drawArc(
        Color.Black.copy(alpha = FogDimens.BeadRim),
        startAngle = 20f,
        sweepAngle = 140f,
        useCenter = false,
        topLeft = centre - Offset(radius, radius),
        size = Size(radius * 2, radius * 2),
        style = Stroke(width = radius * 0.35f),
    )
    drawCircle(
        Color.White.copy(alpha = FogDimens.BeadHighlight),
        radius * 0.3f,
        centre + Offset(-radius * 0.3f, -radius * 0.35f),
    )
}
```

Imports to add to `FoggedMirror.kt`, where they are missing:
- `androidx.compose.ui.draw.drawBehind`
- `androidx.compose.ui.geometry.Size`
- `androidx.compose.ui.graphics.drawscope.Stroke`

- [ ] **Step 4: Run the tests to verify they pass**

Run: `./gradlew spotlessApply :composeApp:iosSimulatorArm64Test`
Expected: BUILD SUCCESSFUL, all tests pass.

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "feat: the fogged glass draws its drops of water"
```

---

### Task 4: A script can start a drip

**Files:**
- Modify: `main/core/demo/DemoController.kt` and `main/core/demo/DemoState.kt`
- Modify: `test/core/demo/FakeController.kt`
- Test: `test/core/demo/DemoStateTest.kt`

**Interfaces:**
- Produces:
  - `DemoController.drip(at: Offset, length: Float)`, a suspend function
  - `DemoState.setDripHandler(handler: (suspend (Offset, Float) -> Unit)?)`
  - `FakeController.drips: MutableList<Pair<Offset, Float>>`, which also logs `"drip(...)"` in `calls`

- [ ] **Step 1: Write the failing tests**

Append to `DemoStateTest`:

```kotlin
    @Test
    fun aDripGoesToTheDemosHandler() = runTest {
        val state = DemoState()
        val drips = mutableListOf<Pair<Offset, Float>>()
        state.setDripHandler { at, length -> drips += at to length }

        state.drip(Offset(0.1f, 0.2f), 0.3f)

        assertEquals(listOf(Offset(0.1f, 0.2f) to 0.3f), drips)
    }

    @Test
    fun aDripWithNoHandlerIsIgnored() = runTest {
        DemoState().drip(Offset(0.5f, 0.5f), 0.2f)
    }
```

- [ ] **Step 2: Run the tests to verify they fail**

Run: `./gradlew :composeApp:iosSimulatorArm64Test --tests '*DemoStateTest*'`
Expected: compilation FAILS with "Unresolved reference 'setDripHandler'".

- [ ] **Step 3: Implement**

In `DemoController`, add after `breathe`:

```kotlin
    /**
     * Starts a drop of condensation at [at], a fraction of the stage, to run [length] of its
     * height. Returns at once; the drop runs on its own.
     */
    suspend fun drip(at: Offset, length: Float)
```

In `DemoState`, add the handler field, the override and the setter, following the `breathe` ones:

```kotlin
    private var dripHandler: (suspend (Offset, Float) -> Unit)? = null
```

```kotlin
    override suspend fun drip(at: Offset, length: Float) {
        dripHandler?.invoke(at, length)
    }
```

```kotlin
    fun setDripHandler(handler: (suspend (Offset, Float) -> Unit)?) {
        dripHandler = handler
    }
```

In `FakeController`, add:

```kotlin
    val drips = mutableListOf<Pair<Offset, Float>>()
```

```kotlin
    override suspend fun drip(at: Offset, length: Float) {
        drips += at to length
        calls += now() to "drip($at, $length)"
    }
```

- [ ] **Step 4: Run the tests to verify they pass**

Run: `./gradlew spotlessApply :composeApp:iosSimulatorArm64Test`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "feat: a demo script can start a drip"
```

---

### Task 5: The mirror drips while you look at it

**Files:**
- Modify: `main/fogdemo/presentation/components/FogDemo.kt`
- Test: `test/fogdemo/presentation/components/FogDemoDripUiTest.kt`

**Interfaces:**
- Consumes:
  - `DripDriver` (Task 2)
  - `FoggedMirror(beads = ...)` (Task 3)
  - `DemoState.setDripHandler` (Task 4)
  - `clipFrameToWindow`, and `FogDemos.FingerBrush`, which is 36 dp
- Produces: FogDemo runs drips on its glass. Random starts are off when `state.recording` or
  `state.replay`, and the script's drips play through the same driver.

- [ ] **Step 1: Write the failing tests**

Create `test/fogdemo/presentation/components/FogDemoDripUiTest.kt`:

```kotlin
package dev.dimvlachos.lab.fogdemo.presentation.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.compose.LocalLifecycleOwner
import dev.dimvlachos.lab.core.audio.MicAccess
import dev.dimvlachos.lab.core.camera.CameraAccess
import dev.dimvlachos.lab.core.demo.DemoState
import dev.dimvlachos.lab.core.presentation.components.fog.FogState
import dev.dimvlachos.lab.core.presentation.components.fog.WipeStroke
import dev.dimvlachos.lab.core.presentation.ui.LabTheme
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalTestApi::class)
class FogDemoDripUiTest {
    private class Owner(state: Lifecycle.State = Lifecycle.State.RESUMED) : LifecycleOwner {
        override val lifecycle = LifecycleRegistry.createUnsafe(this).apply { currentState = state }
    }

    private var scope: CoroutineScope? = null

    private fun ComposeUiTest.showDemo(state: DemoState, fog: FogState, owner: Owner = Owner()) {
        mainClock.autoAdvance = false
        setContent {
            scope = rememberCoroutineScope()
            CompositionLocalProvider(LocalLifecycleOwner provides owner) {
                LabTheme {
                    Box(Modifier.size(400.dp, 500.dp)) {
                        FogDemo(state, fog, MicAccess.Unavailable, CameraAccess.Unavailable)
                    }
                }
            }
        }
        mainClock.advanceTimeByFrame()
    }

    private fun FogState.streaks() = marks.filterIsInstance<WipeStroke>().filter { it.clarity < 1f }

    @Test
    fun aDropRunsDownTheFoggedMirrorByItself() = runComposeUiTest {
        val fog = FogState()
        showDemo(DemoState(), fog)
        mainClock.advanceTimeBy(10_000)
        assertTrue(fog.streaks().isNotEmpty(), "${fog.marks}")
    }

    @Test
    fun theRecordingStartsNoDropsByItself() = runComposeUiTest {
        val fog = FogState()
        showDemo(DemoState(recording = true), fog)
        mainClock.advanceTimeBy(20_000)
        assertTrue(fog.streaks().isEmpty(), "${fog.marks}")
    }

    @Test
    fun theScriptsDripRunsInTheRecording() = runComposeUiTest {
        val fog = FogState()
        val state = DemoState(recording = true)
        showDemo(state, fog)
        runOnUiThread { scope!!.launch { state.drip(Offset(0.5f, 0.2f), 0.3f) } }
        mainClock.advanceTimeBy(4_000)
        assertTrue(fog.streaks().isNotEmpty(), "${fog.marks}")
    }

    @Test
    fun noDropsWhileTheDemoIsNotInFront() = runComposeUiTest {
        val fog = FogState()
        showDemo(DemoState(), fog, Owner(Lifecycle.State.STARTED))
        mainClock.advanceTimeBy(20_000)
        assertTrue(fog.streaks().isEmpty(), "${fog.marks}")
    }

    @Test
    fun aDropLeftMidRunCarriesOnOnReturn() = runComposeUiTest {
        val fog = FogState()
        val state = DemoState(recording = true)
        val owner = Owner()
        showDemo(state, fog, owner)
        runOnUiThread { scope!!.launch { state.drip(Offset(0.5f, 0.1f), 0.4f) } }
        mainClock.advanceTimeBy(1_000)
        runOnUiThread { owner.lifecycle.currentState = Lifecycle.State.STARTED }
        mainClock.advanceTimeBy(100)
        val points = fog.streaks().sumOf { it.points.size }

        mainClock.advanceTimeBy(3_000)
        assertTrue(fog.streaks().sumOf { it.points.size } == points, "paused while away")

        runOnUiThread { owner.lifecycle.currentState = Lifecycle.State.RESUMED }
        mainClock.advanceTimeBy(3_000)
        assertTrue(fog.streaks().sumOf { it.points.size } > points, "carries on back in front")
    }
}
```

`WipeStroke.points` is `internal`, and this test lives in the same module's test source set, so it
can read it.

- [ ] **Step 2: Run the tests to verify they fail**

Run: `./gradlew :composeApp:iosSimulatorArm64Test --tests '*FogDemoDripUiTest*'`
Expected: `aDropRunsDownTheFoggedMirrorByItself`, `theScriptsDripRunsInTheRecording` and
`aDropLeftMidRunCarriesOnOnReturn` FAIL, because no streaks appear. The two "no drops" tests pass, and
they pin the negative behaviour from here on.

- [ ] **Step 3: Implement**

In `FogDemo.kt`, after `val driver = remember(fog) { BreathDriver(fog) }`:

```kotlin
    // Condensation running down the glass; in the clip, only the script's drops.
    val drips = remember(fog) { DripDriver(fog, wipeRadius = FogDemos.FingerBrush) }
    drips.randomStarts = !state.recording && !state.replay
    val density = LocalDensity.current
```

In the `DisposableEffect(state, fog, driver)` block, add a drip handler beside the breathe handler, and
clear it in `onDispose`:

```kotlin
        // The script's drop starts in the clip's frame, placed on whatever window this is.
        state.setDripHandler { at, length ->
            if (!window.isEmpty()) drips.drip(clipFrameToWindow(at, window), length)
        }
```

```kotlin
            state.setDripHandler(null)
```

After the camera block, add the drip loop:

```kotlin
    // Drops run only while the demo is in front of the user. Between drops there is nothing to
    // draw, so the loop sleeps until the next is due rather than asking for frames.
    val dripLifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(drips, dripLifecycle) {
        dripLifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (true) {
                if (!drips.moving) {
                    val wait = drips.secondsToNextStart
                    if (wait == null) {
                        snapshotFlow { drips.moving }.first { it }
                    } else {
                        delay((wait * 1000).toLong().coerceAtLeast(1))
                        drips.advance(wait)
                    }
                    continue
                }
                var previous = withFrameNanos { it }
                while (drips.moving) {
                    withFrameNanos { now ->
                        drips.advance((now - previous) / 1_000_000_000f)
                        previous = now
                    }
                }
            }
        }
    }
```

Give the driver the glass's size, and pass the beads, in the `FoggedMirror` call:

```kotlin
            modifier =
                Modifier.fillMaxSize().onSizeChanged {
                    window = it.toSize()
                    drips.glass = with(density) { DpSize(it.width.toDp(), it.height.toDp()) }
                },
```

```kotlin
            beads = { drips.beads },
```

Imports to add to `FogDemo.kt`:
- `androidx.compose.ui.platform.LocalDensity`
- `androidx.compose.ui.unit.DpSize`
- `dev.dimvlachos.lab.core.presentation.components.fog.DripDriver`
- `kotlinx.coroutines.delay`

`snapshotFlow`, `first`, `withFrameNanos`, `repeatOnLifecycle`, `Lifecycle` and `LocalLifecycleOwner`
are already imported.

- [ ] **Step 4: Run the tests to verify they pass**

Run: `./gradlew spotlessApply :composeApp:iosSimulatorArm64Test`
Expected: BUILD SUCCESSFUL.
- All `FogDemoDripUiTest` tests pass.
- The existing FogDemo tests pass unchanged. None of them runs 4 s of virtual time on fogged glass,
  which is the earliest a drop can start.

- [ ] **Step 5: Konsist and the Android build**

Run: `./gradlew :androidApp:testDebugUnitTest --rerun :androidApp:assembleDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 6: Commit**

```bash
git add -A
git commit -m "feat: drops of condensation run down the fogged mirror while you look at it"
```

---

### Task 6: The clip's drips

**Files:**
- Modify: `main/fogdemo/FogDemos.kt`
- Test: `test/fogdemo/FogDemosTest.kt`

**Interfaces:**
- Consumes:
  - `DemoController.drip` and `FakeController.drips` (Task 4)
  - `DripDriver` (Task 2)
  - `heartWithArrow()`, whose strokes are in clip-frame fractions
- Produces: the clip. It draws the heart, then drips twice, clear of the heart, then breathes 5 s after
  the drawing ends.

- [ ] **Step 1: Write the failing tests**

Add to `FogDemosTest`. The imports it needs:
- `androidx.compose.ui.geometry.Offset`
- `androidx.compose.ui.unit.DpSize`
- `androidx.compose.ui.unit.dp`
- `dev.dimvlachos.lab.core.presentation.components.fog.DripDriver`
- `kotlin.random.Random`

```kotlin
    @Test
    fun theClipDripsTwiceBetweenTheDrawingAndTheBreath() = runTest {
        val controller = FakeController { testScheduler.currentTime }
        FogDemos.all.single().script.play(controller)

        val calls = controller.calls.map { it.second }
        val lastWipe = calls.indexOfLast { it.startsWith("wipe") }
        val breath = calls.indexOfFirst { it.startsWith("breathe") }
        val drips = calls.indices.filter { calls[it].startsWith("drip") }
        assertEquals(2, drips.size, "$calls")
        assertTrue(drips.all { it in lastWipe..breath }, "$calls")
    }

    @Test
    fun theClipsDripsRunClearOfTheHeart() = runTest {
        val controller = FakeController { testScheduler.currentTime }
        FogDemos.all.single().script.play(controller)
        val drawing = heartWithArrow().flatMap { it.path }

        for ((at, length) in controller.drips) {
            var y = at.y
            while (y <= at.y + length) {
                val point = Offset(at.x, y)
                val nearest = drawing.minOf { (it - point).getDistance() }
                assertTrue(nearest > 0.06f, "a drip at $point comes $nearest from the heart")
                y += 0.01f
            }
        }
    }

    @Test
    fun theClipsDripsHaveStoppedBeforeTheBreath() = runTest {
        val controller = FakeController { testScheduler.currentTime }
        FogDemos.all.single().script.play(controller)
        val times = controller.calls.filter { it.second.startsWith("drip") }.map { it.first }
        val breathAt = controller.calls.first { it.second.startsWith("breathe") }.first

        // Whatever the drops' own randomness, over many seeds, on the clip's 400 × 500 frame.
        for (seed in 1..30) {
            val drips =
                DripDriver(newFogDemoState(), Random(seed)).apply {
                    glass = DpSize(400.dp, 500.dp)
                    randomStarts = false
                }
            var now = times.first()
            val pending = controller.drips.zip(times).toMutableList()
            while (now < breathAt) {
                pending.removeAll { (drip, at) ->
                    (at <= now).also { if (it) drips.drip(drip.first, drip.second) }
                }
                drips.advance(0.016f)
                now += 16
            }
            assertTrue(!drips.moving, "seed $seed: a drop still running at the breath")
        }
    }
```

In the existing `theClipsBreathFogsOverTheDrawingSoTheLoopNeedsNoReset`, add the drips' streaks
before the breath. Insert this right after the loop that replays `controller.wipes`:

```kotlin
        val dripper =
            DripDriver(fog, Random(1)).apply {
                glass = DpSize(400.dp, 500.dp)
                randomStarts = false
            }
        for ((at, length) in controller.drips) dripper.drip(at, length)
        repeat(600) { dripper.advance(1 / 60f) }
```

Then add this to that test's final assertion block:

```kotlin
        assertTrue(dripper.beads.isEmpty(), "a drop left on the fresh fog: ${dripper.beads}")
```

- [ ] **Step 2: Run the tests to verify they fail**

Run: `./gradlew :composeApp:iosSimulatorArm64Test --tests '*FogDemosTest*'`
Expected:
- `theClipDripsTwiceBetweenTheDrawingAndTheBreath` FAILS with "expected 2, actual 0".
- `theClipsDripsHaveStoppedBeforeTheBreath` FAILS on `times.first()` with "List is empty".
- The other two pass vacuously until the drips exist.

- [ ] **Step 3: Implement**

In `FogDemos.kt`, replace the script and the look constant:

```kotlin
    // Fogged glass: a beat, a fingertip draws a heart pierced by an arrow, two drops of
    // condensation run down either side of it while it is looked at, then a breath fogs it all
    // over, back to the fogged glass the loop starts from.
    private val wipeTour = demoScript {
        var start = 0.3.seconds
        for (stroke in drawing) {
            at(start) { wipe(stroke.path, stroke.duration) }
            start += stroke.duration + LiftBetweenStrokes
        }
        at(start + 0.1.seconds) { drip(Offset(0.12f, 0.1f), 0.18f) }
        at(start + 0.6.seconds) { drip(Offset(0.88f, 0.6f), 0.18f) }
        at(start + LookAtTheDrawing) { breathe(2.4.seconds, strength = 1f) }
    }
```

```kotlin
// Long enough for both drops to finish running before the breath.
private val LookAtTheDrawing = 5.seconds
```

Add `import androidx.compose.ui.geometry.Offset` to `FogDemos.kt`.

Why those spots are clear of the drawing, in clip-frame fractions:
- the heart spans x 0.26–0.74 and y 0.28–0.67
- the arrow runs from (0.18, 0.76) to (0.84, 0.18), with feathers at the lower left
- drip one falls along x = 0.12 from y 0.10 to 0.28, and drip two along x = 0.88 from y 0.60 to 0.78

`theClipsDripsRunClearOfTheHeart` is the check that decides this.

- [ ] **Step 4: Run the tests to verify they pass**

Run: `./gradlew spotlessApply :composeApp:iosSimulatorArm64Test`
Expected: BUILD SUCCESSFUL, all tests pass.

- [ ] **Step 5: Konsist and the Android build, then commit**

Run: `./gradlew :androidApp:testDebugUnitTest --rerun :androidApp:assembleDebug`
Expected: BUILD SUCCESSFUL.

```bash
git add -A
git commit -m "feat: the clip's mirror drips on either side of the heart"
```

---

## Verification

- **Tests:** `./gradlew :composeApp:iosSimulatorArm64Test`, all green.
- **Konsist:** `./gradlew :androidApp:testDebugUnitTest --rerun`.
- **Build:** `./gradlew :androidApp:assembleDebug`.
- **On the phone** (`./gradlew :androidApp:installDebug`, adb over Wi-Fi), with the user:
  - on the fogged mirror, a drop starts every few seconds and runs down in fits and starts, leaving a
    thin wet streak, then rests
  - blowing fogs the streaks and drops over
  - wiping over a resting drop clears it
  - Home and back: drops carry on
  - smoothness with the camera on
- **Clip:** Replay shows exactly two drips, either side of the heart, and none at random.
