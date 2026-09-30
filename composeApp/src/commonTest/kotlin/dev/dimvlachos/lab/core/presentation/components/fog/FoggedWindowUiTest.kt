package dev.dimvlachos.lab.core.presentation.components.fog

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import dev.dimvlachos.lab.core.presentation.ui.LabTheme
import dev.dimvlachos.lab.resources.Res
import dev.dimvlachos.lab.resources.photo_santorini
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.jetbrains.compose.resources.painterResource

@OptIn(ExperimentalTestApi::class)
class FoggedWindowUiTest {
    @Test
    fun aDragWipesAStrokeInFractionsOfTheWindow() = runComposeUiTest {
        val fog = FogState()
        setContent {
            LabTheme {
                FoggedWindow(
                    photo = painterResource(Res.drawable.photo_santorini),
                    state = fog,
                    modifier = Modifier.size(200.dp).testTag("window"),
                )
            }
        }
        onNodeWithTag("window").performTouchInput { swipe(centerLeft, centerRight) }

        assertEquals(1, fog.strokes.size)
        val stroke = fog.strokes.single()
        assertTrue(stroke.size > 1, "a drag is more than a tap")
        assertTrue(stroke.all { it.x in 0f..1f && it.y in 0.49f..0.51f }, "$stroke")
        assertTrue(stroke.last().x > 0.9f, "the stroke reaches the right edge: $stroke")
    }

    private fun ComposeUiTest.showWindow(
        fog: FogState,
        holds: MutableList<Boolean>,
        shown: () -> Boolean = { true },
    ) {
        setContent {
            LabTheme {
                if (shown()) {
                    FoggedWindow(
                        photo = painterResource(Res.drawable.photo_santorini),
                        state = fog,
                        modifier = Modifier.size(200.dp).testTag("window"),
                        onHoldChange = { holds += it },
                    )
                }
            }
        }
    }

    @Test
    fun aStillHoldBreathesInsteadOfWiping() = runComposeUiTest {
        val fog = FogState()
        val holds = mutableListOf<Boolean>()
        showWindow(fog, holds)

        onNodeWithTag("window").performTouchInput { down(center) }
        mainClock.advanceTimeBy(600)
        onNodeWithTag("window").performTouchInput { up() }
        waitForIdle()

        assertEquals(listOf(true, false), holds)
        assertTrue(fog.strokes.isEmpty())
    }

    @Test
    fun aQuickTapNeitherWipesNorBreathes() = runComposeUiTest {
        val fog = FogState()
        val holds = mutableListOf<Boolean>()
        showWindow(fog, holds)

        onNodeWithTag("window").performTouchInput { down(center) }
        mainClock.advanceTimeBy(100)
        onNodeWithTag("window").performTouchInput { up() }
        waitForIdle()

        assertTrue(holds.isEmpty())
        assertTrue(fog.strokes.isEmpty())
    }

    @Test
    fun aDragStillWipesWhenHoldingIsOn() = runComposeUiTest {
        val fog = FogState()
        val holds = mutableListOf<Boolean>()
        showWindow(fog, holds)

        onNodeWithTag("window").performTouchInput { swipe(centerLeft, centerRight) }

        assertEquals(1, fog.strokes.size)
        assertTrue(holds.isEmpty())
    }

    @Test
    fun leavingMidHoldLetsGo() = runComposeUiTest {
        val fog = FogState()
        val holds = mutableListOf<Boolean>()
        var shown by mutableStateOf(true)
        showWindow(fog, holds, shown = { shown })

        onNodeWithTag("window").performTouchInput { down(center) }
        mainClock.advanceTimeBy(600)
        shown = false
        waitForIdle()

        assertEquals(listOf(true, false), holds)
    }

    @Test
    fun switchingHoldingOffMidHoldStillLetsGo() = runComposeUiTest {
        val fog = FogState()
        val holds = mutableListOf<Boolean>()
        var enabled by mutableStateOf(true)
        setContent {
            LabTheme {
                FoggedWindow(
                    photo = painterResource(Res.drawable.photo_santorini),
                    state = fog,
                    modifier = Modifier.size(200.dp).testTag("window"),
                    onHoldChange = if (enabled) { held -> holds += held } else null,
                )
            }
        }

        onNodeWithTag("window").performTouchInput { down(center) }
        mainClock.advanceTimeBy(600)
        enabled = false
        waitForIdle()

        assertEquals(listOf(true, false), holds)
    }

    @Test
    fun twoFingersWipeTwoStrokesAtOnce() = runComposeUiTest {
        val fog = FogState()
        showWindow(fog, mutableListOf())

        onNodeWithTag("window").performTouchInput {
            down(0, Offset(width * 0.25f, height * 0.3f))
            down(1, Offset(width * 0.75f, height * 0.3f))
            repeat(6) {
                moveBy(0, Offset(0f, height * 0.08f))
                moveBy(1, Offset(0f, height * 0.08f))
            }
            up(0)
            up(1)
        }

        assertEquals(2, fog.strokes.size, "${fog.strokes}")
        val (left, right) = fog.strokes.sortedBy { it.first().x }
        assertTrue(left.all { it.x in 0.2f..0.3f }, "the left finger's own path: $left")
        assertTrue(right.all { it.x in 0.7f..0.8f }, "the right finger's own path: $right")
        assertTrue(left.last().y > 0.7f && right.last().y > 0.7f, "both went down the glass")
    }

    @Test
    fun aFingerJoiningMidWipeStartsItsOwnStroke() = runComposeUiTest {
        val fog = FogState()
        showWindow(fog, mutableListOf())

        onNodeWithTag("window").performTouchInput {
            down(0, Offset(width * 0.2f, height * 0.5f))
            repeat(3) { moveBy(0, Offset(width * 0.1f, 0f)) }
            down(1, Offset(width * 0.5f, height * 0.9f))
            repeat(3) {
                moveBy(0, Offset(width * 0.1f, 0f))
                moveBy(1, Offset(0f, -height * 0.1f))
            }
            up(1)
            up(0)
        }

        assertEquals(2, fog.strokes.size, "${fog.strokes}")
        assertTrue(
            fog.strokes[1].first().y > 0.85f,
            "the second stroke starts where its finger landed",
        )
    }

    @Test
    fun oneFingerHoldsWhileAnotherWipes() = runComposeUiTest {
        val fog = FogState()
        val holds = mutableListOf<Boolean>()
        showWindow(fog, holds)

        onNodeWithTag("window").performTouchInput { down(0, center) }
        mainClock.advanceTimeBy(600)
        onNodeWithTag("window").performTouchInput {
            down(1, Offset(width * 0.2f, height * 0.2f))
            repeat(4) { moveBy(1, Offset(width * 0.1f, 0f)) }
            up(1)
        }
        mainClock.advanceTimeBy(200)
        onNodeWithTag("window").performTouchInput { up(0) }
        waitForIdle()

        assertEquals(listOf(true, false), holds)
        assertEquals(1, fog.strokes.size)
    }
}
