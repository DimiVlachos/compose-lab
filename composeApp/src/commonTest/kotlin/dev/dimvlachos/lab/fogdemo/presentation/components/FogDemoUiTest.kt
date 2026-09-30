package dev.dimvlachos.lab.fogdemo.presentation.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import dev.dimvlachos.lab.core.audio.FakeMicrophone
import dev.dimvlachos.lab.core.audio.MicAccess
import dev.dimvlachos.lab.core.audio.noise
import dev.dimvlachos.lab.core.audio.silence
import dev.dimvlachos.lab.core.demo.DemoState
import dev.dimvlachos.lab.core.presentation.components.fog.Breath
import dev.dimvlachos.lab.core.presentation.components.fog.Evaporation
import dev.dimvlachos.lab.core.presentation.components.fog.FogState
import dev.dimvlachos.lab.core.presentation.components.fog.WipeStroke
import dev.dimvlachos.lab.core.presentation.ui.LabTheme
import dev.dimvlachos.lab.fogdemo.FogDemos
import dev.dimvlachos.lab.resources.Res
import dev.dimvlachos.lab.resources.fog_hint_blow
import dev.dimvlachos.lab.resources.fog_hint_hold
import dev.dimvlachos.lab.resources.mic_card_allow
import dev.dimvlachos.lab.resources.mic_card_blocked
import dev.dimvlachos.lab.resources.mic_card_body_what
import dev.dimvlachos.lab.resources.mic_card_body_why
import dev.dimvlachos.lab.resources.mic_card_not_now
import dev.dimvlachos.lab.resources.mic_card_open_settings
import dev.dimvlachos.lab.resources.mic_card_title
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onStart
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
                    FogDemo(state, fog, MicAccess.Unavailable)
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

        // The breath is the clip's reset now: going back to 0 leaves the drawing on the glass.
        mainClock.advanceTimeBy(1_200)
        assertEquals(1, fog.strokes.size)
        assertTrue(fog.marks.none { it is Evaporation }, "${fog.marks}")
    }

    @Test
    fun aScriptedBreathFogsTheGlassOver() = runComposeUiTest {
        mainClock.autoAdvance = false
        val state = DemoState()
        val fog = FogState()
        var scope: CoroutineScope? = null
        setContent {
            scope = rememberCoroutineScope()
            LabTheme {
                Box(Modifier.size(400.dp, 500.dp)) {
                    FogDemo(state, fog, MicAccess.Unavailable)
                }
            }
        }
        mainClock.advanceTimeByFrame()
        // Wiped once the demo is up: on its first composition the demo starts from fresh fog.
        runOnUiThread { fog.beginStroke(Offset(0.5f, 0.5f)) }
        runOnUiThread { scope!!.launch { state.breathe(2_400.milliseconds, 1f) } }
        mainClock.advanceTimeBy(2_600)

        assertTrue(fog.strokes.isEmpty(), "the wipe is fogged over: ${fog.marks}")
    }

    // Test hosts are not guaranteed to be resumed; the microphone only listens when they are.
    private class ResumedOwner : LifecycleOwner {
        override val lifecycle =
            LifecycleRegistry.createUnsafe(this).apply { currentState = Lifecycle.State.RESUMED }
    }

    private val blowHint = runBlocking { getString(Res.string.fog_hint_blow) }
    private val holdHint = runBlocking { getString(Res.string.fog_hint_hold) }

    private fun ComposeUiTest.showDemo(state: DemoState, fog: FogState, micAccess: MicAccess) {
        setContent {
            CompositionLocalProvider(LocalLifecycleOwner provides ResumedOwner()) {
                LabTheme {
                    Box(Modifier.size(400.dp, 500.dp).testTag("demo")) {
                        FogDemo(state, fog, micAccess)
                    }
                }
            }
        }
    }

    private fun micHearing(frames: Flow<FloatArray>) = MicAccess.Granted(FakeMicrophone(frames))

    @Test
    fun aBlowIntoTheMicrophoneFogsTheGlass() = runComposeUiTest {
        val random = Random(3)
        val fog = FogState()
        showDemo(
            DemoState(),
            fog,
            micHearing(
                flow {
                    repeat(20) { emit(noise(0.003f, random)) }
                    repeat(12) { emit(noise(0.3f, random)) }
                    awaitCancellation()
                }
            ),
        )
        waitForIdle()

        val breath = fog.marks.last() as Breath
        assertTrue(breath.level > 0f, "the fog rose: ${breath.level}")
        onNodeWithText(blowHint).assertDoesNotExist()
    }

    @Test
    fun aListeningMicrophoneInvitesABlow() = runComposeUiTest {
        showDemo(DemoState(), FogState(), micHearing(flow { awaitCancellation() }))
        onNodeWithText(blowHint).assertExists()
    }

    @Test
    fun aMicrophoneThatFailsFallsBackToHolding() = runComposeUiTest {
        showDemo(
            DemoState(),
            FogState(),
            micHearing(flow { throw IllegalStateException("busy") }),
        )
        onNodeWithText(holdHint).assertExists()
    }

    @Test
    fun aMicrophoneThatHearsOnlySilenceFallsBackToHolding() = runComposeUiTest {
        showDemo(
            DemoState(),
            FogState(),
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
        val fog = FogState()
        showDemo(DemoState(), fog, MicAccess.Unavailable)
        mainClock.advanceTimeByFrame()
        onNodeWithText(holdHint).assertExists()

        onNodeWithTag("demo").performTouchInput { down(center) }
        mainClock.advanceTimeBy(1_000)
        onNodeWithTag("demo").performTouchInput { up() }
        mainClock.advanceTimeByFrame()

        val breath = fog.marks.last() as Breath
        assertTrue(breath.level > 0.2f, "about 0.6 s of breath at 0.7: ${breath.level}")
    }

    @Test
    fun theRecordingShowsNoHints() = runComposeUiTest {
        showDemo(DemoState(recording = true), FogState(), MicAccess.Unavailable)
        onNodeWithText(holdHint).assertDoesNotExist()
        onNodeWithText(blowHint).assertDoesNotExist()
    }

    @Test
    fun recomposingWhileListeningKeepsTheSameMicrophoneRunning() = runComposeUiTest {
        // On Android the access is a fresh wrapper around the same microphone at every
        // recomposition; listening must not restart for it, or a new detector learns the blow as
        // the room's level the moment the first breath recomposes the demo.
        var listens = 0
        val microphone =
            FakeMicrophone(flow<FloatArray> { awaitCancellation() }.onStart { listens++ })
        var recomposition by mutableIntStateOf(0)
        setContent {
            CompositionLocalProvider(LocalLifecycleOwner provides ResumedOwner()) {
                LabTheme {
                    recomposition.let {
                        FogDemo(DemoState(), FogState(), MicAccess.Granted(microphone))
                    }
                }
            }
        }
        waitForIdle()
        recomposition++
        waitForIdle()

        assertEquals(1, listens)
    }

    private val cardTitle = runBlocking { getString(Res.string.mic_card_title) }
    private val allow = runBlocking { getString(Res.string.mic_card_allow) }
    private val notNow = runBlocking { getString(Res.string.mic_card_not_now) }
    private val openSettings = runBlocking { getString(Res.string.mic_card_open_settings) }
    private val blockedLine = runBlocking { getString(Res.string.mic_card_blocked) }

    // Taps on the card go through real touch handling, not the click action: a layer over the glass
    // once swallowed every tap before the buttons saw it, and performClick() never noticed.

    // The access the demo is shown with, changeable mid-test as Android would change it.
    private fun ComposeUiTest.showDemoWith(
        access: () -> MicAccess,
        fog: FogState = FogState(),
    ) {
        setContent {
            CompositionLocalProvider(LocalLifecycleOwner provides ResumedOwner()) {
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
        showDemoWith({ MicAccess.Askable {} })

        onNodeWithText(cardTitle).assertExists()
        onNodeWithText(holdHint).assertDoesNotExist()
        onNodeWithText(blowHint).assertDoesNotExist()
    }

    @Test
    fun allowingHandsOverToAndroid() = runComposeUiTest {
        var asks = 0
        showDemoWith({ MicAccess.Askable { asks++ } })

        onNodeWithText(allow).performTouchInput { click() }

        assertEquals(1, asks)
    }

    @Test
    fun aRefusalInAndroidsDialogClosesTheCardAndOffersHolding() = runComposeUiTest {
        // Android hands over a new Askable after each refusal it asked about.
        var access by mutableStateOf<MicAccess>(MicAccess.Askable {})
        showDemoWith({ access })

        onNodeWithText(allow).performTouchInput { click() }
        access = MicAccess.Askable {}
        waitForIdle()

        onNodeWithText(cardTitle).assertDoesNotExist()
        onNodeWithText(holdHint).assertExists()
    }

    @Test
    fun whenAndroidWillNotAskAnyMoreTheCardOffersSettings() = runComposeUiTest {
        var settings = 0
        var access by mutableStateOf<MicAccess>(MicAccess.Askable {})
        showDemoWith({ access })

        onNodeWithText(allow).performTouchInput { click() }
        access = MicAccess.Blocked { settings++ }
        waitForIdle()

        onNodeWithText(blockedLine).assertExists()
        onNodeWithText(openSettings).performTouchInput { click() }
        assertEquals(1, settings)
    }

    @Test
    fun alreadyBlockedTheCardOpensOnSettings() = runComposeUiTest {
        showDemoWith({ MicAccess.Blocked {} })

        onNodeWithText(openSettings).assertExists()
        onNodeWithText(allow).assertDoesNotExist()
    }

    @Test
    fun notNowLeadsToHoldingAndTheHintBringsTheCardBack() = runComposeUiTest {
        showDemoWith({ MicAccess.Askable {} })

        onNodeWithText(notNow).performTouchInput { click() }
        onNodeWithText(cardTitle).assertDoesNotExist()

        onNodeWithText(holdHint).performTouchInput { click() }
        onNodeWithText(cardTitle).assertExists()
    }

    @Test
    fun theGlassIgnoresTouchesWhileTheCardIsOpen() = runComposeUiTest {
        val fog = FogState()
        showDemoWith({ MicAccess.Askable {} }, fog)

        onNodeWithTag("demo").performTouchInput { swipe(topLeft, topRight) }

        assertTrue(fog.strokes.isEmpty())
    }

    @Test
    fun noCardWithoutAMicrophoneToAskFor() = runComposeUiTest {
        showDemoWith({ MicAccess.Unavailable })

        onNodeWithText(cardTitle).assertDoesNotExist()
        onNodeWithText(holdHint).assertExists()
    }

    @Test
    fun noCardOnceTheMicrophoneIsGranted() = runComposeUiTest {
        showDemoWith({ micHearing(flow { awaitCancellation() }) })

        onNodeWithText(cardTitle).assertDoesNotExist()
        onNodeWithText(blowHint).assertExists()
    }

    @Test
    fun noCardInTheRecording() = runComposeUiTest {
        showDemo(DemoState(recording = true), FogState(), MicAccess.Askable {})

        onNodeWithText(cardTitle).assertDoesNotExist()
    }

    @Test
    fun theCardsWordsShowNoEscapeCharacters() {
        // Compose resources print an Android-style \' as it is, backslash and all.
        val words =
            listOf(
                    Res.string.mic_card_title,
                    Res.string.mic_card_body_what,
                    Res.string.mic_card_body_why,
                )
                .map { runBlocking { getString(it) } } + blockedLine
        assertTrue(words.none { '\\' in it }, "$words")
    }

    @Test
    fun aRealFingerThatShiftsAPixelStillPressesTheCardsButtons() = runComposeUiTest {
        // A finger always moves a little while down; the card's buttons must not lose it.
        showDemoWith({ MicAccess.Askable {} })

        onNodeWithText(notNow).performTouchInput {
            down(center)
            moveBy(Offset(2f, 1f))
            moveBy(Offset(1f, 2f))
            up()
        }

        onNodeWithText(cardTitle).assertDoesNotExist()
    }

    @Test
    fun afterHoldingThePillStillBringsTheCardBack() = runComposeUiTest {
        mainClock.autoAdvance = false
        showDemoWith({ MicAccess.Askable {} })
        mainClock.advanceTimeByFrame()
        onNodeWithText(notNow).performTouchInput { click() }
        mainClock.advanceTimeBy(500)

        onNodeWithTag("demo").performTouchInput { down(center) }
        mainClock.advanceTimeBy(1_000)
        onNodeWithTag("demo").performTouchInput { up() }
        mainClock.advanceTimeBy(500)

        onNodeWithText(holdHint).performTouchInput { click() }
        mainClock.advanceTimeBy(500)
        onNodeWithText(cardTitle).assertExists()
    }

    @Test
    fun replayingTheClipShowsNoCardOverIt() = runComposeUiTest {
        showDemo(DemoState(replay = true), FogState(), MicAccess.Askable {})

        onNodeWithText(cardTitle).assertDoesNotExist()
        onNodeWithText(holdHint).assertExists()
    }

    @Test
    fun theScriptDrawsWithAFingertip() = runComposeUiTest {
        mainClock.autoAdvance = false
        val state = DemoState()
        val fog = FogState()
        var scope: CoroutineScope? = null
        setContent {
            scope = rememberCoroutineScope()
            LabTheme {
                Box(Modifier.size(400.dp, 500.dp)) { FogDemo(state, fog, MicAccess.Unavailable) }
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
        assertEquals(FogDemos.FingertipBrush, stroke.radius)
    }
}
