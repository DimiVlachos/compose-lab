package dev.dimvlachos.lab.core.presentation.components.stickers

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import dev.dimvlachos.lab.core.presentation.canvas2d.mulberry
import kotlin.math.max
import kotlin.math.min

/** The stars on the sheet, as the prototype has them. */
internal const val StickerCount = 5

/** The fixed step the stickers move in, as the prototype's 120 Hz simulation. */
internal const val StickerStep = 1f / 120f

internal const val D2R = (kotlin.math.PI / 180).toFloat()

/** What a step did to a star, for its sound and its haptic. */
internal sealed interface StickerEvent {
    /** [star] started to peel, off the sheet or out of its slot. */
    data class Peel(val star: Int) : StickerEvent

    /** [star] was pressed down into its slot. */
    data class Press(val star: Int) : StickerEvent

    /** [star] lay back down on the sheet. */
    data class Settle(val star: Int) : StickerEvent
}

/**
 * The rating's stars: one number per star, [pos], from 0 on the sheet to 1 pressed into its slot,
 * drives its peel, flight and press, so a drag can turn back mid-air without a jump. A star moves
 * only once its neighbour is far enough along: up, the one before it past .38; down, the one after
 * it under .62. That one rule lays a long drag down one star after another.
 *
 * [target] is how many the finger asks for; [landed], how many are over their slots (three quarters
 * of the way), is what the label reads, so it never names a score the stars haven't reached.
 * [initial] stars start pressed into their slots, as a saved score comes back.
 */
@Stable
internal class StarStickerState(seed: Int = 7, initial: Int = 0) {
    private val rnd = mulberry(seed)
    private val pos = FloatArray(StickerCount)

    /** Each star's tilt on the sheet, and the tilt it lands at in its slot. */
    val sheetJit = FloatArray(StickerCount)
    val slotRot = FloatArray(StickerCount)

    /** When, in [now]'s milliseconds, each star was last pressed down, for its bounce and glint. */
    val pressAt = FloatArray(StickerCount) { -1e9f }

    /** The simulation's clock, in milliseconds. */
    var now = 0f
        private set

    var target by mutableIntStateOf(0)
        private set

    var landed by mutableIntStateOf(0)
        private set

    /** Bumped on every step that moves anything, for the canvas to redraw on. */
    var frame by mutableIntStateOf(0)
        private set

    init {
        for (i in 0 until StickerCount) {
            sheetJit[i] = ((rnd() - .5) * 12 * D2R).toFloat()
            slotRot[i] = ((rnd() - .5) * 10 * D2R).toFloat()
        }
        target = initial.coerceIn(0, StickerCount)
        for (i in 0 until target) pos[i] = 1f
        landed = target
    }

    fun pos(star: Int): Float = pos[star]

    /** Whether any star is still on its way, or just pressed and still bouncing. */
    val moving: Boolean
        get() =
            (0 until StickerCount).any { pos[it] != if (it < target) 1f else 0f } ||
                pressAt.any { now - it < BounceMs }

    fun ask(n: Int) {
        target = n.coerceIn(0, StickerCount)
    }

    /** Puts every star back on the sheet at once. */
    fun reset() {
        target = 0
        pos.fill(0f)
        landed = 0
        frame++
    }

    /** Moves the stars on by [dt] seconds, telling what they did. */
    fun step(dt: Float, reduced: Boolean): List<StickerEvent> {
        now += dt * 1000f
        val up = if (reduced) 7f else 2.1f
        val down = if (reduced) 7f else 2.5f
        var events: MutableList<StickerEvent>? = null
        fun emit(e: StickerEvent) {
            (events ?: mutableListOf<StickerEvent>().also { events = it }) += e
        }
        for (i in 0 until StickerCount) {
            val prev = pos[i]
            val goal = if (i < target) 1f else 0f
            if (goal > prev) {
                if (i == 0 || pos[i - 1] > .38f) pos[i] = min(1f, prev + up * dt)
            } else if (goal < prev) {
                if (i == StickerCount - 1 || pos[i + 1] < .62f) pos[i] = max(0f, prev - down * dt)
            }
            val p = pos[i]
            if (prev == 0f && p > 0f) {
                slotRot[i] = ((rnd() - .5) * 12 * D2R).toFloat()
                emit(StickerEvent.Peel(i))
            }
            if (prev == 1f && p < 1f) emit(StickerEvent.Peel(i))
            if (prev < 1f && p == 1f) {
                pressAt[i] = now
                emit(StickerEvent.Press(i))
            }
            if (prev > 0f && p == 0f) emit(StickerEvent.Settle(i))
        }
        val down2 = pos.count { it >= .75f }
        if (down2 != landed) landed = down2
        frame++
        return events ?: emptyList()
    }

    internal companion object {
        private const val BounceMs = 1000f

        /** Keeps the score, which comes back with its stars already pressed in. */
        val Saver: Saver<StarStickerState, Int> =
            Saver(save = { it.target }, restore = { StarStickerState(initial = it) })
    }
}

/** A [StarStickerState] whose score survives the screen being made again. */
@Composable
internal fun rememberStarStickerState(): StarStickerState =
    rememberSaveable(saver = StarStickerState.Saver) { StarStickerState() }
