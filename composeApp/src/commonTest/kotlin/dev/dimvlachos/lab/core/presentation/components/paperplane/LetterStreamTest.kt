package dev.dimvlachos.lab.core.presentation.components.paperplane

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class LetterStreamTest {
    @Test
    fun aShortMessageGoesInAtASteadyPace() {
        assertEquals(PaperPlaneDimens.LetterGapMs, letterGap(8))
        assertEquals(0f, letterGap(1))
    }

    @Test
    fun aLongMessageIsInWithinTheStreamsTime() {
        for (count in listOf(10, 40, 300)) {
            val last = (count - 1) * letterGap(count) + PaperPlaneDimens.LetterMs
            assertTrue(last <= PaperPlaneDimens.StreamMs + 0.01f, "$count letters take $last ms")
        }
    }

    // Each text laid out, and the letters found in it.
    private fun lettersOf(vararg texts: String): List<List<Rect>> {
        var found: List<List<Rect>> = emptyList()
        runComposeUiTest {
            setContent {
                val measurer = rememberTextMeasurer()
                found = texts.map { text ->
                    val layout: TextLayoutResult =
                        measurer.measure(text, TextStyle(fontSize = 16.sp))
                    letterBoxes(layout)
                }
            }
            waitForIdle()
        }
        return found
    }

    @Test
    fun aLetterIsWhatTheEyeTakesForOne() {
        val (plain, spaced, accent) = lettersOf("Hi!", "a b  c", "e\u0301")
        assertEquals(3, plain.size)
        // Spaces are left behind.
        assertEquals(3, spaced.size)
        // A letter and the accent drawn over it are one.
        assertEquals(1, accent.size)
        // And no two letters overlap.
        for (letters in listOf(plain, spaced)) {
            letters.zipWithNext().forEach { (a, b) -> assertTrue(a.right <= b.left + 0.5f) }
        }
    }

    @Test
    fun theCharactersOfOneEmojiAreOneLetterHoweverThePlatformBoxesThem() {
        // A family emoji is five characters. Skia gives each the whole emoji's box; Android gives
        // the first the whole box and the rest none.
        val emoji = Rect(10f, 0f, 30f, 20f)
        val a = Rect(0f, 0f, 10f, 20f)
        val b = Rect(30f, 0f, 40f, 20f)
        val skia = listOf(a) + List(5) { emoji } + listOf(b)
        val android = listOf(a, emoji) + List(4) { Rect(30f, 0f, 30f, 20f) } + listOf(b)
        assertEquals(listOf(a, emoji, b), oneEach(skia))
        assertEquals(listOf(a, emoji, b), oneEach(android))
        // Letters that only touch, or sit on another line, stay apart.
        val touching = Rect(10f, 0f, 20f, 20f)
        val below = Rect(0f, 20f, 10f, 40f)
        assertEquals(listOf(a, touching, below), oneEach(listOf(a, touching, below)))
    }
}
