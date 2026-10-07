package dev.dimvlachos.lab.fogdemo.presentation.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.click
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.compose.LocalLifecycleOwner
import dev.dimvlachos.lab.core.camera.CameraAccess
import dev.dimvlachos.lab.core.camera.FakeMirrorCamera
import dev.dimvlachos.lab.core.demo.DemoState
import dev.dimvlachos.lab.core.presentation.components.fog.Breath
import dev.dimvlachos.lab.core.presentation.components.fog.Evaporation
import dev.dimvlachos.lab.core.presentation.components.fog.FogState
import dev.dimvlachos.lab.core.presentation.components.fog.Mist
import dev.dimvlachos.lab.core.presentation.components.fog.WipeStroke
import dev.dimvlachos.lab.core.presentation.ui.LabTheme
import dev.dimvlachos.lab.fogdemo.FogDemos
import dev.dimvlachos.lab.fogdemo.SlowFogTest
import dev.dimvlachos.lab.resources.Res
import dev.dimvlachos.lab.resources.fog_hint_camera
import dev.dimvlachos.lab.resources.fog_hint_wipe
import dev.dimvlachos.lab.resources.mirror_card_allow
import dev.dimvlachos.lab.resources.mirror_card_body_what
import dev.dimvlachos.lab.resources.mirror_card_camera_blocked
import dev.dimvlachos.lab.resources.mirror_card_camera_why
import dev.dimvlachos.lab.resources.mirror_card_not_now
import dev.dimvlachos.lab.resources.mirror_card_open_settings
import dev.dimvlachos.lab.resources.mirror_card_promise
import dev.dimvlachos.lab.resources.mirror_card_title
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.getString

@OptIn(ExperimentalTestApi::class)
class FogDemoUiTest {
    @Test
    fun scriptedWipesDrawStrokesThatStayWhenTheSelectionMoves() = runComposeUiTest {
        mainClock.autoAdvance = false
        val state = DemoState()
        val fog = FogState()
        var scope: CoroutineScope? = null
        setContent {
            scope = rememberCoroutineScope()
            // The clip's own 4:5 frame, so the script's points land where they say.
            LabTheme {
                Box(Modifier.size(400.dp, 500.dp)) {
                    FogDemo(state, fog, CameraAccess.Unavailable)
                }
            }
        }
        mainClock.advanceTimeByFrame()
        runOnUiThread {
            state.select(1)
            scope!!.launch {
                state.wipe(listOf(Offset(0.1f, 0.5f), Offset(0.9f, 0.5f)), 400.milliseconds)
            }
        }
        Snapshot.sendApplyNotifications()
        mainClock.advanceTimeBy(600)

        assertEquals(1, fog.strokes.size)
        val stroke = fog.strokes.single()
        assertTrue(
            (stroke.first() - Offset(0.1f, 0.5f)).getDistance() < 0.001f,
            "${stroke.first()}",
        )
        assertTrue(stroke.last().x > 0.89f, "the finger reaches the end: $stroke")

        runOnUiThread { state.select(0) }
        Snapshot.sendApplyNotifications()
        mainClock.advanceTimeBy(500)

        // The mist is the clip's reset now: going back to 0 leaves the wipe on the glass.
        mainClock.advanceTimeBy(1_200)
        assertEquals(1, fog.strokes.size)
        assertTrue(fog.marks.none { it is Evaporation }, "${fog.marks}")
    }

    // Test hosts are not guaranteed to be resumed; the room's mist only runs when they are.
    private class Owner(state: Lifecycle.State) : LifecycleOwner {
        override val lifecycle = LifecycleRegistry.createUnsafe(this).apply { currentState = state }
    }

    // The card and the pill do not wait on the room: paused, its mist stays off rather than running
    // on through every wait for idle.
    private fun pausedOwner() = Owner(Lifecycle.State.STARTED)

    // Glass already wiped once, past the first hint ("wipe away the steam").
    private fun wipedFog() = FogState().apply { beginStroke(Offset(0.5f, 0.5f)) }

    private val wipeHint = runBlocking { getString(Res.string.fog_hint_wipe) }
    private val cameraHint = runBlocking { getString(Res.string.fog_hint_camera) }
    private val cardTitle = runBlocking { getString(Res.string.mirror_card_title) }
    private val allow = runBlocking { getString(Res.string.mirror_card_allow) }
    private val notNow = runBlocking { getString(Res.string.mirror_card_not_now) }
    private val openSettings = runBlocking { getString(Res.string.mirror_card_open_settings) }
    private val blockedLine = runBlocking { getString(Res.string.mirror_card_camera_blocked) }

    private fun ComposeUiTest.showDemo(state: DemoState, fog: FogState, camera: CameraAccess) {
        setContent {
            CompositionLocalProvider(LocalLifecycleOwner provides pausedOwner()) {
                LabTheme {
                    Box(Modifier.size(400.dp, 500.dp).testTag("demo")) {
                        FogDemo(state, fog, camera)
                    }
                }
            }
        }
    }

