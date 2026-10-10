package dev.dimvlachos.lab.popupdemo.presentation.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.util.lerp
import dev.dimvlachos.lab.core.presentation.components.popup.PopUpBookState
import dev.dimvlachos.lab.core.presentation.components.popup.Vec3
import dev.dimvlachos.lab.core.presentation.components.touch.ScriptedTouch
import dev.dimvlachos.lab.core.presentation.components.touch.drawTouch
import kotlin.time.Duration
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext

// Where a scripted tap lands on the book: well out on the near or the far page.
private const val TapAcross = 120f

// How long a pulled tab is held out before the finger lets go.
private const val TabHoldMs = 300L

/**
 * A fingertip drawn over the pop-up book while the script touches it. It drives the book through
 * the same calls a finger makes, so the clip shows what a hand did.
 */
@Stable
internal class PopUpFinger {
    private var at by mutableStateOf(Offset.Zero)
    private val alpha = Animatable(0f)
    private var touches = 0

    /** Taps the near half of the book to turn forward, or the far half to turn back. */
    suspend fun tap(book: PopUpBookState, forward: Boolean) {
        val camera = book.layout?.camera ?: return
        val touch = ++touches
        at = camera.project(Vec3(0f, if (forward) -TapAcross else TapAcross, 0f))
        var held = false
        try {
            fade(1f, ScriptedTouch.DownMs)
            held = book.dragStart(at)
            delay(ScriptedTouch.TapHoldMs)
            if (held) book.dragEnd(Velocity.Zero)
            held = false
            fade(0f, ScriptedTouch.UpMs)
        } finally {
            if (held) book.dragEnd(Velocity.Zero)
            lift(touch)
        }
    }

    /**
     * Takes the page at [from] (fractions of the book), drags it to [to] over [duration], lets go.
     */
    suspend fun drag(book: PopUpBookState, from: Offset, to: Offset, duration: Duration) {
        val camera = book.layout?.camera ?: return
        val touch = ++touches
        val start = Offset(from.x * camera.width, from.y * camera.height)
        val end = Offset(to.x * camera.width, to.y * camera.height)
        at = start
        var held = false
        try {
            fade(1f, ScriptedTouch.DownMs)
            held = book.dragStart(start)
            if (!held) return
            animate(
                0f,
                1f,
                animationSpec =
                    tween(duration.inWholeMilliseconds.toInt(), easing = FastOutSlowInEasing),
            ) { fraction, _ ->
                at = Offset(lerp(start.x, end.x, fraction), lerp(start.y, end.y, fraction))
                book.dragTo(at)
            }
            book.dragEnd(Velocity.Zero)
            held = false
            fade(0f, ScriptedTouch.UpMs)
        } finally {
            if (held) book.dragEnd(Velocity.Zero)
            lift(touch)
        }
    }

    /** Pulls the open spread's tab [out] book units over [duration], holds it and lets go. */
    suspend fun pullTab(book: PopUpBookState, out: Float, duration: Duration) {
        val layout = book.layout ?: return
        val start = layout.tabPoint(book.spread - 1) ?: return
        val touch = ++touches
        at = start
        var held = false
        try {
            fade(1f, ScriptedTouch.DownMs)
            held = book.tabStart(start)
            if (!held) return
            val reach = out * layout.camera.pxPerUnit
            animate(
                0f,
                1f,
                animationSpec =
                    tween(duration.inWholeMilliseconds.toInt(), easing = FastOutSlowInEasing),
            ) { fraction, _ ->
                at = Offset(start.x + reach * fraction, start.y)
                book.tabTo(at)
            }
            delay(TabHoldMs)
            book.tabEnd()
            held = false
            fade(0f, ScriptedTouch.UpMs)
        } finally {
            if (held) book.tabEnd()
            lift(touch)
        }
    }

    // One dot shows every touch, so the next touch's fade cuts this one's short: that is not a
    // reason to stop this touch, only a cancelled script is.
    private suspend fun fade(to: Float, ms: Int) {
        try {
            alpha.animateTo(to, tween(ms))
        } catch (e: CancellationException) {
            if (!currentCoroutineContext().isActive) throw e
        }
    }

    // Gone at once when a script is stopped; only the latest touch takes the dot away.
    private suspend fun lift(touch: Int) {
        if (touch != touches) return
        withContext(NonCancellable) { alpha.snapTo(0f) }
    }

    fun Modifier.drawFinger(color: Color): Modifier = drawWithContent {
        drawContent()
        drawTouch(color, at, alpha.value)
    }
}
