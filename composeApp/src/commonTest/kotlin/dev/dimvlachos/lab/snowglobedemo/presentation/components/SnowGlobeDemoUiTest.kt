package dev.dimvlachos.lab.snowglobedemo.presentation.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import dev.dimvlachos.lab.core.demo.DemoState
import dev.dimvlachos.lab.core.presentation.components.snowglobe.GlobeScene
import dev.dimvlachos.lab.core.presentation.components.snowglobe.SnowGlobeDimens
import dev.dimvlachos.lab.core.presentation.components.snowglobe.SnowGlobeState
import dev.dimvlachos.lab.core.presentation.ui.LabTheme
import dev.dimvlachos.lab.snowglobedemo.FakeForecastSource
import dev.dimvlachos.lab.snowglobedemo.SnowGlobeDemos
import dev.dimvlachos.lab.snowglobedemo.WeatherBoard
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes

/**
 * How long the test that plays the demo's script through may take, in real time: on a CI runner
 * with few cores, longer than the test runner's own 60 s.
 */
private val SlowSnowGlobeTest: Duration = 3.minutes

@OptIn(ExperimentalTestApi::class)
class SnowGlobeDemoUiTest {
    private val board = WeatherBoard()
    private val globe = SnowGlobeState()

    // The globe's frame loop never idles, so every test steps the clock itself.
    private fun ComposeUiTest.show(
        source: FakeForecastSource = FakeForecastSource(300.milliseconds),
        state: DemoState = DemoState(),
        reducedMotion: Boolean = false,
        script: Boolean = false,
        onScriptEnd: () -> Unit = {},
    ) {
        mainClock.autoAdvance = false
        setContent {
            LabTheme {
                Box(Modifier.size(400.dp, 800.dp)) {
                    SnowGlobeDemo(state, board, source, globe, reducedMotion)
                }
                if (script) {
                    LaunchedEffect(Unit) {
                        SnowGlobeDemos.all.single().script.play(state)
                        onScriptEnd()
                    }
                }
            }
        }
        mainClock.advanceTimeByFrame()
    }

    // Steps the clock a frame at a time until [done], failing after [most] ms with [what].
    private fun ComposeUiTest.advanceUntil(what: String, most: Long = 6_000, done: () -> Boolean) {
        var waited = 0L
        while (!done()) {
            assertTrue(waited < most, "waited $most ms for $what")
            mainClock.advanceTimeByFrame()
            waited += 16
        }
    }

    private fun ComposeUiTest.pressEnterOnTheGlobe() {
        onNodeWithTag("snowglobe").requestFocus()
        onNodeWithTag("snowglobe").performKeyInput { pressKey(Key.Enter) }
    }

    private fun allResting() = globe.glitter.resting.all { it }