    @Test
    fun theRecordingShowsNoHints() = runComposeUiTest {
        showDemo(DemoState(recording = true), FogState(), CameraAccess.Unavailable)
        onNodeWithText(wipeHint).assertDoesNotExist()
        onNodeWithText(cameraHint).assertDoesNotExist()
    }

    // Taps on the card go through real touch handling, not the click action: a layer over the glass
    // once swallowed every tap before the buttons saw it, and performClick() never noticed.

    // The access the demo is shown with, changeable mid-test as Android would change it.
    private fun ComposeUiTest.showDemoWith(
        access: () -> CameraAccess,
        fog: FogState = wipedFog(),
    ) {
        setContent {
            CompositionLocalProvider(LocalLifecycleOwner provides pausedOwner()) {
                LabTheme {
                    Box(Modifier.size(400.dp, 500.dp).testTag("demo")) {
                        FogDemo(DemoState(), fog, access())
                    }
                }
            }
        }
    }

    @Test
    fun theCardExplainsBeforeAndroidAsks() = runComposeUiTest {
        showDemoWith({ CameraAccess.Askable {} })

        onNodeWithText(cardTitle).assertExists()
        onNodeWithText(cameraHint).assertDoesNotExist()
    }

    @Test
    fun allowingHandsOverToAndroid() = runComposeUiTest {
        var asks = 0
        showDemoWith({ CameraAccess.Askable { asks++ } })

        onNodeWithText(allow).performTouchInput { click() }

        assertEquals(1, asks)
    }

    @Test
    fun aRefusalInAndroidsDialogClosesTheCardOnTheStillReflection() = runComposeUiTest {
        // Android hands over a new Askable after each refusal it asked about.
        var access by mutableStateOf<CameraAccess>(CameraAccess.Askable {})
        showDemoWith({ access })

        onNodeWithText(allow).performTouchInput { click() }
        access = CameraAccess.Askable {}
        waitForIdle()

        onNodeWithText(cardTitle).assertDoesNotExist()
        onNodeWithText(cameraHint).assertExists()
    }

    @Test
    fun whenAndroidWillNotAskAnyMoreTheCardOffersSettings() = runComposeUiTest {
        var settings = 0
        var access by mutableStateOf<CameraAccess>(CameraAccess.Askable {})
        showDemoWith({ access })

        onNodeWithText(allow).performTouchInput { click() }
        access = CameraAccess.Blocked { settings++ }
        waitForIdle()

        onNodeWithText(blockedLine).assertExists()
        onNodeWithText(openSettings).performTouchInput { click() }
        assertEquals(1, settings)
    }

    @Test
    fun alreadyBlockedTheCardOpensOnSettings() = runComposeUiTest {
        showDemoWith({ CameraAccess.Blocked {} })

        onNodeWithText(openSettings).assertExists()
        onNodeWithText(allow).assertDoesNotExist()
    }

    @Test
    fun notNowLeadsToTheStillReflectionAndThePillBringsTheCardBack() = runComposeUiTest {
        showDemoWith({ CameraAccess.Askable {} })

        onNodeWithText(notNow).performTouchInput { click() }
        onNodeWithText(cardTitle).assertDoesNotExist()

        onNodeWithText(cameraHint).performTouchInput { click() }
        onNodeWithText(cardTitle).assertExists()
    }

    @Test
    fun theGlassIgnoresTouchesWhileTheCardIsOpen() = runComposeUiTest {
        val fog = FogState()
        showDemoWith({ CameraAccess.Askable {} }, fog)

        onNodeWithTag("demo").performTouchInput { swipe(topLeft, topRight) }

        assertTrue(fog.strokes.isEmpty())
    }

    @Test
    fun noCardOrPillWithoutACameraToAskFor() = runComposeUiTest {
        showDemoWith({ CameraAccess.Unavailable })

        onNodeWithText(cardTitle).assertDoesNotExist()
        onNodeWithText(cameraHint).assertDoesNotExist()
    }

    @Test
    fun noCardOrPillOnceTheCameraIsGranted() = runComposeUiTest {
        showDemoWith({ CameraAccess.Granted(FakeMirrorCamera()) })

        onNodeWithText(cardTitle).assertDoesNotExist()
        onNodeWithText(cameraHint).assertDoesNotExist()
    }

    @Test
    fun noCardInTheRecording() = runComposeUiTest {
        showDemo(DemoState(recording = true), FogState(), CameraAccess.Askable {})

        onNodeWithText(cardTitle).assertDoesNotExist()
    }

