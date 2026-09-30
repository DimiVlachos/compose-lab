package dev.dimvlachos.lab.core.presentation.components.frost

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
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
class FrostedWindowUiTest {
    @Test
    fun aDragWipesAStrokeInFractionsOfTheWindow() = runComposeUiTest {
        val frost = FrostState()
        setContent {
            LabTheme {
                FrostedWindow(
                    photo = painterResource(Res.drawable.photo_santorini),
                    state = frost,
                    modifier = Modifier.size(200.dp).testTag("window"),
                )
            }
        }
        onNodeWithTag("window").performTouchInput { swipe(centerLeft, centerRight) }

        assertEquals(1, frost.strokes.size)
        val stroke = frost.strokes.single()
        assertTrue(stroke.size > 1, "a drag is more than a tap")
        assertTrue(stroke.all { it.x in 0f..1f && it.y in 0.49f..0.51f }, "$stroke")
        assertTrue(stroke.last().x > 0.9f, "the stroke reaches the right edge: $stroke")
    }

    private fun ComposeUiTest.showWindow(
        frost: FrostState,
        holds: MutableList<Boolean>,
        shown: () -> Boolean = { true },
    ) {
        setContent {
            LabTheme {
                if (shown()) {
                    FrostedWindow(
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
}
