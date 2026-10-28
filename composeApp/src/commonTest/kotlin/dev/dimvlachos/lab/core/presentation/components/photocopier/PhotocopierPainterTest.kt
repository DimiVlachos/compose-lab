package dev.dimvlachos.lab.core.presentation.components.photocopier

import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.rememberTextMeasurer
import dev.dimvlachos.lab.core.presentation.canvas2d.Canvas2D
import dev.dimvlachos.lab.core.presentation.ui.PhotocopierScreenColors
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class PhotocopierPainterTest {
    private val lists = listOf(CopierItem(1, "Weekly meal plan", "Mon tacos", "12 items · Sunday"))

    private fun withMeasurer(block: (TextMeasurer) -> Unit) = runComposeUiTest {
        var measurer: TextMeasurer? = null
        setContent { measurer = rememberTextMeasurer() }
        waitForIdle()
        block(checkNotNull(measurer))
    }

    private fun paint(s: PhotocopierState, m: TextMeasurer, dark: Boolean = false): Canvas2D {
        val c = Canvas2D.create(412, 820)
        c.textMeasurer = m
        val colors = if (dark) PhotocopierScreenColors.Dark else PhotocopierScreenColors.Light
        PhotocopierPainter().draw(c, s, colors, dark, 1f)
        return c
    }

    @Test
    fun aHiddenCopierDrawsNothing() = withMeasurer { m ->
        val s = PhotocopierState(lists).apply { layout(412f, 820f, 133f, 687f) }
        val px = paint(s, m).bitmap.toPixelMap()
        for (y in 0 until 820 step 20) assertEquals(0f, px[206, y].alpha, "at $y")
    }

    @Test
    fun theRaisedCopierFillsTheFootAndLeavesTheListClear() = withMeasurer { m ->
        val s = PhotocopierState(lists).apply { layout(412f, 820f, 133f, 687f) }
        s.duplicate(1)
        repeat(60) { s.step(1f / 60f) }
        val px = paint(s, m).bitmap.toPixelMap()
        val by = s.by.toInt()
        assertTrue(by in 400..600, "the copier is up: $by")
        assertEquals(0f, px[206, 200].alpha, "the list is clear")
        assertTrue(px[20, by + 40].alpha > .99f, "the body is solid")
        // The putty body, a warm light grey by day.
        val body = px[20, by + 40]
        assertTrue(body.red > .75f && body.red >= body.blue, "putty: $body")
    }

    @Test
    fun tonerFlecksAreBakedOncePerTheme() {
        val p = PhotocopierPainter()
        val a = p.speck(false, 2f)
        assertTrue(a === p.speck(false, 2f))
        assertTrue(a !== p.speck(true, 2f))
        assertEquals(720, a.width)
        assertEquals(144, a.height)
        val px = a.toPixelMap()
        var inked = 0
        for (y in 0 until 144 step 2) for (x in 0 until 720 step 2) if (px[x, y].alpha > .1f)
            inked++
        assertTrue(inked in 50..4000, "a few specks: $inked")
    }
}