    @Test
    fun enterOnTheGlobeRefreshesAndTomorrowBecomesAShower() = runComposeUiTest {
        show()
        pressEnterOnTheGlobe()
        mainClock.advanceTimeBy(8_000)
        assertFalse(board.refreshPending)
        assertEquals(GlobeScene.Shower, board.days[1].scene)
        onNodeWithTag("updated").assertTextEquals("Updated just now")
        onNodeWithTag("live")
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.ContentDescription,
                    listOf("Updated. Tomorrow · Thu 15: Light shower, 22 degrees."),
                )
            )
    }

    @Test
    fun tappingADayChangesTheHeaderAndTheGlobesState() = runComposeUiTest {
        show()
        onNodeWithTag("snowglobe")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Sunny"))
        onNodeWithTag("day-2").performClick()
        mainClock.advanceTimeBy(100)
        onNodeWithTag("day-2").assertIsSelected()
        onNodeWithTag("temperature").assertTextEquals("23°")
        onNodeWithTag("snowglobe")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Meltemi"))
        mainClock.advanceTimeBy(4_000)
        assertFalse(board.refreshPending, "changing day never refreshes")
        onNodeWithTag("updated").assertTextEquals("Updated 3 h ago")
    }

    @Test
    fun theArrowKeysMoveTheDayTabs() = runComposeUiTest {
        show()
        onNodeWithTag("day-1").requestFocus()
        onNodeWithTag("day-1").performKeyInput { pressKey(Key.DirectionRight) }
        mainClock.advanceTimeBy(100)
        onNodeWithTag("day-2").assertIsSelected()
        onNodeWithTag("temperature").assertTextEquals("23°")
        onNodeWithTag("day-2").performKeyInput { pressKey(Key.DirectionRight) }
        mainClock.advanceTimeBy(100)
        onNodeWithTag("day-3").assertIsSelected()
        onNodeWithTag("day-3").performKeyInput { pressKey(Key.DirectionLeft) }
        mainClock.advanceTimeBy(100)
        onNodeWithTag("day-2").assertIsSelected()
        onNodeWithTag("temperature").assertTextEquals("23°")
        assertFalse(board.refreshPending)
    }

    @Test
    fun aSwipeOutsideTheGlobeChangesDayWithoutARefresh() = runComposeUiTest {
        show()
        onNodeWithText("Oia, Santorini").performTouchInput {
            down(center)
            // 100 dp: past one day's 64 dp step and short of two.
            repeat(10) { moveBy(Offset(-10f * density, 0f)) }
            up()
        }
        mainClock.advanceTimeBy(100)
        assertEquals(2, board.selected)
        onNodeWithTag("temperature").assertTextEquals("23°")
        mainClock.advanceTimeBy(4_000)
        assertFalse(board.refreshPending, "changing day never refreshes")
        onNodeWithTag("updated").assertTextEquals("Updated 3 h ago")
    }

    @Test
    fun aSwipeOnTheGlobeStirsItAndLeavesTheDay() = runComposeUiTest {
        show()
        onNodeWithTag("snowglobe").performTouchInput {
            down(center)
            repeat(10) { moveBy(Offset(-10f * density, 0f)) }
            up()
        }
        mainClock.advanceTimeBy(100)
        assertEquals(1, board.selected, "the globe keeps its drags")
    }

    @Test
    fun aFailedRefreshSaysSo() = runComposeUiTest {
        show(FakeForecastSource(300.milliseconds).apply { failNext = true })
        pressEnterOnTheGlobe()
        mainClock.advanceTimeBy(8_000)
        onNodeWithTag("updated").assertTextEquals("Couldn’t update")
        assertEquals(GlobeScene.Sunny, board.days[1].scene)
    }

    @Test
    fun withReducedMotionEnterStillRefreshesAndTheGlitterStaysDown() = runComposeUiTest {
        show(reducedMotion = true)
        pressEnterOnTheGlobe()
        repeat(8_000 / 16) {
            mainClock.advanceTimeByFrame()
            assertTrue(allResting(), "the glitter must stay at rest")
        }
        assertFalse(board.refreshPending)
        assertEquals(GlobeScene.Shower, board.days[1].scene)
        onNodeWithTag("updated").assertTextEquals("Updated just now")
    }

    @Test
    fun theDayCardsShowEachDaysHighAndLowAndTheHintSaysShake() = runComposeUiTest {
        show()
        onNodeWithTag("day-2").assert(hasText("23°"))
        onNodeWithTag("day-2").assert(hasText("18°"))
        onNodeWithTag("day-2").assert(hasText("Fri"))
        onNodeWithTag("hint").assert(hasAnyDescendant(hasText("Shake to refresh")))
        onNodeWithText("TOMORROW · THU 15").assertExists()
        onNodeWithText("H 25° · L 18° · Rain 10%").assertExists()
    }

    @Test
    fun theScriptRefreshesLooksAtFridayAndEndsOnTomorrowAtRest() =
        runComposeUiTest(testTimeout = SlowSnowGlobeTest) {
            var ended = false
            show(script = true, state = DemoState(replay = true), onScriptEnd = { ended = true })
            advanceUntil("the scripted shake to refresh the forecast", most = 6_000) {
                board.days[1].scene == GlobeScene.Shower
            }
            advanceUntil("the script to look at Friday", most = 8_000) { board.selected == 2 }
            advanceUntil("the script to end", most = 10_000) { ended }
            assertEquals(1, board.selected, "the clip ends back on tomorrow")
            assertEquals(GlobeScene.Sunny, board.days[1].scene, "the forecast is put back")
            assertTrue(
                globe.energy < SnowGlobeDimens.SettleThreshold,
                "the globe has calmed: ${globe.energy}",
            )
            assertTrue(allResting(), "the glitter is at rest as it began")
        }
}
