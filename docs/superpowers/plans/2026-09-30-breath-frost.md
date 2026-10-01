# Breath Frost Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Blowing on the phone fogs the fogged mirror back over, with a soft cloud rising from the bottom edge; the recorded clip shows the same moment with a scripted breath that also closes its loop.

**Architecture:** `FrostState` becomes an ordered list of marks (wipes and breaths) replayed inside the existing offscreen frost layer: wipes erase with `DstOut`, breaths redraw the frost through a billowing fog mask. A shared `BlowDetector` turns microphone frames into a blow strength; a `BreathDriver` turns strength over time into a fog level. The microphone (Android `AudioRecord`, iOS stub), the hold-to-breathe gesture and the demo script all feed the same driver.

**Tech Stack:** Kotlin Multiplatform, Compose Multiplatform 1.11.1, Kotlin 2.4.10, Android minSdk 31, `AudioRecord`, `activity-compose` permission launcher, JetBrains `lifecycle-runtime-compose` 2.9.6, Kermit, kotlin-test + Compose UI test.

**Spec:** `docs/superpowers/specs/2026-09-30-breath-frost-design.md`

## Global Constraints

- Shared code lives in `composeApp/src/commonMain`; platform code only in `androidMain` / `iosMain`.
- Android minSdk is 31.
- Konsist (`androidApp/src/test/.../architecture/`) must stay green: no wildcard imports; `@Preview` functions private; no `println(`; no `android.util.Log` (use Kermit); no `GlobalScope`; no `Color(0x` outside `core.presentation.ui`; `core` never imports a feature package (`catalog`, `demo`, `navbardemo`, `gallerydemo`, `frostdemo`) or `App`; `core.presentation.components` never imports `core.demo`.
- Component numbers go in `FrostDimens` (`core/presentation/components/frost/FrostDimens.kt`).
- Formatting: run `./gradlew spotlessApply` before every commit; CI runs `spotlessCheck`.
- Test command for shared code: `./gradlew :composeApp:iosSimulatorArm64Test` (add `--tests '<pattern>'` to narrow). Konsist: `./gradlew :androidApp:testDebugUnitTest --rerun`. Android build: `./gradlew :androidApp:assembleDebug`.
- The permission request never happens in record mode; record mode shows no hints.
- Fog level is a fraction of the window height from 0 (bottom edge) to 1 (solid fog reaches the top); at 1 the breath and every earlier mark are dropped.
- A steady full-strength blow covers the glass in about 1.2 s; hold-to-breathe uses strength 0.7; the hold starts after 400 ms still within touch slop.
- Detector frames: 512 mono samples at 16 kHz. Onset after 3 blowing frames, release after 4 quiet frames.
- Clip: breath `breathe(2.4.seconds, strength = 1f)` at 7.4 s, `select(0)` at 10.2 s.
- Commit messages follow the repo style (`feat:`, `test:`, `docs:`), each ending with:
  ```
  Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
  Claude-Session: https://claude.ai/code/session_01TkrycYKRAwVDjLBuvHiu6E
  ```

## Review Focus

- A fan or vacuum switched on mid-session is loud, steady broadband noise, the same as a blow: it must stop counting as one after about 4 s, not fog the glass forever. Test in Task 4.
- A microphone that delivers only silence (another app or a phone call holding it) must fall back to hold-to-breathe instead of showing "Blow on the screen" while nothing works. Test in Task 9.
- Wiping while still blowing: the next blow frames start a new breath from the bottom that covers the new wipe, and the earlier fog stays where it was. Test in Task 5.
- Leaving the demo mid-hold must release the hold (`onHoldChange(false)`), so no breath keeps running. Test in Task 3.
- A quick tap with hold enabled neither wipes nor breathes. Test in Task 3.

---

## File Structure

| File | Responsibility |
|---|---|
| `core/presentation/components/frost/FrostState.kt` (modify) | Ordered marks: `WipeStroke`, `Breath`; breath levels; dropping covered marks |
| `core/presentation/components/frost/FogMask.kt` (create) | `DrawScope.drawFogMask(level)`: the fog's shape, in alpha |
| `core/presentation/components/frost/FoggedMirror.kt` (modify) | Replays marks; hold vs drag gesture |
| `core/presentation/components/frost/FrostDimens.kt` (modify) | `FogEdge`, `HoldDelayMillis` |
| `core/audio/AudioFormat.kt` (create) | `MicSampleRate`, `MicFrameSize`, `MicFrameSeconds` |
| `core/audio/Fft.kt` (create) | `powerSpectrum`, `spectralFlatness` |
| `core/audio/BlowDetector.kt` (create) | Frames in, blow strength out |
| `core/audio/Microphone.kt` (create) | `Microphone`, `MicAccess`, `expect rememberMicAccess` |
| `androidMain/.../core/audio/Microphone.android.kt` (create) | Permission launcher, `AndroidMicrophone` (`AudioRecord`) |
| `iosMain/.../core/audio/Microphone.ios.kt` (create) | Stub: `MicAccess.Unavailable` |
| `core/demo/DemoController.kt`, `DemoState.kt` (modify) | `breathe(duration, strength)`, `recording` |
| `demo/presentation/screen/DemoScreen.kt` (modify) | Passes `recording` to `DemoState` |
| `frostdemo/BreathDriver.kt` (create) | Strength over time to fog level; `scriptedBreathStrength` |
| `frostdemo/FrostDemos.kt` (modify) | Clip script with the breath |
| `frostdemo/presentation/components/FrostDemo.kt` (modify) | Script handlers, mic loop, hold fallback, hints |
| `androidApp/src/main/AndroidManifest.xml`, `composeApp/build.gradle.kts`, `gradle/libs.versions.toml`, `strings.xml` (modify) | Permission, dependencies, hint strings |

All Kotlin paths above are under `composeApp/src/commonMain/kotlin/dev/dimvlachos/lab/` unless they name another source set.

---

### Task 1: Ordered marks in `FrostState`

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/dev/dimvlachos/lab/core/presentation/components/frost/FrostState.kt`
- Test: `composeApp/src/commonTest/kotlin/dev/dimvlachos/lab/core/presentation/components/frost/FrostStateTest.kt`

**Interfaces:**
- Consumes: nothing new.
- Produces: `sealed interface FrostMark`; `class WipeStroke : FrostMark` (internal `points: SnapshotStateList<Offset>`); `class Breath : FrostMark { val level: Float }`; `FrostState.marks: List<FrostMark>`; `FrostState.strokes: List<List<Offset>>` (unchanged meaning); `beginStroke(at: Offset): WipeStroke`; `extendStroke(stroke: WipeStroke, to: Offset)`; `beginBreath(): Breath`; `setBreathLevel(breath: Breath, level: Float)`; `clear()`.

- [ ] **Step 1: Write the failing tests**

Append to `FrostStateTest` (keep the existing tests):

```kotlin
    @Test
    fun wipesAndBreathsKeepTheOrderTheyWereMadeIn() {
        val frost = FrostState()
        val first = frost.beginStroke(Offset(0.1f, 0.1f))
        val breath = frost.beginBreath()
        val second = frost.beginStroke(Offset(0.2f, 0.2f))

        assertEquals(listOf<FrostMark>(first, breath, second), frost.marks)
    }

    @Test
    fun aBreathRisesButNeverFalls() {
        val frost = FrostState()
        val breath = frost.beginBreath()
        frost.setBreathLevel(breath, 0.4f)
        frost.setBreathLevel(breath, 0.2f)

        assertEquals(0.4f, breath.level)
    }

    @Test
    fun aBreathReachingTheTopFrostsOverEverythingBeforeIt() {
        val frost = FrostState()
        frost.beginStroke(Offset(0.1f, 0.1f))
        val earlier = frost.beginBreath()
        frost.setBreathLevel(earlier, 0.3f)
        val breath = frost.beginBreath()
        val after = frost.beginStroke(Offset(0.5f, 0.5f))

        frost.setBreathLevel(breath, 1f)

        assertEquals(listOf<FrostMark>(after), frost.marks)
    }

    @Test
    fun aBreathDroppedByClearStaysGone() {
        val frost = FrostState()
        val breath = frost.beginBreath()
        frost.clear()
        frost.setBreathLevel(breath, 0.5f)

        assertTrue(frost.marks.isEmpty())
    }
```

- [ ] **Step 2: Run the tests to verify they fail**

Run: `./gradlew :composeApp:iosSimulatorArm64Test --tests '*FrostStateTest*'`
Expected: compilation fails with `Unresolved reference 'FrostMark'`, `'beginBreath'`, `'marks'`.

- [ ] **Step 3: Implement**

Replace `FrostState.kt` with:

```kotlin
package dev.dimvlachos.lab.core.presentation.components.frost

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.geometry.Offset

/**
 * The marks on a [FoggedMirror], in the order they were made: wipes that clear the frost and
 * breaths that fog it back over. The order is the point: a wipe after a breath clears its fog, a
 * breath after a wipe covers it. Wipe points are fractions of the window, so the marks stay put when
 * the window changes size.
 */
@Stable
class FrostState {
    private val _marks = mutableStateListOf<FrostMark>()

    val marks: List<FrostMark>
        get() = _marks

    /** Each wipe's points, oldest first. */
    val strokes: List<List<Offset>>
        get() = _marks.filterIsInstance<WipeStroke>().map { it.points }

    /**
     * Starts a stroke and returns it. Each finger extends only its own, so a real finger and the
     * script's can wipe at once without joining up.
     */
    fun beginStroke(at: Offset): WipeStroke {
        val stroke = WipeStroke(mutableStateListOf(at))
        _marks += stroke
        return stroke
    }

    /** Adds [to] to [stroke]; a stroke from before the last [clear] stays gone. */
    fun extendStroke(stroke: WipeStroke, to: Offset) {
        stroke.points += to
    }

    /** Starts a breath with its fog at the bottom edge, and returns it. */
    fun beginBreath(): Breath {
        val breath = Breath()
        _marks += breath
        return breath
    }

    /**
     * Raises [breath]'s fog to [level], a fraction of the window height from the bottom. Fog never
     * sinks, and a breath already dropped stays gone. At 1 the fog covers the window, which is then
     * fresh frost: the breath and every mark before it are dropped.
     */
    fun setBreathLevel(breath: Breath, level: Float) {
        val index = _marks.indexOf(breath)
        if (index < 0 || level <= breath.level) return
        breath.level = level
        if (level >= 1f) _marks.removeRange(0, index + 1)
    }

