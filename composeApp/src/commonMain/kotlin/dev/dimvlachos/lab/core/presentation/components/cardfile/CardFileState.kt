package dev.dimvlachos.lab.core.presentation.components.cardfile

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sqrt
import kotlin.time.Duration
import kotlin.time.TimeSource

private val startMark = TimeSource.Monotonic.markNow()

/**
 * A card file's wheel and the card pulled out of it, with the prototype's feel. The wheel's
 * [position] is in cards. A drag moves it under the finger, rubber-banded past the ends; a release
 * springs it to the whole card a fling of that speed would stop on, starting at the fling's speed,
 * so it has momentum and a detent. A spring onto an end card comes to rest without swinging past
 * it. While a card is [pulled] the wheel stays put; pushing the card down takes it back toward the
 * rail, and let go far enough in, it files.
 */
@Stable
internal class CardFileState(
    initial: Int = 0,
    /** Milliseconds on a monotonic clock; a test passes its virtual time. */
    private val now: () -> Long = { startMark.elapsedNow().inWholeMilliseconds },
) {
    private val wheel = Animatable(initial.toFloat())
    private val out = Animatable(0f)
    private var raw = initial.toFloat()
    private val throttle = TickThrottle(CardFileDimens.TickGapMs)

    var count by mutableIntStateOf(0)
        private set

    var pulled by mutableStateOf<Int?>(null)
        private set

    val position: Float
        get() = wheel.value

    val front: Int
        get() = if (count == 0) 0 else position.roundToInt().coerceIn(0, count - 1)

    /** How far the pulled card is out: 0 in the rail, 1 facing you. */
    val pullProgress: Float
        get() = out.value

    /** How far down, in dp, a pushed card goes to be all the way back in. */
    var pushTravel = 300f

    var reducedMotion = false
    var canPull: (Int) -> Boolean = { true }
    var onTick: (() -> Unit)? = null

    /**
     * Sets a grown deck size at once, without suspending, so the very first frame already shows the
     * starting card. A shrink waits for [updateCount], which also snaps the wheel back.
     */
    fun growCount(count: Int) {
        if (count > this.count) this.count = count
    }

    /**
     * Sets the deck size; if the deck shrank below the wheel, the wheel snaps back onto its last
     * card.
     */
    suspend fun updateCount(count: Int) {
        this.count = count
        val last = (count - 1).coerceAtLeast(0).toFloat()
        if (wheel.value > last) {
            wheel.snapTo(last)
            raw = last
        }
        if (pulled?.let { it >= count } == true) {
            out.snapTo(0f)
            pulled = null
        }
    }

    private val spinnable: Boolean
        get() = pulled == null && count > 1

    private fun tickFrom(before: Float) {
        // Past an end the wheel only bands; no card crosses there.
        val last = (count - 1).coerceAtLeast(0).toFloat()
        val from = before.coerceIn(0f, last)
        val to = wheel.value.coerceIn(0f, last)
        if (CardWheel.cardsCrossed(from, to) > 0 && throttle.shouldTick(now())) {
            onTick?.invoke()
        }
    }

    /**
     * Springs the wheel to [target], starting at [velocity]. Mid-deck the spring is a little loose,
     * for a detent. Stopping on an end card, as a fling that would carry on past it does, or coming
     * back from past one, it is critically damped and starts no faster than it can stop from, so it
     * never swings past the end card.
     */
    private suspend fun moveTo(target: Float, velocity: Float = 0f) {
        if (reducedMotion) {
            wheel.snapTo(target)
            raw = target
            return
        }
        val last = (count - 1).coerceAtLeast(0).toFloat()
        val position = wheel.value
        val stiffness = CardFileDimens.WheelStiffness
        val firm = target <= 0f || target >= last || position < 0f || position > last
        val start =
            if (!firm) velocity
            else {
                val gap = target - position
                // Only toward the card, and no faster than a critically damped spring stops from
                // without crossing it: its natural frequency times the distance.
                if (gap * velocity <= 0f) 0f
                else velocity.coerceIn(-sqrt(stiffness) * abs(gap), sqrt(stiffness) * abs(gap))
            }
        var previous = wheel.value
        wheel.animateTo(
            target,
            spring(
                dampingRatio = if (firm) 1f else CardFileDimens.WheelDamping,
                stiffness = stiffness,
            ),
            start,
        ) {
            tickFrom(previous)
            previous = value
        }
        raw = target
    }

    /** A finger lands: stop any settle or spin and carry on from exactly where the wheel is. */
    suspend fun beginDrag() {
        wheel.stop()
        raw = CardWheel.unbanded(wheel.value, count)
    }

    /** Whether the wheel is turning on its own: settling, flung, or spun to a card. */
    val isSpinning: Boolean
        get() = wheel.isRunning

    /** A finger lands on a turning wheel: it stops where it is. */
    suspend fun stop() = beginDrag()

    /**
     * Turns [by] cards from the card the wheel is heading for, so a second step pressed mid-spin
     * goes one card further rather than to the same card again.
     */
    suspend fun step(by: Int) {
        if (!spinnable) return
        val from = if (wheel.isRunning) wheel.targetValue.roundToInt() else front
        spinTo(from + by)
    }

    suspend fun dragBy(cards: Float) {
        if (!spinnable) return
        if (wheel.isRunning) beginDrag()
        raw += cards
        val before = wheel.value
        wheel.snapTo(CardWheel.banded(raw, count))
        tickFrom(before)
    }

    suspend fun release(velocity: Float) {
        if (!spinnable) {
            raw = wheel.value
            return
        }
        val v = velocity.coerceIn(-CardFileDimens.MaxFlingVelocity, CardFileDimens.MaxFlingVelocity)
        moveTo(CardWheel.restingCard(wheel.value, v, count).toFloat(), v)
    }

    suspend fun spinTo(index: Int) {
        if (!spinnable) return
        moveTo(index.coerceIn(0, count - 1).toFloat())
    }

    /** A scripted spin of [cards] over [duration], at an even pace. */
    suspend fun spinBy(cards: Int, duration: Duration) {
        if (!spinnable) return
        val target = (front + cards).coerceIn(0, count - 1).toFloat()
        if (reducedMotion) {
            wheel.snapTo(target)
            raw = target
            return
        }
        var last = wheel.value
        wheel.animateTo(
            target,
            tween(duration.inWholeMilliseconds.toInt(), easing = LinearEasing),
        ) {
            tickFrom(last)
            last = value
        }
        raw = target
    }

    /**
     * Brings the wheel to rest after a spin or pull was cut short, as when a script is stopped
     * mid-step: a card caught on its way out or back in comes all the way out, and a wheel caught
     * between cards turns to the nearest one.
     */
    suspend fun settle() {
        if (pulled != null) {
            if (reducedMotion) out.snapTo(1f) else out.animateTo(1f, pullSpring)
        } else if (count > 0) {
            moveTo(front.toFloat())
        }
    }

    /** Pulls the front card out, unless it's a divider or one is already out. */
    suspend fun pull(): Boolean {
        if (pulled != null || count == 0 || !canPull(front)) return false
        val card = front
        wheel.stop()
        wheel.snapTo(card.toFloat())
        raw = card.toFloat()
        pulled = card
        if (reducedMotion) out.snapTo(1f) else out.animateTo(1f, pullSpring)
        return true
    }

    /** Files the pulled card back into its slot, starting at [velocity] (out per second). */
    suspend fun file(velocity: Float = 0f) {
        if (pulled == null) return
        if (reducedMotion) out.snapTo(0f) else out.animateTo(0f, pullSpring, velocity)
        pulled = null
    }

    /** The pulled card pushed [down] dp from where the finger took it: back toward the rail. */
    suspend fun pushTo(down: Float) {
        if (pulled == null) return
        out.snapTo((1f - down.coerceAtLeast(0f) / pushTravel).coerceIn(0f, 1f))
    }

    /**
     * Let go of the pushed card going down at [velocity] dp a second: filed if it was pushed most
     * of the way in or flicked down, else back out.
     */
    suspend fun releasePush(velocity: Float) {
        if (pulled == null) return
        if (out.value < CardFileDimens.FiledBelow || velocity > CardFileDimens.FlickFiles) {
            file(-velocity.coerceAtLeast(0f) / pushTravel)
        } else if (reducedMotion) out.snapTo(1f) else out.animateTo(1f, pullSpring)
    }
}

private val pullSpring =
    spring<Float>(
        dampingRatio = CardFileDimens.PullDamping,
        stiffness = CardFileDimens.PullStiffness,
    )

@Composable internal fun rememberCardFileState(): CardFileState = remember { CardFileState() }
