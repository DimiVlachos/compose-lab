package dev.dimvlachos.lab.core.demo

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned

/**
 * A layer over the whole demo screen, its bar included, for what a demo throws past its own edges:
 * the stage clips to itself under the bar. It never takes a touch.
 *
 * A demo hands it a drawing with [show], in its own coordinates from [from], and takes it back with
 * [clear]. Drawing reads state as any canvas does, so it redraws when that state changes.
 */
@Stable
internal class DemoOverlay {
    private var drawing by mutableStateOf<Drawing?>(null)
    private var coordinates: LayoutCoordinates? = null

    private class Drawing(val from: () -> LayoutCoordinates?, val draw: DrawScope.() -> Unit)

    /** Draws [draw] over the screen, placed as if it were drawn in [from]'s coordinates. */
    fun show(from: () -> LayoutCoordinates?, draw: DrawScope.() -> Unit) {
        drawing = Drawing(from, draw)
    }

    /** Takes the drawing away. */
    fun clear() {
        drawing = null
    }

    @Composable
    internal fun Layer(modifier: Modifier = Modifier) {
        Canvas(modifier.fillMaxSize().onGloballyPositioned { coordinates = it }) {
            val d = drawing ?: return@Canvas
            val here = coordinates?.takeIf { it.isAttached } ?: return@Canvas
            val there = d.from()?.takeIf { it.isAttached } ?: return@Canvas
            val at = here.localPositionOf(there, Offset.Zero)
            translate(at.x, at.y) { d.draw(this) }
        }
    }
}

/** The demo screen's [DemoOverlay], or null where a demo is shown without one, as in its tests. */
internal val LocalDemoOverlay = staticCompositionLocalOf<DemoOverlay?> { null }