    /** Frosts the whole window over again. */
    fun clear() {
        _marks.clear()
    }
}

/** A wipe or a breath on a [FrostState]. */
sealed interface FrostMark

/** One finger's stroke on a [FrostState], from [FrostState.beginStroke]. */
class WipeStroke internal constructor(internal val points: SnapshotStateList<Offset>) : FrostMark

/** Fog rising from the bottom of a [FrostState], from [FrostState.beginBreath]. */
class Breath internal constructor() : FrostMark {
    var level by mutableFloatStateOf(0f)
        internal set
}
```

- [ ] **Step 4: Run the tests to verify they pass**

Run: `./gradlew :composeApp:iosSimulatorArm64Test`
Expected: BUILD SUCCESSFUL (the whole suite: `FoggedMirror` still compiles because `strokes` is unchanged).

- [ ] **Step 5: Commit**

```bash
./gradlew spotlessApply
git add composeApp/src/commonMain/kotlin/dev/dimvlachos/lab/core/presentation/components/frost/FrostState.kt composeApp/src/commonTest/kotlin/dev/dimvlachos/lab/core/presentation/components/frost/FrostStateTest.kt
git commit -m "feat: keep frost wipes and breaths in the order they were made" -m "Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
Claude-Session: https://claude.ai/code/session_01TkrycYKRAwVDjLBuvHiu6E"
```

---

### Task 2: Fog mask and replaying marks in `FoggedMirror`

**Files:**
- Create: `composeApp/src/commonMain/kotlin/dev/dimvlachos/lab/core/presentation/components/frost/FogMask.kt`
- Modify: `composeApp/src/commonMain/kotlin/dev/dimvlachos/lab/core/presentation/components/frost/FrostDimens.kt`
- Modify: `composeApp/src/commonMain/kotlin/dev/dimvlachos/lab/core/presentation/components/frost/FoggedMirror.kt`
- Test: `composeApp/src/commonTest/kotlin/dev/dimvlachos/lab/core/presentation/components/frost/FogMaskUiTest.kt`
- Test: `composeApp/src/commonTest/kotlin/dev/dimvlachos/lab/core/presentation/components/frost/FrostFogUiTest.kt`

**Interfaces:**
- Consumes: Task 1's `FrostState.marks`, `WipeStroke.points`, `Breath.level`.
- Produces: `internal fun DrawScope.drawFogMask(level: Float, edge: Float = FrostDimens.FogEdge)`; `FrostDimens.FogEdge = 0.12f`.

- [ ] **Step 1: Write the failing mask test**

Create `FogMaskUiTest.kt`:

```kotlin
package dev.dimvlachos.lab.core.presentation.components.frost

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertTrue

// The mask drawn in black on white: dark where the fog is, white where it is not.
@OptIn(ExperimentalTestApi::class)
class FogMaskUiTest {
    private fun fogAt(level: Float, x: Float, y: Float): Float {
        var fog = 0f
        runComposeUiTest {
            setContent {
                Box(
                    Modifier.size(100.dp)
                        .background(Color.White)
                        .drawBehind { drawFogMask(level) }
                        .testTag("mask")
                )
            }
            val pixels = onNodeWithTag("mask").captureToImage().toPixelMap()
            val pixel = pixels[(x * (pixels.width - 1)).toInt(), (y * (pixels.height - 1)).toInt()]
            fog = 1f - pixel.red
        }
        return fog
    }

    @Test
    fun belowTheFrontTheFogIsSolid() {
        assertTrue(fogAt(level = 0.5f, x = 0.5f, y = 0.9f) > 0.95f)
    }

    @Test
    fun wellAboveTheFrontThereIsNoFog() {
        assertTrue(fogAt(level = 0.5f, x = 0.5f, y = 0.1f) < 0.05f)
    }

    @Test
    fun aBreathNotYetBegunDrawsNothing() {
        assertTrue(fogAt(level = 0f, x = 0.5f, y = 0.99f) < 0.05f)
    }

    @Test
    fun aFullBreathCoversTheTop() {
        assertTrue(fogAt(level = 1f, x = 0.5f, y = 0.01f) > 0.95f)
    }
}
```

- [ ] **Step 2: Write the failing replay test**

Create `FrostFogUiTest.kt`. The photo is plain red: where the frost is cleared the window is pure red (green near 0); where frost or fog covers it, the pale film lifts green above 0.2.

```kotlin
package dev.dimvlachos.lab.core.presentation.components.frost

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

@OptIn(ExperimentalTestApi::class)
class FrostFogUiTest {
    private fun FrostState.wipeAcross(y: Float) {
        val stroke = beginStroke(Offset(0.1f, y))
        extendStroke(stroke, Offset(0.9f, y))
    }

    @Test
    fun fogCoversTheWipesBelowItsFrontAndLaterWipesClearIt() = runComposeUiTest {
        val frost = FrostState()
        frost.wipeAcross(0.2f)
        frost.wipeAcross(0.5f)
        // Solid fog up to 0.38 of the way down; its soft edge and puffs stop short of 0.2.
        frost.setBreathLevel(frost.beginBreath(), 0.62f)
        frost.wipeAcross(0.7f)
        setContent {
            FoggedMirror(
                photo = ColorPainter(Color.Red),
                state = frost,
                modifier = Modifier.size(200.dp).testTag("window"),
            )
        }
        val pixels = onNodeWithTag("window").captureToImage().toPixelMap()
        fun greenAt(y: Float) = pixels[pixels.width / 2, (y * (pixels.height - 1)).toInt()].green

        assertTrue(greenAt(0.2f) < 0.12f, "the wipe above the fog is still clear: ${greenAt(0.2f)}")
        assertTrue(greenAt(0.5f) > 0.2f, "the wipe under the fog is covered: ${greenAt(0.5f)}")
        assertTrue(greenAt(0.7f) < 0.12f, "the wipe after the breath clears it: ${greenAt(0.7f)}")
    }
}
```

- [ ] **Step 3: Run the tests to verify they fail**

Run: `./gradlew :composeApp:iosSimulatorArm64Test --tests '*Fog*'`
Expected: compilation fails with `Unresolved reference 'drawFogMask'`.

- [ ] **Step 4: Add the dimensions**

In `FrostDimens.kt`, add inside the object:

```kotlin
    /** The fog's soft edge, as a share of the window height. */
    const val FogEdge = 0.12f

    /** How long a press must stay still to breathe rather than wipe. */
    const val HoldDelayMillis = 400L
```

- [ ] **Step 5: Create the fog mask**

Create `FogMask.kt`:

```kotlin
package dev.dimvlachos.lab.core.presentation.components.frost

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope

// Puffs along the fog's front: where across the window, and how big against its soft edge. Fixed,
// so the front billows the same way every time.
private val Puffs =
    listOf(
        0.00f to 0.80f,
        0.13f to 0.60f,
        0.26f to 0.90f,
        0.39f to 0.55f,
        0.52f to 0.85f,
        0.65f to 0.65f,
        0.78f to 0.90f,
        0.90f to 0.60f,
        1.00f to 0.80f,
    )

/**
 * Draws, in alpha, where a breath at [level] has fogged the glass: solid below its front, a soft
 * band of [edge] of the height above it, and puffs along it so the front billows like a cloud.
 */
internal fun DrawScope.drawFogMask(level: Float, edge: Float = FrostDimens.FogEdge) {
    if (level <= 0f) return
    val front = (1f - level) * size.height
    val band = edge * size.height
    drawRect(Color.Black, topLeft = Offset(0f, front), size = Size(size.width, size.height - front))
    drawRect(
        Brush.verticalGradient(
            0f to Color.Transparent,
            1f to Color.Black,
            startY = front - band,
            endY = front,
        ),
        topLeft = Offset(0f, front - band),
        size = Size(size.width, band),
    )
    for ((x, scale) in Puffs) {
        val centre = Offset(x * size.width, front - band / 2)
        val radius = band * scale
        drawCircle(
            Brush.radialGradient(
                0f to Color.Black,
                1f to Color.Transparent,
                center = centre,
                radius = radius,
            ),
            radius,
            centre,
        )
    }
}
```

- [ ] **Step 6: Replay the marks in `FoggedMirror`**

In `FoggedMirror.kt`, replace the whole `.drawWithContent { ... }` block on the offscreen `Box` (the one after `.graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }`) with:

```kotlin
                .drawWithContent {
                    drawContent()
                    val radius = brushRadius.toPx()
                    // One brush, centred on the origin and moved to each dab, not a gradient per
                    // dab.
                    val brush = softBrush(radius)
                    for (mark in state.marks) {
                        when (mark) {
                            is WipeStroke -> drawWipe(mark.points, radius, brush)
                            is Breath -> drawFog(mark.level)
                        }
                    }
                }
```

Add these private functions at the bottom of the file, above `softBrush`:

```kotlin
private fun DrawScope.drawWipe(stroke: List<Offset>, radius: Float, brush: Brush) {
    val pixels = stroke.map { Offset(it.x * size.width, it.y * size.height) }
    for (dab in wipeDabs(pixels, radius * FrostDimens.DabSpacingRatio)) {
        translate(dab.x, dab.y) {
            drawCircle(brush, radius, Offset.Zero, blendMode = BlendMode.DstOut)
        }
    }
}

// The frost drawn again, kept only where the fog mask is: the mask goes into a layer, then the
// frost is drawn into a nested layer that SrcIn composites onto the mask alone.
private fun ContentDrawScope.drawFog(level: Float) {
    if (level <= 0f) return
    val bounds = Rect(Offset.Zero, size)
    drawIntoCanvas { canvas ->
        canvas.saveLayer(bounds, Paint())
        drawFogMask(level)
        canvas.saveLayer(bounds, Paint().apply { blendMode = BlendMode.SrcIn })
        drawContent()
        canvas.restore()
        canvas.restore()
    }
}
```

Add the imports:

```kotlin
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
```

Update the KDoc of `FoggedMirror` by appending this paragraph:

```kotlin
 *
 * Breaths fog the glass back over: each draws the frost again through a mask rising from the bottom.
 * Wipes and breaths replay in the order they were made, so a later wipe clears fresh fog too.
