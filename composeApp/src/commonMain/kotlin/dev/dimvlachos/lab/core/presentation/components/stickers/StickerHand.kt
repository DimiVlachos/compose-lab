package dev.dimvlachos.lab.core.presentation.components.stickers

import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.geometry.Offset
import dev.dimvlachos.lab.core.presentation.components.touch.ScriptedTouch
import kotlin.math.pow

/**
 * A scripted hand on the rating: it moves the [StickerFinger] the rating draws and works the same
 * [StickerGesture] a finger does, so a script lays stars down exactly as a person would. Points are
 * in the rating's CSS pixels; [layout] says where the stars and slots are once the rating is laid
 * out. A real touch calls [onInterrupt].
 */
internal class StickerHand {
    private var gesture: StickerGesture? = null
    private var finger: StickerFinger? = null

    var layout: StickerLayout? = null
        private set

    /** Told when a real finger touches the rating while the hand is at work. */
    var onInterrupt: (() -> Unit)? = null

    private var pressed = false

    internal fun bind(layout: StickerLayout, gesture: StickerGesture, finger: StickerFinger?) {
        this.layout = layout
        this.gesture = gesture
        this.finger = finger
    }

    internal fun interrupt() {
        onInterrupt?.invoke()
    }

    /** Where star [i] sits on the sheet, moved by [dx] and [dy] slots and radii. */
    fun onSheet(i: Int, dx: Float = 0f, dy: Float = 0f): Offset {
        val l = checkNotNull(layout)
        return Offset(l.sheetX(i) + l.spacing * dx, l.sheetY(i) + l.radius * dy)
    }

    /** Where slot [i] is, moved by [dx] slots. */
    fun inSlot(i: Int, dx: Float = 0f): Offset {
        val l = checkNotNull(layout)
        return Offset(l.slotX(i) + l.spacing * dx, l.slotY)
    }

    /** Shows the fingertip at [at], lifted. */
    suspend fun appear(at: Offset) {
        val f = finger ?: return
        f.at = at
        animate(ScriptedTouch.UpMs.toFloat()) { f.alpha = it * .6f }
    }

    /** Puts the fingertip down where it is. */
    suspend fun press() {
        val f = finger
        gesture?.down(f?.at ?: Offset.Zero)
        pressed = true
        if (f != null) animate(ScriptedTouch.DownMs.toFloat()) { f.alpha = .6f + .4f * it }
    }

    /** Lifts the fingertip, letting go of the rating. */
    suspend fun lift() {
        val f = finger
        gesture?.up(f?.at ?: Offset.Zero)
        pressed = false
        if (f != null) animate(ScriptedTouch.DownMs.toFloat()) { f.alpha = 1f - .4f * it }
    }

    /** Taps where the fingertip is. */
    suspend fun tap() {
        press()
        kotlinx.coroutines.delay(ScriptedTouch.TapHoldMs)
        lift()
    }

    /** Glides the fingertip to [to] over [ms], dragging the rating if it is down. */
    suspend fun glide(to: Offset, ms: Float) {
        val f = finger ?: return
        val from = f.at
        animate(ms) { p ->
            val e = if (p < .5f) 2 * p * p else 1 - (-2 * p + 2).pow(2) / 2
            f.at = Offset(from.x + (to.x - from.x) * e, from.y + (to.y - from.y) * e)
            if (pressed) gesture?.move(f.at)
        }
    }

    /** Fades the fingertip out. */
    suspend fun hide() {
        val f = finger ?: return
        if (pressed) lift()
        val a0 = f.alpha
        animate(ScriptedTouch.UpMs.toFloat()) { f.alpha = a0 * (1 - it) }
    }

    /** Lets go at once, when a script is stopped. */
    fun stop() {
        if (pressed) gesture?.cancel()
        pressed = false
        finger?.alpha = 0f
    }

    private suspend fun animate(ms: Float, frame: (Float) -> Unit) {
        val start = withFrameNanos { it }
        var p = 0f
        while (p < 1f) {
            withFrameNanos { now -> p = ((now - start) / 1e6f / ms).coerceIn(0f, 1f) }
            frame(p)
        }
    }
}
