package dev.dimvlachos.lab.core.presentation.components.frost

import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
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
}