```

- [ ] **Step 7: Run the tests to verify they pass**

Run: `./gradlew :composeApp:iosSimulatorArm64Test`
Expected: BUILD SUCCESSFUL, including `FogMaskUiTest` (4 tests) and `FrostFogUiTest`.

- [ ] **Step 8: Commit**

```bash
./gradlew spotlessApply
git add composeApp/src/commonMain/kotlin/dev/dimvlachos/lab/core/presentation/components/frost composeApp/src/commonTest/kotlin/dev/dimvlachos/lab/core/presentation/components/frost
git commit -m "feat: draw breaths as fog rising over the frost, in order with the wipes" -m "Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
Claude-Session: https://claude.ai/code/session_01TkrycYKRAwVDjLBuvHiu6E"
```

---

### Task 3: Hold to breathe, drag to wipe

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/dev/dimvlachos/lab/core/presentation/components/frost/FoggedMirror.kt`
- Test: `composeApp/src/commonTest/kotlin/dev/dimvlachos/lab/core/presentation/components/frost/FoggedMirrorUiTest.kt`

**Interfaces:**
- Consumes: `FrostDimens.HoldDelayMillis` (Task 2).
- Produces: `FoggedMirror(photo, state, modifier, brushRadius, onHoldChange: ((Boolean) -> Unit)? = null)`.

- [ ] **Step 1: Write the failing tests**

Add to `FoggedMirrorUiTest` (keep the existing test). Add imports `androidx.compose.runtime.getValue`, `androidx.compose.runtime.mutableStateOf`, `androidx.compose.runtime.setValue`, `androidx.compose.ui.test.ComposeUiTest`.

```kotlin
    private fun ComposeUiTest.showWindow(
        frost: FrostState,
        holds: MutableList<Boolean>,
        shown: () -> Boolean = { true },
    ) {
        setContent {
            LabTheme {
                if (shown()) {
                    FoggedMirror(
                        photo = painterResource(Res.drawable.photo_santorini),
                        state = frost,
                        modifier = Modifier.size(200.dp).testTag("window"),
                        onHoldChange = { holds += it },
                    )
                }
            }
        }
    }

    @Test
    fun aStillHoldBreathesInsteadOfWiping() = runComposeUiTest {
        val frost = FrostState()
        val holds = mutableListOf<Boolean>()
        showWindow(frost, holds)

        onNodeWithTag("window").performTouchInput { down(center) }
        mainClock.advanceTimeBy(600)
        onNodeWithTag("window").performTouchInput { up() }
        waitForIdle()

        assertEquals(listOf(true, false), holds)
        assertTrue(frost.strokes.isEmpty())
    }

    @Test
    fun aQuickTapNeitherWipesNorBreathes() = runComposeUiTest {
        val frost = FrostState()
        val holds = mutableListOf<Boolean>()
        showWindow(frost, holds)

        onNodeWithTag("window").performTouchInput { down(center) }
        mainClock.advanceTimeBy(100)
        onNodeWithTag("window").performTouchInput { up() }
        waitForIdle()

        assertTrue(holds.isEmpty())
        assertTrue(frost.strokes.isEmpty())
    }

    @Test
    fun aDragStillWipesWhenHoldingIsOn() = runComposeUiTest {
        val frost = FrostState()
        val holds = mutableListOf<Boolean>()
        showWindow(frost, holds)

        onNodeWithTag("window").performTouchInput { swipe(centerLeft, centerRight) }

        assertEquals(1, frost.strokes.size)
        assertTrue(holds.isEmpty())
    }

    @Test
    fun leavingMidHoldLetsGo() = runComposeUiTest {
        val frost = FrostState()
        val holds = mutableListOf<Boolean>()
        var shown by mutableStateOf(true)
        showWindow(frost, holds, shown = { shown })

        onNodeWithTag("window").performTouchInput { down(center) }
        mainClock.advanceTimeBy(600)
        shown = false
        waitForIdle()

        assertEquals(listOf(true, false), holds)
    }
```

- [ ] **Step 2: Run the tests to verify they fail**

Run: `./gradlew :composeApp:iosSimulatorArm64Test --tests '*FoggedMirrorUiTest*'`
Expected: compilation fails with `No parameter with name 'onHoldChange' found`.

- [ ] **Step 3: Implement the gesture**

In `FoggedMirror.kt`:

1. Add the parameter after `brushRadius`, and document it in the KDoc:

```kotlin
    onHoldChange: ((Boolean) -> Unit)? = null,
```

```kotlin
 *
 * With [onHoldChange] set, a press that stays still for [FrostDimens.HoldDelayMillis] reports `true`
 * instead of wiping, and `false` when it lifts or the window goes away.
```

2. Replace the `modifier.pointerInput(state) { ... }` argument of the outer `Box` with:

```kotlin
        modifier.pointerInput(state, onHoldChange != null) {
            val holdEnabled = onHoldChange != null
            awaitEachGesture {
                val down = awaitFirstDown()
                val start =
                    if (holdEnabled) {
                        withTimeoutOrNull(FrostDimens.HoldDelayMillis) { awaitDragOrLift(down) }
                            ?: PressStart.Held
                    } else {
                        awaitDragOrLift(down)
                    }
                when (start) {
                    PressStart.Held -> {
                        holdChange?.invoke(true)
                        try {
                            waitForUpOrCancellation()
                        } finally {
                            holdChange?.invoke(false)
                        }
                    }
                    PressStart.Lifted -> Unit
                    is PressStart.Dragged -> {
                        val stroke = state.beginStroke(down.position.fractionOf(size))
                        state.extendStroke(stroke, start.change.position.fractionOf(size))
                        drag(start.change.id) { change ->
                            state.extendStroke(stroke, change.position.fractionOf(size))
                            change.consume()
                        }
                    }
                }
            }
        }
```

