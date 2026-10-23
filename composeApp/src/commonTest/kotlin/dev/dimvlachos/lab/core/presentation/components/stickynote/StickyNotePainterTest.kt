package dev.dimvlachos.lab.core.presentation.components.stickynote

import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.rememberTextMeasurer
import dev.dimvlachos.lab.core.presentation.canvas2d.Canvas2D
import kotlin.math.roundToInt
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals

/** What the painter draws, read back from its pixels. */
class StickyNotePainterTest {
    private val words =
        NoteWords(
            "Archive",
            "Snooze",
            "Tomorrow 9 AM",
            "Done",
            "Later",
            "Swipe left",
            "Swipe right",
        )

    @OptIn(ExperimentalTestApi::class)
    private fun withMeasurer(block: (TextMeasurer) -> Unit) = runComposeUiTest {
        var measurer: TextMeasurer? = null
        setContent { measurer = rememberTextMeasurer() }
        waitForIdle()
        block(checkNotNull(measurer))
    }

    // The base layer with the first note pulled by [dx]: the note itself goes on the lift layer,
    // so this is the panel under it. Returns the panel's text end, as a row of pixel values.
    private fun panel(m: TextMeasurer, notes: StickyNoteState, dx: Float): List<Int> {
        val c = Canvas2D.create(notes.width.toInt(), (notes.slotY(1)).roundToInt())
        c.textMeasurer = m
        c.fonts = mapOf(HandFont to FontFamily.Default, SansFont to FontFamily.Default)
        notes.rows[0].dx = dx
        StickyNotePainter(notePaper(), words, 1f).drawBase(c, notes, NoteInks.Day)
        val px = c.bitmap.toPixelMap()
        val y0 = notes.slotY(0).toInt()
        val x0 = if (dx < 0) (notes.mx + notes.noteW - 110).toInt() else notes.mx.toInt()
        return buildList {
            for (y in y0 until y0 + notes.rh.toInt()) {
                for (x in x0 until x0 + 110) add(px[x, y].hashCode())
            }
        }
    }

    @Test
    fun thePanelsWordsSitStillThroughTheWholePull() = withMeasurer { m ->
        val notes = StickyNoteState(List(6) { Reminder("Note $it", "Meta $it", it) }, Random(3))
        notes.layout(412f, 760f, 76f)
        for (sign in listOf(-1f, 1f)) {
            val first = panel(m, notes, sign * 1f)
            // From the first pixel of the pull, past the 42 % where the note arms, to the stop.
            for (pull in listOf(8f, 40f, 120f, .41f * notes.noteW, .43f * notes.noteW, 260f)) {
                assertEquals(first, panel(m, notes, sign * pull), "moved at ${sign * pull}")
            }
        }
    }
}
