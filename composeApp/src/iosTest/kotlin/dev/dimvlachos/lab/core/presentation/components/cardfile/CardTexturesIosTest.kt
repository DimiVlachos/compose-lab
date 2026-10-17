package dev.dimvlachos.lab.core.presentation.components.cardfile

import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.rememberTextMeasurer
import kotlin.test.Test
import kotlin.test.assertTrue

class CardTexturesIosTest {
    // Skia copies a mutable bitmap into a new image every time it is drawn; a card is drawn in
    // fourteen strips a frame, so a baked card must be immutable to be drawn from its own pixels.
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun bakedCardsAndTheirGrainAreFrozen() = runComposeUiTest {
        var measurer: TextMeasurer? = null
        setContent { measurer = rememberTextMeasurer() }
        waitForIdle()
        val grain = makeGrain()
        val ink = CardInk(checkNotNull(measurer), mapOf("TYPED" to FontFamily.Monospace), grain)
        val card = PaperCard.Contact("Abby Carter", "Friend", listOf("(802) 555-0142"), 31)
        assertTrue(grain.asSkiaBitmap().isImmutable, "the grain")
        assertTrue(bakeFace(card, 100f, 1f, ink).bitmap.asSkiaBitmap().isImmutable, "a face")
        assertTrue(bakeBack(card, 100f, 1f, ink).bitmap.asSkiaBitmap().isImmutable, "a back")
    }
}