3. At the top of the function body, before `val noise`, add (a caller's lambda changes on every recomposition; the gesture reads the latest without restarting):

```kotlin
    val holdChange by rememberUpdatedState(onHoldChange)
```

4. At the bottom of the file add:

```kotlin
private sealed interface PressStart {
    data object Held : PressStart

    data object Lifted : PressStart

    class Dragged(val change: PointerInputChange) : PressStart
}

// Waits for the press to move past touch slop, a drag, or to lift first.
private suspend fun AwaitPointerEventScope.awaitDragOrLift(down: PointerInputChange): PressStart =
    awaitTouchSlopOrCancellation(down.id) { change, _ -> change.consume() }
        ?.let { PressStart.Dragged(it) } ?: PressStart.Lifted
```

5. Imports: remove `androidx.compose.foundation.gestures.detectDragGestures`; add

```kotlin
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitTouchSlopOrCancellation
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerInputChange
```

- [ ] **Step 4: Run the tests to verify they pass**

Run: `./gradlew :composeApp:iosSimulatorArm64Test`
Expected: BUILD SUCCESSFUL; `FoggedMirrorUiTest` has 5 passing tests, and the older `aDragWipesAStrokeInFractionsOfTheWindow` still passes (a stroke now begins at the finger's first touch, which is still within 0..1).

- [ ] **Step 5: Commit**

```bash
./gradlew spotlessApply
git add composeApp/src/commonMain/kotlin/dev/dimvlachos/lab/core/presentation/components/frost/FoggedMirror.kt composeApp/src/commonTest/kotlin/dev/dimvlachos/lab/core/presentation/components/frost/FoggedMirrorUiTest.kt
git commit -m "feat: a still press on the fogged mirror can breathe instead of wiping" -m "Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
Claude-Session: https://claude.ai/code/session_01TkrycYKRAwVDjLBuvHiu6E"
```

---

### Task 4: The blow detector

**Files:**
- Create: `composeApp/src/commonMain/kotlin/dev/dimvlachos/lab/core/audio/AudioFormat.kt`
- Create: `composeApp/src/commonMain/kotlin/dev/dimvlachos/lab/core/audio/Fft.kt`
- Create: `composeApp/src/commonMain/kotlin/dev/dimvlachos/lab/core/audio/BlowDetector.kt`
- Create: `composeApp/src/commonTest/kotlin/dev/dimvlachos/lab/core/audio/TestSignals.kt`
- Test: `composeApp/src/commonTest/kotlin/dev/dimvlachos/lab/core/audio/FftTest.kt`
- Test: `composeApp/src/commonTest/kotlin/dev/dimvlachos/lab/core/audio/BlowDetectorTest.kt`

**Interfaces:**
- Consumes: nothing.
- Produces: `const val MicSampleRate = 16_000`; `const val MicFrameSize = 512`; `const val MicFrameSeconds: Float = 0.032f`; `internal fun powerSpectrum(samples: FloatArray): FloatArray`; `internal fun spectralFlatness(power: FloatArray, sampleRate: Int, fromHz: Float, toHz: Float): Float`; `class BlowDetector { fun process(frame: FloatArray): Float }`. Test helpers (commonTest, internal): `noise(amplitude: Float, random: Random): FloatArray`, `tone(amplitude: Float, frameIndex: Int, vararg hz: Float): FloatArray`, `silence(): FloatArray`.

- [ ] **Step 1: Create the audio format and test signals**

`AudioFormat.kt`:

```kotlin
package dev.dimvlachos.lab.core.audio

/** Microphone audio as the blow detector hears it: mono, at this many samples a second. */
const val MicSampleRate = 16_000

/** Samples in each frame the detector takes, about 32 ms. */
const val MicFrameSize = 512

/** How long one frame lasts. */
const val MicFrameSeconds: Float = MicFrameSize / MicSampleRate.toFloat()
```

`TestSignals.kt` (commonTest):

```kotlin
package dev.dimvlachos.lab.core.audio

import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

/** One frame of white noise, uniform in -[amplitude]..[amplitude]. */
internal fun noise(amplitude: Float, random: Random) =
    FloatArray(MicFrameSize) { (random.nextFloat() * 2f - 1f) * amplitude }

/** One frame, number [frameIndex] of a steady tone made of [hz], peaking near [amplitude]. */
internal fun tone(amplitude: Float, frameIndex: Int, vararg hz: Float) =
    FloatArray(MicFrameSize) { i ->
        val seconds = (frameIndex * MicFrameSize + i) / MicSampleRate.toDouble()
        (hz.sumOf { sin(2 * PI * it * seconds) } * amplitude / hz.size).toFloat()
    }

internal fun silence() = FloatArray(MicFrameSize)
```

- [ ] **Step 2: Write the failing FFT tests**

`FftTest.kt`:

```kotlin
package dev.dimvlachos.lab.core.audio

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FftTest {
    @Test
    fun aToneLandsInItsOwnBin() {
        // 16 kHz over 512 samples is 31.25 Hz a bin, so 1 kHz is bin 32.
        val power = powerSpectrum(tone(0.5f, 0, 1_000f))
        assertEquals(257, power.size)
        assertEquals(32, power.indices.maxBy { power[it] })
    }

    @Test
    fun noiseIsFlatAndAToneIsNot() {
        val noisy = spectralFlatness(powerSpectrum(noise(0.3f, Random(1))), MicSampleRate, 100f, 4_000f)
        val tonal = spectralFlatness(powerSpectrum(tone(0.3f, 0, 220f, 440f, 660f)), MicSampleRate, 100f, 4_000f)
        assertTrue(noisy > 0.4f, "noise flatness $noisy")
        assertTrue(tonal < 0.1f, "tone flatness $tonal")
    }
}
```

- [ ] **Step 3: Write the failing detector tests**

`BlowDetectorTest.kt`:

```kotlin
package dev.dimvlachos.lab.core.audio

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BlowDetectorTest {
    private val random = Random(7)

    // A quiet room: noise around -55 dBFS.
    private fun room() = noise(0.003f, random)

    // A blow: loud noise around -15 dBFS.
    private fun blow(amplitude: Float = 0.3f) = noise(amplitude, random)

    private fun BlowDetector.settleInAQuietRoom() = repeat(20) { process(room()) }

    @Test
    fun aBlowCountsAfterAbout100ms() {
        val detector = BlowDetector()
        detector.settleInAQuietRoom()

        val strengths = List(5) { detector.process(blow()) }

        assertEquals(0f, strengths[0])
        assertEquals(0f, strengths[1])
        assertTrue(strengths.drop(2).all { it > 0f }, "$strengths")
    }

    @Test
    fun aVoiceLikeToneIsNotABlow() {
        val detector = BlowDetector()
        detector.settleInAQuietRoom()

        val strengths = List(30) { detector.process(tone(0.3f, it, 180f, 360f, 540f, 720f)) }

        assertTrue(strengths.all { it == 0f }, "$strengths")
    }

    @Test
    fun silenceIsNotABlow() {
        val detector = BlowDetector()
        assertTrue(List(40) { detector.process(silence()) }.all { it == 0f })
    }

    @Test
    fun aRoomThatIsNoisyFromTheStartIsNotABlow() {
        val detector = BlowDetector()
        assertTrue(List(60) { detector.process(blow()) }.all { it == 0f })
    }

    @Test
    fun theBlowEndsAfterAbout130msOfQuiet() {
        val detector = BlowDetector()
        detector.settleInAQuietRoom()
        repeat(10) { detector.process(blow()) }

        val strengths = List(4) { detector.process(room()) }

        assertTrue(strengths.take(3).all { it > 0f }, "$strengths")
        assertEquals(0f, strengths[3])
    }

    @Test
    fun aHarderBlowIsStronger() {
        val soft = BlowDetector().apply { settleInAQuietRoom() }
        val hard = BlowDetector().apply { settleInAQuietRoom() }
        repeat(4) {
            soft.process(blow(0.05f))
            hard.process(blow(0.5f))
        }

        assertTrue(soft.process(blow(0.05f)) < hard.process(blow(0.5f)))
    }

    @Test
    fun aFanSwitchedOnStopsCountingAsABlowAfterAbout4s() {
        val detector = BlowDetector()
        detector.settleInAQuietRoom()

        val strengths = List(200) { detector.process(blow()) }

        assertTrue(strengths[10] > 0f, "it starts out as a blow")
        assertTrue(strengths.takeLast(50).all { it == 0f }, "then it is the room")
    }
}
```

- [ ] **Step 4: Run the tests to verify they fail**

Run: `./gradlew :composeApp:iosSimulatorArm64Test --tests '*core.audio*'`
Expected: compilation fails with `Unresolved reference 'powerSpectrum'` and `'BlowDetector'`.

- [ ] **Step 5: Implement the FFT**

`Fft.kt`:

```kotlin
package dev.dimvlachos.lab.core.audio

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.sin

/**
 * The power in each frequency bin, from 0 to half the sample rate, of [samples] under a Hann window.
 * [samples] must be a power of two long; the result has one more than half as many bins.
 */
internal fun powerSpectrum(samples: FloatArray): FloatArray {
    val n = samples.size
    require(n > 1 && (n and (n - 1)) == 0) { "the frame must be a power of two long: $n" }
    val re = DoubleArray(n) { samples[it] * (0.5 - 0.5 * cos(2 * PI * it / (n - 1))) }
    val im = DoubleArray(n)
    // Bit-reversed order, then butterflies of doubling length.
    var j = 0
    for (i in 1 until n) {
        var bit = n shr 1
        while ((j and bit) != 0) {
            j = j xor bit
            bit = bit shr 1
        }
        j = j xor bit
        if (i < j) {
            val swap = re[i]
            re[i] = re[j]
            re[j] = swap
        }
    }
    var length = 2
    while (length <= n) {
        val angle = -2 * PI / length
        for (start in 0 until n step length) {
            for (k in 0 until length / 2) {
                val wr = cos(angle * k)
                val wi = sin(angle * k)
                val a = start + k
                val b = a + length / 2
                val xr = re[b] * wr - im[b] * wi
                val xi = re[b] * wi + im[b] * wr
                re[b] = re[a] - xr
                im[b] = im[a] - xi
                re[a] += xr
                im[a] += xi
            }
        }
        length = length shl 1
    }
    return FloatArray(n / 2 + 1) { (re[it] * re[it] + im[it] * im[it]).toFloat() }
}

/**
 * How noise-like the sound is between [fromHz] and [toHz]: the geometric over the arithmetic mean of
 * the [power]. Near 1 for hiss, whose power is spread evenly; near 0 for a tone or a voice, whose
 * power sits in a few bins.
 */
internal fun spectralFlatness(power: FloatArray, sampleRate: Int, fromHz: Float, toHz: Float): Float {
    val binHz = sampleRate / 2f / (power.size - 1)
    val from = (fromHz / binHz).toInt().coerceAtLeast(1)
    val to = (toHz / binHz).toInt().coerceAtMost(power.size - 1)
    var logSum = 0.0
    var sum = 0.0
    for (i in from..to) {
        val p = power[i] + 1e-12
        logSum += ln(p)
        sum += p
    }
    val count = to - from + 1
    return (exp(logSum / count) / (sum / count)).toFloat()
}
```

- [ ] **Step 6: Implement the detector**

`BlowDetector.kt`:

```kotlin
package dev.dimvlachos.lab.core.audio

import kotlin.math.log10
import kotlin.math.sqrt

// How far above the room a frame must be to count, in dB, and how much further a firm blow goes.
private const val MarginDb = 15f
private const val StrengthRangeDb = 25f

// Below this a frame is too quiet to be a blow, however quiet the room.
private const val MinLevelDb = -50f

// Breath is hiss; a voice or music is tones.
private const val MinFlatness = 0.35f

// About 100 ms to start and 130 ms to stop, so a breath does not flicker; past about 4 s, a steady
// "blow" is the room, a fan or a vacuum, and becomes the new background.
private const val OnsetFrames = 3
private const val ReleaseFrames = 4
private const val MaxBlowFrames = 125

// How quickly the background follows the room, per frame.
private const val BackgroundFollow = 0.05f

// A blow too faint to see is still a blow: the fog always moves.
private const val MinStrength = 0.3f

/**
 * Hears a blow on the microphone: sound well above the room's own level, noise-like rather than
 * tonal, for long enough to be a breath. Feed it [MicFrameSize]-sample frames in order; each returns
 * the blow's strength, from 0, no blow, to 1, a firm one.
 */
class BlowDetector(private val sampleRate: Int = MicSampleRate) {
    private var background = Float.NaN
    private var candidateFrames = 0
    private var blowFrames = 0
    private var quietFrames = 0
    private var strength = 0f

    fun process(frame: FloatArray): Float {
        val level = loudnessDb(frame)
        if (background.isNaN()) background = level
        val margin = level - background
        val blowing =
            level >= MinLevelDb &&
                margin >= MarginDb &&
                spectralFlatness(powerSpectrum(frame), sampleRate, 100f, 4_000f) >= MinFlatness
        if (strength > 0f) {
            if (blowing) {
                quietFrames = 0
                blowFrames++
                strength = strengthFor(margin)
                if (blowFrames > MaxBlowFrames) {
                    background = level
                    stop()
                }
            } else if (++quietFrames >= ReleaseFrames) {
                stop()
            }
        } else if (blowing) {
            if (++candidateFrames >= OnsetFrames) {
                blowFrames = candidateFrames
                strength = strengthFor(margin)
            }
        } else {
            candidateFrames = 0
            background += (level - background) * BackgroundFollow
        }
        return strength
    }

    private fun stop() {
        strength = 0f
        candidateFrames = 0
        blowFrames = 0
        quietFrames = 0
    }

    private fun strengthFor(margin: Float) =
        ((margin - MarginDb) / StrengthRangeDb).coerceIn(MinStrength, 1f)
}

private fun loudnessDb(frame: FloatArray): Float {
    var sum = 0.0
    for (sample in frame) sum += sample * sample
    val rms = sqrt(sum / frame.size)
    return (20 * log10(rms.coerceAtLeast(1e-9))).toFloat().coerceAtLeast(-90f)
}
```

- [ ] **Step 7: Run the tests to verify they pass**

Run: `./gradlew :composeApp:iosSimulatorArm64Test --tests '*core.audio*'`
Expected: BUILD SUCCESSFUL, 9 tests. If `aHarderBlowIsStronger` fails with equal strengths, both saturated: lower the soft blow's amplitude to 0.03 in the test, since the rule under test is the ordering.

- [ ] **Step 8: Commit**

```bash
./gradlew spotlessApply
git add composeApp/src/commonMain/kotlin/dev/dimvlachos/lab/core/audio composeApp/src/commonTest/kotlin/dev/dimvlachos/lab/core/audio
git commit -m "feat: hear a blow in microphone audio as loud, noise-like sound" -m "Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
Claude-Session: https://claude.ai/code/session_01TkrycYKRAwVDjLBuvHiu6E"
```

---

### Task 5: The breath driver

**Files:**
- Create: `composeApp/src/commonMain/kotlin/dev/dimvlachos/lab/frostdemo/BreathDriver.kt`
- Test: `composeApp/src/commonTest/kotlin/dev/dimvlachos/lab/frostdemo/BreathDriverTest.kt`

**Interfaces:**
- Consumes: Task 1's `FrostState.marks`, `beginBreath()`, `setBreathLevel()`, `Breath.level`.
- Produces: `internal class BreathDriver(frost: FrostState, secondsToCover: Float = 1.2f) { fun advance(strength: Float, seconds: Float) }`; `internal fun scriptedBreathStrength(time: Float, peak: Float): Float`.

- [ ] **Step 1: Write the failing tests**

`BreathDriverTest.kt`:

```kotlin
package dev.dimvlachos.lab.frostdemo

import androidx.compose.ui.geometry.Offset
import dev.dimvlachos.lab.core.presentation.components.frost.Breath
import dev.dimvlachos.lab.core.presentation.components.frost.FrostState
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotSame
import kotlin.test.assertSame
import kotlin.test.assertTrue

class BreathDriverTest {
    private fun assertNear(expected: Float, actual: Float) =
        assertTrue(abs(expected - actual) < 0.001f, "expected $expected, got $actual")

    @Test
    fun theFogRisesWithTheBlowsStrength() {
        val frost = FrostState()
        BreathDriver(frost, secondsToCover = 1.2f).advance(strength = 0.5f, seconds = 0.6f)

        assertNear(0.25f, (frost.marks.single() as Breath).level)
    }

    @Test
    fun noBlowLeavesTheGlassAlone() {
        val frost = FrostState()
        BreathDriver(frost).advance(strength = 0f, seconds = 1f)

        assertTrue(frost.marks.isEmpty())
    }

    @Test
    fun blowingAgainContinuesTheSameFog() {
        val frost = FrostState()
        val driver = BreathDriver(frost, secondsToCover = 1.2f)
        driver.advance(1f, 0.3f)
        driver.advance(0f, 1f)
        driver.advance(1f, 0.3f)

        assertNear(0.5f, (frost.marks.single() as Breath).level)
    }

    @Test
    fun aWipeMidBlowStartsNewFogFromTheBottomAndTheOldFogStays() {
        val frost = FrostState()
        val driver = BreathDriver(frost, secondsToCover = 1.2f)
        driver.advance(1f, 0.6f)
        val first = frost.marks.single() as Breath
        val wipe = frost.beginStroke(Offset(0.5f, 0.8f))
        driver.advance(1f, 0.12f)

        assertEquals(3, frost.marks.size)
        assertSame(wipe, frost.marks[1])
        val second = frost.marks[2] as Breath
        assertNotSame(first, second)
        assertNear(0.5f, first.level)
        assertNear(0.1f, second.level)
    }

    @Test
    fun aLongEnoughBlowFrostsTheGlassOver() {
        val frost = FrostState()
        frost.beginStroke(Offset(0.5f, 0.5f))
        BreathDriver(frost, secondsToCover = 1.2f).advance(1f, 1.3f)

        assertTrue(frost.marks.isEmpty())
    }

    @Test
    fun aScriptedBreathSwellsAndFades() {
        assertEquals(0f, scriptedBreathStrength(0f, peak = 1f))
        assertNear(0.8f, scriptedBreathStrength(0.5f, peak = 0.8f))
        assertTrue(scriptedBreathStrength(1f, peak = 1f) < 0.001f)
    }
}
```

- [ ] **Step 2: Run the tests to verify they fail**

Run: `./gradlew :composeApp:iosSimulatorArm64Test --tests '*BreathDriverTest*'`
Expected: compilation fails with `Unresolved reference 'BreathDriver'`.

- [ ] **Step 3: Implement**

`BreathDriver.kt`:

```kotlin
package dev.dimvlachos.lab.frostdemo

import dev.dimvlachos.lab.core.presentation.components.frost.Breath
import dev.dimvlachos.lab.core.presentation.components.frost.FrostState
import kotlin.math.PI
import kotlin.math.sin

/**
 * Turns a blow into fog on [frost]: each moment of blowing raises the fog in step with the blow's
 * strength, so a steady full-strength blow covers the glass in [secondsToCover]. Blowing again
 * carries on the same fog, unless something has been wiped since: then new fog rises from the bottom
 * and covers that too. The microphone, a held finger and the script all breathe through this.
 */
internal class BreathDriver(
    private val frost: FrostState,
    private val secondsToCover: Float = 1.2f,
) {
    private var breath: Breath? = null

    fun advance(strength: Float, seconds: Float) {
        if (strength <= 0f || seconds <= 0f) return
        val current =
            breath?.takeIf { frost.marks.lastOrNull() === it }
                ?: frost.beginBreath().also { breath = it }
        frost.setBreathLevel(current, current.level + strength * seconds / secondsToCover)
    }
}

/**
 * The script's breath at [time], from 0 to 1 through it: it swells to [peak] halfway and fades, as a
 * breath out does, rather than switching on and off.
 */
internal fun scriptedBreathStrength(time: Float, peak: Float): Float =
    peak * sin(PI * time.coerceIn(0f, 1f)).toFloat()
```

- [ ] **Step 4: Run the tests to verify they pass**

Run: `./gradlew :composeApp:iosSimulatorArm64Test --tests '*BreathDriverTest*'`
Expected: BUILD SUCCESSFUL, 6 tests.

- [ ] **Step 5: Commit**

```bash
./gradlew spotlessApply
git add composeApp/src/commonMain/kotlin/dev/dimvlachos/lab/frostdemo/BreathDriver.kt composeApp/src/commonTest/kotlin/dev/dimvlachos/lab/frostdemo/BreathDriverTest.kt
git commit -m "feat: turn a blow's strength over time into rising fog" -m "Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
Claude-Session: https://claude.ai/code/session_01TkrycYKRAwVDjLBuvHiu6E"
```

---

### Task 6: The script can breathe, and knows when it is recording

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/dev/dimvlachos/lab/core/demo/DemoController.kt`
- Modify: `composeApp/src/commonMain/kotlin/dev/dimvlachos/lab/core/demo/DemoState.kt`
- Modify: `composeApp/src/commonMain/kotlin/dev/dimvlachos/lab/demo/presentation/screen/DemoScreen.kt:52`
- Modify: `composeApp/src/commonTest/kotlin/dev/dimvlachos/lab/core/demo/FakeController.kt`
- Test: `composeApp/src/commonTest/kotlin/dev/dimvlachos/lab/core/demo/DemoStateTest.kt`

**Interfaces:**
- Consumes: nothing new.
- Produces: `DemoController.breathe(duration: Duration, strength: Float)`; `DemoState(recording: Boolean = false)` with `val recording: Boolean`; `DemoState.setBreatheHandler(handler: (suspend (Duration, Float) -> Unit)?)`; `FakeController.breaths: MutableList<Pair<Duration, Float>>`.

- [ ] **Step 1: Write the failing tests**

Append to `DemoStateTest`:

```kotlin
    @Test
    fun aBreathGoesToTheDemosHandler() = runTest {
        val state = DemoState()
        val breaths = mutableListOf<Pair<Duration, Float>>()
        state.setBreatheHandler { duration, strength -> breaths += duration to strength }

        state.breathe(2.seconds, 0.8f)

        assertEquals(listOf(2.seconds to 0.8f), breaths)
    }

    @Test
    fun aBreathWithNoHandlerIsIgnored() = runTest {
        DemoState().breathe(1.seconds, 1f)
    }

    @Test
    fun theStateKnowsWhetherItIsBeingRecorded() {
        assertEquals(false, DemoState().recording)
        assertEquals(true, DemoState(recording = true).recording)
    }
```

- [ ] **Step 2: Run the tests to verify they fail**

Run: `./gradlew :composeApp:iosSimulatorArm64Test --tests '*DemoStateTest*'`
Expected: compilation fails with `Unresolved reference 'setBreatheHandler'`.

- [ ] **Step 3: Implement**

In `DemoController.kt`, add to the interface:

```kotlin
    /** Breathes on the stage for [duration], swelling to [strength], from 0 to 1, and fading. */
    suspend fun breathe(duration: Duration, strength: Float)
```

Replace `DemoState.kt` with:

```kotlin
package dev.dimvlachos.lab.core.demo

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import kotlin.time.Duration

/** A demo's scripted state; [recording] when the recorder is capturing it as a clip. */
@Stable
class DemoState(val recording: Boolean = false) : DemoController {
    override var selectedIndex: Int by mutableIntStateOf(0)
        private set

    private var scrollHandler: (suspend (Float) -> Unit)? = null
    private var wipeHandler: (suspend (List<Offset>, Duration) -> Unit)? = null
    private var breatheHandler: (suspend (Duration, Float) -> Unit)? = null

    override fun select(index: Int) {
        selectedIndex = index
    }

    override suspend fun scrollBy(px: Float) {
        scrollHandler?.invoke(px)
    }

    override suspend fun wipe(path: List<Offset>, duration: Duration) {
        wipeHandler?.invoke(path, duration)
    }

    override suspend fun breathe(duration: Duration, strength: Float) {
        breatheHandler?.invoke(duration, strength)
    }

    fun setScrollHandler(handler: (suspend (Float) -> Unit)?) {
        scrollHandler = handler
    }

    fun setWipeHandler(handler: (suspend (List<Offset>, Duration) -> Unit)?) {
        wipeHandler = handler
    }

    fun setBreatheHandler(handler: (suspend (Duration, Float) -> Unit)?) {
        breatheHandler = handler
    }
}
```

In `DemoScreen.kt` line 52, change:

```kotlin
    val state = remember(runId) { DemoState() }
```

to:

```kotlin
    val state = remember(runId) { DemoState(recording = record) }
```

In `FakeController.kt`, add the field after `wipes` and the override after `wipe`:

```kotlin
    val breaths = mutableListOf<Pair<Duration, Float>>()
```

```kotlin
    override suspend fun breathe(duration: Duration, strength: Float) {
        breaths += duration to strength
        calls += now() to "breathe($duration, $strength)"
    }
```

- [ ] **Step 4: Run the tests to verify they pass**

Run: `./gradlew :composeApp:iosSimulatorArm64Test`
Expected: BUILD SUCCESSFUL (the whole suite, since `DemoController` changed).

- [ ] **Step 5: Commit**

```bash
./gradlew spotlessApply
git add composeApp/src/commonMain/kotlin/dev/dimvlachos/lab/core/demo composeApp/src/commonMain/kotlin/dev/dimvlachos/lab/demo/presentation/screen/DemoScreen.kt composeApp/src/commonTest/kotlin/dev/dimvlachos/lab/core/demo
git commit -m "feat: let demo scripts breathe, and tell demos when they are recorded" -m "Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
Claude-Session: https://claude.ai/code/session_01TkrycYKRAwVDjLBuvHiu6E"
```

---

### Task 7: The clip breathes the glass over

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/dev/dimvlachos/lab/frostdemo/presentation/components/FrostDemo.kt`
- Modify: `composeApp/src/commonMain/kotlin/dev/dimvlachos/lab/frostdemo/FrostDemos.kt`
- Test: `composeApp/src/commonTest/kotlin/dev/dimvlachos/lab/frostdemo/FrostDemosTest.kt`
- Test: `composeApp/src/commonTest/kotlin/dev/dimvlachos/lab/frostdemo/presentation/components/FrostDemoUiTest.kt`

**Interfaces:**
- Consumes: `BreathDriver`, `scriptedBreathStrength` (Task 5); `DemoState.setBreatheHandler`, `FakeController.breaths` (Task 6).
- Produces: `FrostDemo` plays scripted breaths; the clip script breathes at 7.4 s.

The spec places the "breath covers the glass" check in `CatalogTest`; it goes in a new `FrostDemosTest` instead, because it needs `frostdemo` internals that the generic catalog rules should not know about.

- [ ] **Step 1: Write the failing tests**

`FrostDemosTest.kt`:

```kotlin
@file:OptIn(ExperimentalCoroutinesApi::class)

package dev.dimvlachos.lab.frostdemo

import androidx.compose.ui.geometry.Offset
import dev.dimvlachos.lab.core.demo.FakeController
import dev.dimvlachos.lab.core.presentation.components.frost.FrostState
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest

class FrostDemosTest {
    @Test
    fun theClipsBreathFrostsTheWholeGlassSoTheLoopNeedsNoReset() = runTest {
        val controller = FakeController { testScheduler.currentTime }
        FrostDemos.all.single().script.play(controller)
        val (duration, peak) = controller.breaths.single()

        val frost = FrostState()
        frost.beginStroke(Offset(0.5f, 0.5f))
        val driver = BreathDriver(frost)
        val steps = (duration.inWholeMilliseconds / 16).toInt()
        for (i in 1..steps) driver.advance(scriptedBreathStrength(i / steps.toFloat(), peak), 0.016f)

        assertTrue(frost.marks.isEmpty(), "fog left the glass partly clear: ${frost.marks}")
    }
}
```

Add to `FrostDemoUiTest` (after the existing test):

```kotlin
    @Test
    fun aScriptedBreathFogsTheGlassOver() = runComposeUiTest {
        mainClock.autoAdvance = false
        val state = DemoState()
        val frost = FrostState()
        frost.beginStroke(Offset(0.5f, 0.5f))
        var scope: CoroutineScope? = null
        setContent {
            scope = rememberCoroutineScope()
            LabTheme { Box(Modifier.size(400.dp, 500.dp)) { FrostDemo(state, frost) } }
        }
        mainClock.advanceTimeByFrame()
        runOnUiThread { scope!!.launch { state.breathe(2_400.milliseconds, 1f) } }
        mainClock.advanceTimeBy(2_600)

        assertTrue(frost.marks.isEmpty(), "${frost.marks}")
    }
```

- [ ] **Step 2: Run the tests to verify they fail**

Run: `./gradlew :composeApp:iosSimulatorArm64Test --tests '*frostdemo*'`
Expected: FAIL: `FrostDemosTest` with `NoSuchElementException` (no breath in the script, from `single()`), `aScriptedBreathFogsTheGlassOver` with the wipe still in `marks`.

- [ ] **Step 3: Play scripted breaths in `FrostDemo`**

In `FrostDemo.kt`:

1. After `var window by remember { mutableStateOf(Size.Zero) }` add:

```kotlin
    val driver = remember(frost) { BreathDriver(frost) }
```

2. Change `DisposableEffect(state, frost) {` to `DisposableEffect(state, frost, driver) {` and, after the `state.setWipeHandler { ... }` call, add:

```kotlin
        // The script's breath swells and fades through the same driver as a real one.
        state.setBreatheHandler { duration, strength ->
            val seconds = duration.inWholeMilliseconds / 1000f
            var previous = 0f
            animate(
                0f,
                1f,
                animationSpec = tween(duration.inWholeMilliseconds.toInt(), easing = LinearEasing),
            ) { t, _ ->
                driver.advance(scriptedBreathStrength(t, strength), (t - previous) * seconds)
                previous = t
            }
        }
```

3. Replace `onDispose { state.setWipeHandler(null) }` with:

```kotlin
        onDispose {
            state.setWipeHandler(null)
            state.setBreatheHandler(null)
        }
```

4. Add imports `dev.dimvlachos.lab.frostdemo.BreathDriver` and `dev.dimvlachos.lab.frostdemo.scriptedBreathStrength`.

- [ ] **Step 4: Breathe in the clip**

In `FrostDemos.kt`, replace `wipeTour` with:

```kotlin
    // The scrub, a moment to look through the oval, then a breath fogs the glass over from the
    // bottom: by the loop's reset the glass is already fresh frost, so the reset shows nothing.
    private val wipeTour = demoScript {
        at(0.seconds) { select(1) }
        at(0.4.seconds) { wipe(porthole.path(), porthole.duration) }
        at(7.4.seconds) { breathe(2.4.seconds, strength = 1f) }
        at(10.2.seconds) { select(0) }
    }
```

- [ ] **Step 5: Run the tests to verify they pass**

Run: `./gradlew :composeApp:iosSimulatorArm64Test`
Expected: BUILD SUCCESSFUL, including `CatalogTest` (the clip is about 10.8 s with its holds, under 40 s, and ends on `select(0)`).

- [ ] **Step 6: Commit**

```bash
./gradlew spotlessApply
git add composeApp/src/commonMain/kotlin/dev/dimvlachos/lab/frostdemo composeApp/src/commonTest/kotlin/dev/dimvlachos/lab/frostdemo
git commit -m "feat: end the frost clip with a breath that fogs the glass over" -m "Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
Claude-Session: https://claude.ai/code/session_01TkrycYKRAwVDjLBuvHiu6E"
```

---

### Task 8: The microphone: Android recorder, permission, iOS stub

**Files:**
- Create: `composeApp/src/commonMain/kotlin/dev/dimvlachos/lab/core/audio/Microphone.kt`
- Create: `composeApp/src/androidMain/kotlin/dev/dimvlachos/lab/core/audio/Microphone.android.kt`
- Create: `composeApp/src/iosMain/kotlin/dev/dimvlachos/lab/core/audio/Microphone.ios.kt`
- Modify: `composeApp/build.gradle.kts` (sourceSets)
- Modify: `androidApp/src/main/AndroidManifest.xml`
- Test: `composeApp/src/iosTest/kotlin/dev/dimvlachos/lab/core/audio/MicrophoneIosTest.kt`

**Interfaces:**
- Consumes: `MicSampleRate`, `MicFrameSize` (Task 4).
- Produces: `interface Microphone { val frames: Flow<FloatArray> }`; `sealed interface MicAccess { data object Pending; data object Unavailable; class Granted(val microphone: Microphone) }`; `@Composable expect fun rememberMicAccess(enabled: Boolean): MicAccess`.

- [ ] **Step 1: Write the failing iOS test**

`MicrophoneIosTest.kt` (iosTest source set; it is compiled and run by `iosSimulatorArm64Test`):

```kotlin
package dev.dimvlachos.lab.core.audio

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runComposeUiTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class MicrophoneIosTest {
    @Test
    fun iosHasNoMicrophoneYet() = runComposeUiTest {
        var access: MicAccess? = null
        setContent { access = rememberMicAccess(enabled = true) }
        waitForIdle()
        assertEquals(MicAccess.Unavailable, access)
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `./gradlew :composeApp:iosSimulatorArm64Test --tests '*MicrophoneIosTest*'`
Expected: compilation fails with `Unresolved reference 'rememberMicAccess'`.

- [ ] **Step 3: Declare the microphone in common code**

`Microphone.kt`:

```kotlin
package dev.dimvlachos.lab.core.audio

import androidx.compose.runtime.Composable
import kotlinx.coroutines.flow.Flow

/**
 * A microphone as a cold stream of [MicFrameSize]-sample mono frames at [MicSampleRate], in -1..1:
 * collecting starts recording, cancelling stops it. The stream fails if the microphone cannot start
 * or stops working.
 */
interface Microphone {
    val frames: Flow<FloatArray>
}

/** Whether this demo may listen. */
sealed interface MicAccess {
    /** Still asking the user. */
    data object Pending : MicAccess

    /** Denied, not asked (recording), or no microphone on this platform. */
    data object Unavailable : MicAccess

    class Granted(val microphone: Microphone) : MicAccess
}

/**
 * Asks for the microphone the first time it is composed with [enabled], and returns where that
 * stands. Disabled, it never asks and returns [MicAccess.Unavailable].
 */
@Composable expect fun rememberMicAccess(enabled: Boolean): MicAccess
```

- [ ] **Step 4: The iOS stub**

`Microphone.ios.kt`:

```kotlin
package dev.dimvlachos.lab.core.audio

import androidx.compose.runtime.Composable

// No microphone on iOS yet: the demo falls back to hold-to-breathe.
@Composable actual fun rememberMicAccess(enabled: Boolean): MicAccess = MicAccess.Unavailable
```

- [ ] **Step 5: The Android recorder and permission**

`Microphone.android.kt`:

```kotlin
package dev.dimvlachos.lab.core.audio

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive

@Composable
actual fun rememberMicAccess(enabled: Boolean): MicAccess {
    if (!enabled) return MicAccess.Unavailable
    val context = LocalContext.current
    var granted by remember {
        mutableStateOf(
            context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var answered by rememberSaveable { mutableStateOf(false) }
    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
            granted = it
            answered = true
        }
    LaunchedEffect(Unit) { if (!granted && !answered) launcher.launch(Manifest.permission.RECORD_AUDIO) }
    val unprocessed = remember {
        context.getSystemService(AudioManager::class.java)
            .getProperty(AudioManager.PROPERTY_SUPPORT_AUDIO_SOURCE_UNPROCESSED) == "true"
    }
    val microphone = remember(unprocessed) { AndroidMicrophone(unprocessed) }
    return when {
        granted -> MicAccess.Granted(microphone)
        answered -> MicAccess.Unavailable
        else -> MicAccess.Pending
    }
}

/**
 * The phone's microphone through [AudioRecord]. Raw audio where the device offers it: the voice
 * sources' noise suppression and gain control are made to remove exactly the hiss a blow makes.
 */
private class AndroidMicrophone(private val unprocessed: Boolean) : Microphone {
    // Only created once RECORD_AUDIO is granted.
    @SuppressLint("MissingPermission")
    override val frames: Flow<FloatArray> =
        flow {
                val source =
                    if (unprocessed) MediaRecorder.AudioSource.UNPROCESSED
                    else MediaRecorder.AudioSource.MIC
                val minBuffer =
                    AudioRecord.getMinBufferSize(
                        MicSampleRate,
                        AudioFormat.CHANNEL_IN_MONO,
                        AudioFormat.ENCODING_PCM_16BIT,
                    )
                val record =
                    AudioRecord(
                        source,
                        MicSampleRate,
                        AudioFormat.CHANNEL_IN_MONO,
                        AudioFormat.ENCODING_PCM_16BIT,
                        maxOf(minBuffer, MicFrameSize * 2 * 4),
                    )
                try {
                    check(record.state == AudioRecord.STATE_INITIALIZED) {
                        "the microphone could not start"
                    }
                    record.startRecording()
                    val samples = ShortArray(MicFrameSize)
                    while (currentCoroutineContext().isActive) {
                        var read = 0
                        while (read < MicFrameSize) {
                            val count = record.read(samples, read, MicFrameSize - read)
                            check(count >= 0) { "the microphone stopped: $count" }
                            read += count
                        }
                        emit(FloatArray(MicFrameSize) { samples[it] / 32_768f })
                    }
                } finally {
                    if (record.recordingState == AudioRecord.RECORDSTATE_RECORDING) record.stop()
                    record.release()
                }
            }
            .flowOn(Dispatchers.IO)
}
```

- [ ] **Step 6: Dependencies and the manifest**

In `composeApp/build.gradle.kts`, inside `sourceSets { ... }`, after `commonMain.dependencies { ... }` add:

```kotlin
        androidMain.dependencies { implementation(libs.androidx.activity.compose) }
```

In `androidApp/src/main/AndroidManifest.xml`, add before `<application`:

```xml
    <uses-permission android:name="android.permission.RECORD_AUDIO" />
```

- [ ] **Step 7: Run the tests and the Android build**

Run: `./gradlew :composeApp:iosSimulatorArm64Test --tests '*MicrophoneIosTest*' :androidApp:assembleDebug`
Expected: BUILD SUCCESSFUL. The Android recorder itself is checked on the device in Task 10.

- [ ] **Step 8: Commit**

```bash
./gradlew spotlessApply
git add composeApp/src/commonMain/kotlin/dev/dimvlachos/lab/core/audio/Microphone.kt composeApp/src/androidMain/kotlin/dev/dimvlachos/lab/core/audio composeApp/src/iosMain/kotlin/dev/dimvlachos/lab/core/audio composeApp/src/iosTest composeApp/build.gradle.kts androidApp/src/main/AndroidManifest.xml
git commit -m "feat: listen to the microphone on Android, with its permission" -m "Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
Claude-Session: https://claude.ai/code/session_01TkrycYKRAwVDjLBuvHiu6E"
```

---

### Task 9: Blow, or hold, to fog the glass in the demo

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/dev/dimvlachos/lab/frostdemo/presentation/components/FrostDemo.kt`
- Modify: `composeApp/src/commonMain/composeResources/values/strings.xml`
- Modify: `gradle/libs.versions.toml`, `composeApp/build.gradle.kts`
- Test: `composeApp/src/commonTest/kotlin/dev/dimvlachos/lab/frostdemo/presentation/components/FrostDemoUiTest.kt`
- Create: `composeApp/src/commonTest/kotlin/dev/dimvlachos/lab/core/audio/FakeMicrophone.kt`

**Interfaces:**
- Consumes: `BlowDetector`, `MicFrameSeconds` (Task 4); `BreathDriver` (Task 5); `DemoState.recording` (Task 6); `Microphone`, `MicAccess`, `rememberMicAccess` (Task 8); `FoggedMirror(onHoldChange)` (Task 3); test signals (Task 4).
- Produces: `FrostDemo(state: DemoState, frost: FrostState = …, micAccess: MicAccess = rememberMicAccess(enabled = !state.recording))`; strings `frost_hint_blow`, `frost_hint_hold`.

- [ ] **Step 1: Add the lifecycle dependency and the strings**

In `gradle/libs.versions.toml`, under `[versions]` add `lifecycle = "2.9.6"`; under `[libraries]` add:

```toml
lifecycle-runtime-compose = { module = "org.jetbrains.androidx.lifecycle:lifecycle-runtime-compose", version.ref = "lifecycle" }
```

In `composeApp/build.gradle.kts`, inside `commonMain.dependencies { ... }` add `implementation(libs.lifecycle.runtime.compose)`.

In `strings.xml`, after `demo_frost` add:

```xml
    <string name="frost_hint_blow">Blow on the screen</string>
    <string name="frost_hint_hold">Hold to breathe</string>
```

- [ ] **Step 2: Write the failing tests**

`FakeMicrophone.kt` (commonTest):

```kotlin
package dev.dimvlachos.lab.core.audio

import kotlinx.coroutines.flow.Flow

internal class FakeMicrophone(override val frames: Flow<FloatArray>) : Microphone
```

Add to `FrostDemoUiTest`. Imports to add: `androidx.compose.runtime.CompositionLocalProvider`, `androidx.compose.ui.test.ComposeUiTest`, `androidx.compose.ui.test.onNodeWithText`, `androidx.compose.ui.test.onNodeWithTag`, `androidx.compose.ui.platform.testTag`, `androidx.compose.ui.test.performTouchInput`, `androidx.lifecycle.Lifecycle`, `androidx.lifecycle.LifecycleOwner`, `androidx.lifecycle.LifecycleRegistry`, `androidx.lifecycle.compose.LocalLifecycleOwner`, `dev.dimvlachos.lab.core.audio.FakeMicrophone`, `dev.dimvlachos.lab.core.audio.MicAccess`, `dev.dimvlachos.lab.core.audio.noise`, `dev.dimvlachos.lab.core.audio.silence`, `dev.dimvlachos.lab.core.presentation.components.frost.Breath`, `dev.dimvlachos.lab.resources.Res`, `dev.dimvlachos.lab.resources.frost_hint_blow`, `dev.dimvlachos.lab.resources.frost_hint_hold`, `kotlin.random.Random`, `kotlinx.coroutines.awaitCancellation`, `kotlinx.coroutines.flow.Flow`, `kotlinx.coroutines.flow.flow`, `kotlinx.coroutines.runBlocking`, `org.jetbrains.compose.resources.getString`.

```kotlin
    // Test hosts are not guaranteed to be resumed; the microphone only listens when they are.
    private class ResumedOwner : LifecycleOwner {
        override val lifecycle =
            LifecycleRegistry.createUnsafe(this).apply { currentState = Lifecycle.State.RESUMED }
    }

    private val blowHint = runBlocking { getString(Res.string.frost_hint_blow) }
    private val holdHint = runBlocking { getString(Res.string.frost_hint_hold) }

    private fun ComposeUiTest.showDemo(state: DemoState, frost: FrostState, micAccess: MicAccess) {
        setContent {
            CompositionLocalProvider(LocalLifecycleOwner provides ResumedOwner()) {
                LabTheme {
                    Box(Modifier.size(400.dp, 500.dp).testTag("demo")) {
                        FrostDemo(state, frost, micAccess)
                    }
                }
            }
        }
    }

    private fun micHearing(frames: Flow<FloatArray>) =
        MicAccess.Granted(FakeMicrophone(frames))

    @Test
    fun aBlowIntoTheMicrophoneFogsTheGlass() = runComposeUiTest {
        val random = Random(3)
        val frost = FrostState()
        showDemo(
            DemoState(),
            frost,
            micHearing(
                flow {
                    repeat(20) { emit(noise(0.003f, random)) }
                    repeat(12) { emit(noise(0.3f, random)) }
                    awaitCancellation()
                }
            ),
        )
        waitForIdle()

        val fog = frost.marks.last() as Breath
        assertTrue(fog.level > 0f, "the fog rose: ${fog.level}")
        onNodeWithText(blowHint).assertDoesNotExist()
    }

    @Test
    fun aListeningMicrophoneInvitesABlow() = runComposeUiTest {
        showDemo(DemoState(), FrostState(), micHearing(flow { awaitCancellation() }))
        onNodeWithText(blowHint).assertExists()
    }

    @Test
    fun aMicrophoneThatFailsFallsBackToHolding() = runComposeUiTest {
        showDemo(DemoState(), FrostState(), micHearing(flow { throw IllegalStateException("busy") }))
        onNodeWithText(holdHint).assertExists()
    }

    @Test
    fun aMicrophoneThatHearsOnlySilenceFallsBackToHolding() = runComposeUiTest {
        showDemo(
            DemoState(),
            FrostState(),
            micHearing(
                flow {
                    repeat(80) { emit(silence()) }
                    awaitCancellation()
                }
            ),
        )
        onNodeWithText(holdHint).assertExists()
    }

    @Test
    fun withoutAMicrophoneHoldingTheGlassFogsIt() = runComposeUiTest {
        mainClock.autoAdvance = false
        val frost = FrostState()
        showDemo(DemoState(), frost, MicAccess.Unavailable)
        mainClock.advanceTimeByFrame()
        onNodeWithText(holdHint).assertExists()

        onNodeWithTag("demo").performTouchInput { down(center) }
        mainClock.advanceTimeBy(1_000)
        onNodeWithTag("demo").performTouchInput { up() }
        mainClock.advanceTimeByFrame()

        val fog = frost.marks.last() as Breath
        assertTrue(fog.level > 0.2f, "about 0.6 s of breath at 0.7: ${fog.level}")
    }

    @Test
    fun theRecordingShowsNoHints() = runComposeUiTest {
        showDemo(DemoState(recording = true), FrostState(), MicAccess.Unavailable)
        onNodeWithText(holdHint).assertDoesNotExist()
        onNodeWithText(blowHint).assertDoesNotExist()
    }
```

In the two existing tests of `FrostDemoUiTest`, change `FrostDemo(state, frost)` to `FrostDemo(state, frost, MicAccess.Unavailable)`.

- [ ] **Step 3: Run the tests to verify they fail**

Run: `./gradlew :composeApp:iosSimulatorArm64Test --tests '*FrostDemoUiTest*'`
Expected: compilation fails: `FrostDemo` has no third parameter.

- [ ] **Step 4: Implement**

Replace `FrostDemo.kt` with:

```kotlin
package dev.dimvlachos.lab.frostdemo.presentation.components

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.toSize
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import co.touchlab.kermit.Logger
import dev.dimvlachos.lab.core.audio.BlowDetector
import dev.dimvlachos.lab.core.audio.MicAccess
import dev.dimvlachos.lab.core.audio.MicFrameSeconds
import dev.dimvlachos.lab.core.audio.rememberMicAccess
import dev.dimvlachos.lab.core.demo.DemoState
import dev.dimvlachos.lab.core.presentation.components.frost.FrostState
import dev.dimvlachos.lab.core.presentation.components.frost.FoggedMirror
import dev.dimvlachos.lab.core.presentation.ui.LabTheme
import dev.dimvlachos.lab.frostdemo.BreathDriver
import dev.dimvlachos.lab.frostdemo.FrostDemos
import dev.dimvlachos.lab.frostdemo.clipFrameToWindow
import dev.dimvlachos.lab.frostdemo.pointAt
import dev.dimvlachos.lab.frostdemo.scriptedBreathStrength
import dev.dimvlachos.lab.resources.Res
import dev.dimvlachos.lab.resources.frost_hint_blow
import dev.dimvlachos.lab.resources.frost_hint_hold
import dev.dimvlachos.lab.resources.photo_santorini
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

// A held finger breathes steadily, a little softer than a firm blow.
private const val HoldStrength = 0.7f

// About 2 s of nothing but zeros: the microphone is taken by something else, a call perhaps.
private const val SilentMicFrames = 60

private val log = Logger.withTag("FrostDemo")

@Composable
internal fun FrostDemo(
    state: DemoState,
    frost: FrostState = remember { FrostState() },
    micAccess: MicAccess = rememberMicAccess(enabled = !state.recording),
) {
    var window by remember { mutableStateOf(Size.Zero) }
    val driver = remember(frost) { BreathDriver(frost) }
    var micFailed by remember { mutableStateOf(false) }
    var breathed by remember { mutableStateOf(false) }
    var holding by remember { mutableStateOf(false) }
    val listening = micAccess is MicAccess.Granted && !micFailed

    // The script's wipe plays its finger back sample by sample: the path already holds the hand's
    // speed, so the playback itself is linear. The path is drawn in the clip's frame, placed on
    // whatever window this is.
    DisposableEffect(state, frost, driver) {
        state.setWipeHandler { path, duration ->
            if (window.isEmpty()) return@setWipeHandler
            val stroke = frost.beginStroke(clipFrameToWindow(path.first(), window))
            animate(
                0f,
                1f,
                animationSpec = tween(duration.inWholeMilliseconds.toInt(), easing = LinearEasing),
            ) { t, _ ->
                frost.extendStroke(stroke, clipFrameToWindow(pointAt(path, t), window))
            }
        }
        // The script's breath swells and fades through the same driver as a real one.
        state.setBreatheHandler { duration, strength ->
            val seconds = duration.inWholeMilliseconds / 1000f
            var previous = 0f
            animate(
                0f,
                1f,
                animationSpec = tween(duration.inWholeMilliseconds.toInt(), easing = LinearEasing),
            ) { t, _ ->
                driver.advance(scriptedBreathStrength(t, strength), (t - previous) * seconds)
                previous = t
            }
        }
        onDispose {
            state.setWipeHandler(null)
            state.setBreatheHandler(null)
        }
    }

    // A blow on the microphone fogs the glass, only while the demo is in front of the user.
    if (micAccess is MicAccess.Granted && !micFailed) {
        val lifecycle = LocalLifecycleOwner.current.lifecycle
        LaunchedEffect(micAccess, lifecycle) {
            lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                val detector = BlowDetector()
                var silentFrames = 0
                try {
                    micAccess.microphone.frames.collect { frame ->
                        silentFrames = if (frame.all { it == 0f }) silentFrames + 1 else 0
                        check(silentFrames < SilentMicFrames) { "the microphone hears only silence" }
                        val strength = detector.process(frame)
                        if (strength > 0f) {
                            driver.advance(strength, MicFrameSeconds)
                            breathed = true
                        }
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    log.w(e) { "no microphone, so holding the glass breathes on it" }
                    micFailed = true
                }
            }
        }
    }

    // Without a microphone, a held finger breathes on the glass for as long as it stays.
    LaunchedEffect(holding) {
        if (!holding) return@LaunchedEffect
        var previous = withFrameNanos { it }
        while (true) {
            withFrameNanos { now ->
                driver.advance(HoldStrength, (now - previous) / 1_000_000_000f)
                previous = now
            }
            breathed = true
        }
    }

    // Back on 0, the loop's start, the glass frosts over again. Only on a return to 0: clearing on
    // the first composition too would race a microphone that is already fogging the glass.
    LaunchedEffect(state, frost) {
        snapshotFlow { state.selectedIndex }.drop(1).filter { it == 0 }.collect { frost.clear() }
    }

    Box(Modifier.fillMaxSize()) {
        FoggedMirror(
            photo = painterResource(Res.drawable.photo_santorini),
            state = frost,
            modifier = Modifier.fillMaxSize().onSizeChanged { window = it.toSize() },
            brushRadius = FrostDemos.ScrubBrush,
            onHoldChange = if (listening || state.recording) null else { held -> holding = held },
        )
        val hint =
            when {
                state.recording || breathed -> null
                listening -> Res.string.frost_hint_blow
                micAccess is MicAccess.Pending -> null
                else -> Res.string.frost_hint_hold
            }
        Crossfade(
            targetState = hint,
            modifier = Modifier.align(Alignment.BottomCenter).padding(LabTheme.spacing.mediumLarge),
        ) { shown ->
            if (shown != null) {
                Text(
                    stringResource(shown),
                    color = LabTheme.colors.textPrimary,
                    style = LabTheme.typography.body,
                    modifier =
                        Modifier.background(
                                LabTheme.colors.surface.copy(alpha = 0.7f),
                                RoundedCornerShape(percent = 50),
                            )
                            .padding(
                                horizontal = LabTheme.spacing.mediumLarge,
                                vertical = LabTheme.spacing.small,
                            ),
                )
            }
        }
    }
}
```

- [ ] **Step 5: Run the tests to verify they pass**

Run: `./gradlew :composeApp:iosSimulatorArm64Test`
Expected: BUILD SUCCESSFUL, with `FrostDemoUiTest` at 8 tests. If `aBlowIntoTheMicrophoneFogsTheGlass` sees no breath, check that `LocalLifecycleOwner` is imported from `androidx.lifecycle.compose` in both the test and `FrostDemo`; the provider only overrides that exact local.

- [ ] **Step 6: Run Konsist and the Android build**

Run: `./gradlew spotlessApply :androidApp:testDebugUnitTest --rerun :androidApp:assembleDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 7: Commit**

```bash
git add composeApp/src/commonMain/kotlin/dev/dimvlachos/lab/frostdemo/presentation/components/FrostDemo.kt composeApp/src/commonMain/composeResources/values/strings.xml gradle/libs.versions.toml composeApp/build.gradle.kts composeApp/src/commonTest/kotlin/dev/dimvlachos/lab/frostdemo/presentation/components/FrostDemoUiTest.kt composeApp/src/commonTest/kotlin/dev/dimvlachos/lab/core/audio/FakeMicrophone.kt
git commit -m "feat: blow on the phone, or hold the glass, to fog the fogged mirror over" -m "Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
Claude-Session: https://claude.ai/code/session_01TkrycYKRAwVDjLBuvHiu6E"
```

---

### Task 10: On the device: blow, talk, deny, record

This task is manual; it verifies what the tests cannot: the real microphone, the permission dialog and the look of the fog.

**Files:**
- Modify only if tuning is needed: `composeApp/src/commonMain/kotlin/dev/dimvlachos/lab/core/audio/BlowDetector.kt` (constants `MarginDb`, `MinFlatness`, `MinLevelDb`)

- [ ] **Step 1: Install and grant**

Run:
```bash
./gradlew :androidApp:installDebug
adb shell pm revoke dev.dimvlachos.lab android.permission.RECORD_AUDIO
adb shell am start -S -n dev.dimvlachos.lab/.MainActivity --es demo frost.window
```
Expected: the permission dialog appears over the frost demo. Tap Allow; "Blow on the screen" shows over the glass and the system mic indicator is on.

- [ ] **Step 2: Blow, talk, stay quiet**

With the demo open (wait for the scripted scrub to clear the oval, or wipe it by hand):
- Blow steadily at the bottom of the phone for 2 s. Expected: fog rises from the bottom, billowing, and covers the glass; the hint fades.
- Wipe a patch, then talk normally at the phone for 5 s. Expected: no fog.
- Stay quiet for 10 s. Expected: no fog.

If a blow does not register, lower `MarginDb` to 12 or `MinFlatness` to 0.3; if talking fogs the glass, raise `MinFlatness` to 0.4. Re-run `./gradlew :composeApp:iosSimulatorArm64Test --tests '*BlowDetectorTest*'` after any change, then repeat this step.

- [ ] **Step 3: Background and return**

Press Home, wait 3 s, return to the app. Expected: the mic indicator turns off while away and back on when the demo is visible again; blowing still works.

- [ ] **Step 4: Deny**

Run:
```bash
adb shell pm revoke dev.dimvlachos.lab android.permission.RECORD_AUDIO
adb shell am start -S -n dev.dimvlachos.lab/.MainActivity --es demo frost.window
```
Tap Don't allow. Expected: "Hold to breathe" shows. Press and hold the glass without moving. Expected: after about 0.4 s fog rises until release; dragging still wipes.

- [ ] **Step 5: Record the clip**

Run: `python3 scripts/record.py frost.window`
Expected: no permission dialog during recording; the clip shows the scrub, a pause, then fog rolling up and covering the glass, and the loop point shows no jump. Make a GIF of the recording and review it with the user.

- [ ] **Step 6: Commit any tuning**

If Step 2 changed constants:
```bash
./gradlew spotlessApply
git add composeApp/src/commonMain/kotlin/dev/dimvlachos/lab/core/audio/BlowDetector.kt
git commit -m "fix: tune blow detection on a real phone" -m "Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
Claude-Session: https://claude.ai/code/session_01TkrycYKRAwVDjLBuvHiu6E"
```
