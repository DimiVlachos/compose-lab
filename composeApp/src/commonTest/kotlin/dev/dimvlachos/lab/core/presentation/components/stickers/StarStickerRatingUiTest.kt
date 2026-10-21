package dev.dimvlachos.lab.core.presentation.components.stickers

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.test.swipe
import androidx.compose.ui.unit.dp
import dev.dimvlachos.lab.core.presentation.components.Recreation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class StarStickerRatingUiTest {
    @Test
    fun aDragAfterTheWidthChangesCountsTheNewRow() = runComposeUiTest {
        val stickers = StarStickerState()
        var width by mutableStateOf(200.dp)
        setContent {
            Box(Modifier.width(width)) {
                StarStickerRating(
                    stickers,
                    label = { "" },
                    valueText = { "$it" },
                    description = "",
                    labelColor = Color.Gray,
                    labelSetColor = Color.Gray,
                    reducedMotion = true,
                )
            }
        }
        // As when the phone turns: the rating is laid out again, wider, without being made anew.
        width = 400.dp
        waitForIdle()
        onNodeWithTag("stickers").performTouchInput {
            swipe(Offset(left + 4f, top + 60f), Offset(centerX, top + 60f), durationMillis = 400)
        }
        waitForIdle()
        assertEquals(3, stickers.target)
    }

    @Test
    fun theScoreComesBackWithItsStarsPressedIn() = runComposeUiTest {
        val screen = Recreation(this)
        var state: StarStickerState? = null
        screen.setContent { state = rememberStarStickerState() }
        val first = state!!
        first.ask(4)
        screen.saveAndRestore()
        val back = state!!
        assertTrue(back !== first, "made again")
        assertEquals(4, back.target)
        assertEquals(4, back.landed)
        assertEquals(1f, back.pos(3))
        assertEquals(0f, back.pos(4))
    }
}