    @Test
    fun theCardsWordsShowNoEscapeCharacters() {
        // Compose resources print an Android-style \' as it is, backslash and all.
        val words =
            listOf(
                    Res.string.mirror_card_title,
                    Res.string.mirror_card_body_what,
                    Res.string.mirror_card_camera_why,
                    Res.string.mirror_card_promise,
                    Res.string.mirror_card_camera_blocked,
                    Res.string.fog_hint_camera,
                )
                .map { runBlocking { getString(it) } }
        assertTrue(words.none { '\\' in it }, "$words")
    }

    @Test
    fun aRealFingerThatShiftsAPixelStillPressesTheCardsButtons() = runComposeUiTest {
        // A finger always moves a little while down; the card's buttons must not lose it.
        showDemoWith({ CameraAccess.Askable {} })

        onNodeWithText(notNow).performTouchInput {
            down(center)
            moveBy(Offset(2f, 1f))
            moveBy(Offset(1f, 2f))
            up()
        }

        onNodeWithText(cardTitle).assertDoesNotExist()
    }

    @Test
    fun replayingTheClipShowsNoCardOverIt() = runComposeUiTest {
        showDemo(DemoState(replay = true), wipedFog(), CameraAccess.Askable {})

        onNodeWithText(cardTitle).assertDoesNotExist()
        onNodeWithText(cameraHint).assertDoesNotExist()
    }

    // The live mirror, like the bathroom's, is in a steamy room: left alone, it mists back over.
    @Test
    fun theWipedLiveMirrorMistsBackOverByItself() =
        runComposeUiTest(testTimeout = SlowFogTest) {
            mainClock.autoAdvance = false
            val fog = FogState()
            showLiveMirror(DemoState(), fog)
            mainClock.advanceTimeByFrame()
            onNodeWithTag("demo").performTouchInput {
                swipe(percentOffset(0.25f, 0.4f), percentOffset(0.75f, 0.4f))
            }
            mainClock.advanceTimeByFrame()
            assertTrue(fog.marks.any { it is WipeStroke && it.clarity >= 1f })

            mainClock.advanceTimeBy(32_000)
            assertTrue(
                fog.marks.none { it is WipeStroke && it.clarity >= 1f },
                "fogged over again: ${fog.marks}",
            )
        }

    @Test
    fun holdingTheLiveMirrorDoesNotBreatheOnIt() = runComposeUiTest {
        mainClock.autoAdvance = false
        val fog = FogState()
        showLiveMirror(DemoState(), fog)
        mainClock.advanceTimeByFrame()

        onNodeWithTag("demo").performTouchInput { down(center) }
        mainClock.advanceTimeBy(1_500)
        onNodeWithTag("demo").performTouchInput { up() }
        mainClock.advanceTimeByFrame()

        assertTrue(fog.marks.none { it is Breath }, "${fog.marks}")
    }

    @Test
    fun theLiveMirrorsReplayAndRecordingDoNotMistItOverByThemselves() = runComposeUiTest {
        mainClock.autoAdvance = false
        val fog = FogState()
        fog.beginStroke(Offset(0.2f, 0.5f)).also { fog.extendStroke(it, Offset(0.8f, 0.5f)) }
        var state by mutableStateOf(DemoState(replay = true))
        showLiveMirror({ state }, fog)
        mainClock.advanceTimeBy(10_000)
        assertTrue(fog.marks.none { it is Mist }, "only the script mists it: ${fog.marks}")

        state = DemoState(recording = true)
        mainClock.advanceTimeBy(10_000)
        assertTrue(fog.marks.none { it is Mist }, "${fog.marks}")
    }

    private fun ComposeUiTest.showLiveMirror(state: DemoState, fog: FogState) =
        showLiveMirror({ state }, fog)

    private fun ComposeUiTest.showLiveMirror(state: () -> DemoState, fog: FogState) {
        setContent {
            CompositionLocalProvider(LocalLifecycleOwner provides Owner(Lifecycle.State.RESUMED)) {
                LabTheme {
                    Box(Modifier.size(400.dp, 500.dp).testTag("demo")) {
                        FogDemo(
                            state(),
                            fog,
                            CameraAccess.Granted(FakeMirrorCamera()),
                        )
                    }
                }
            }
        }
    }

    @Test
    fun theScriptScrubsWithTheFlatOfAFinger() = runComposeUiTest {
        mainClock.autoAdvance = false
        val state = DemoState()
        val fog = FogState()
        var scope: CoroutineScope? = null
        setContent {
            scope = rememberCoroutineScope()
            LabTheme {
                Box(Modifier.size(400.dp, 500.dp)) {
                    FogDemo(state, fog, CameraAccess.Unavailable)
                }
            }
        }
        mainClock.advanceTimeByFrame()
        runOnUiThread {
            scope!!.launch {
                state.wipe(listOf(Offset(0.2f, 0.5f), Offset(0.8f, 0.5f)), 300.milliseconds)
            }
        }
        mainClock.advanceTimeBy(500)

        val stroke = fog.marks.single() as WipeStroke
        assertEquals(FogDemos.FingerBrush, stroke.radius)
    }
}
