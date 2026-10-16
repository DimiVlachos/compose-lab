package dev.dimvlachos.lab.core.presentation.components.cardfile

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlin.math.roundToInt
import kotlin.time.Duration
import kotlin.time.TimeSource

private val startMark = TimeSource.Monotonic.markNow()

/**
 * A card file's wheel and the card pulled out of it. The wheel's [position] is in cards. A drag
 * moves it under the finger, rubber-banded past the ends; a release springs it to the whole card a
 * fling of that speed would stop on, starting at the fling's speed, so it has momentum and a
 * detent. While a card is [pulled] the wheel stays put.
 */
@Stable
internal class CardFileState(
    initial: Int = 0,
    /** Milliseconds on a monotonic clock; a test passes its virtual time. */
    private val now: () -> Long = { startMark.elapsedNow().inWholeMilliseconds },
) {
    private val wheel = Animatable(initial.toFloat())
    private val out = Animatable(0f)
    private val push = Animatable(0f)
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

    val pullProgress: Float
        get() = out.value

    val pushDown: Float
        get() = push.value

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
            push.snapTo(0f)
            pulled = null
        }
    }

    private val spinnable: Boolean
        get() = pulled == null && count > 1

    private fun tickFrom(before: Float) {
        if (CardWheel.cardsCrossed(before, wheel.value) > 0 && throttle.shouldTick(now())) {
            onTick?.invoke()
        }
    }

    private suspend fun moveTo(target: Float, velocity: Float = 0f) {
        if (reducedMotion) {
            wheel.snapTo(target)
            raw = target
            return
        }
        var last = wheel.value
        wheel.animateTo(
            target,
            spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow),
            velocity,
        ) {
            tickFrom(last)
            last = value
        }
        raw = target
    }

    /** A finger lands: stop any settle or spin and carry on from exactly where the wheel is. */
    suspend fun beginDrag() {
        wheel.stop()
        raw = unband(wheel.value)
    }

    private fun unband(v: Float): Float {
        val last = (count - 1).coerceAtLeast(0).toFloat()
        val k = CardFileDimens.RubberStiffness
        return when {
            v < 0f -> v / (1f + v * k).coerceAtLeast(1e-3f)
            v > last -> last + (v - last) / (1f - (v - last) * k).coerceAtLeast(1e-3f)
            else -> v
        }
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
        moveTo(CardWheel.restingCard(wheel.value, velocity, count).toFloat(), velocity)
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

    /** Pulls the front card out, unless it's a divider or one is already out. */
    suspend fun pull(): Boolean {
        if (pulled != null || count == 0 || !canPull(front)) return false
        val card = front
        wheel.stop()
        wheel.snapTo(card.toFloat())
        raw = card.toFloat()
        pulled = card
        push.snapTo(0f)
        if (reducedMotion) out.snapTo(1f)
        else out.animateTo(1f, spring(dampingRatio = 0.86f, stiffness = Spring.StiffnessLow))
        return true
    }

    /** Files the pulled card back into its slot. */
    suspend fun file() {
        if (pulled == null) return
        if (reducedMotion) out.snapTo(0f)
        else out.animateTo(0f, spring(dampingRatio = 1f, stiffness = Spring.StiffnessMediumLow))
        push.snapTo(0f)
        pulled = null
    }

    /** The pulled card pushed down by [fraction] of its height. */
    suspend fun pushBy(fraction: Float) {
        if (pulled == null) return
        push.snapTo((push.value + fraction).coerceIn(0f, 1f))
    }

    /** Let go of the pushed card: filed if pushed past a third or flicked down, else back out. */
    suspend fun releasePush(velocity: Float) {
        if (pulled == null) return
        if (
            push.value >= CardFileDimens.FileFraction ||
                velocity > CardFileDimens.FlickFilesVelocity
        )
            file()
        else push.animateTo(0f, spring())
    }
}

@Composable internal fun rememberCardFileState(): CardFileState = remember { CardFileState() }
