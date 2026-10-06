package dev.dimvlachos.lab.core.presentation.components.pullcord

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.isSpecified
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sqrt

/**
 * A cord as a Verlet rope: [points] points [segment] apart, the first pinned where the lamp holds
 * it, the rest falling under gravity. Each point keeps where it was a step ago as well as where it
 * is, so its speed is the difference: put a point somewhere and it carries on from there. A rope
 * only pulls: a segment longer than its length is drawn back to it, a shorter one is left slack, so
 * the cord crumples when it is thrown up. Positions are in dp.
 */
internal class CordRope(points: Int = PullCordDimens.CordPoints, private val segment: Float) {
    private val x = FloatArray(points)
    private val y = FloatArray(points)
    private val lastX = FloatArray(points)
    private val lastY = FloatArray(points)

    // Where a finger holds the end, or unspecified while nothing does: a plain value, so stepping
    // a held cord boxes nothing.
    private var held: Offset = Offset.Unspecified

    /** How far each segment is stretched, 1 at its own length. */
    var stretch = 1f

    /** Whether nothing moved more than a hair in the last step. */
    var still = true
        private set

    val size: Int
        get() = x.size

    operator fun get(i: Int): Offset = Offset(x[i], y[i])

    /** Hangs the cord at rest straight down from [top]. */
    fun hang(top: Offset) {
        for (i in 0 until size) {
            x[i] = top.x
            y[i] = top.y + i * segment
            lastX[i] = x[i]
            lastY[i] = y[i]
        }
        still = true
    }

    /** Pins the top at [top]; the top carries no speed of its own. */
    fun pin(top: Offset) {
        lastX[0] = x[0]
        lastY[0] = y[0]
        x[0] = top.x
        y[0] = top.y
    }

    /**
     * Holds the end at [end], each segment [stretch] times its length, until [letGo]. The end keeps
     * where it was, so let go it carries on at the finger's speed.
     */
    fun hold(end: Offset, stretch: Float) {
        held = end
        this.stretch = stretch
    }

    /** Lets go of the end: it carries on at the speed it was last moved at. */
    fun letGo() {
        held = Offset.Unspecified
    }

    /** Moves every point by [by], as it is, speed and all: the lamp has been moved. */
    fun shift(by: Offset) {
        for (i in 0 until size) {
            x[i] += by.x
            y[i] += by.y
            lastX[i] += by.x
            lastY[i] += by.y
        }
    }

    fun step(dt: Float) {
        val keep = 1f - PullCordDimens.CordDamping * dt
        val fall = PullCordDimens.Gravity * dt * dt
        var moved = 0f
        val end = size - 1
        for (i in 1 until size) {
            val vx = (x[i] - lastX[i]) * keep
            val vy = (y[i] - lastY[i]) * keep
            lastX[i] = x[i]
            lastY[i] = y[i]
            x[i] += vx
            y[i] += vy + fall
        }
        val holding = held.isSpecified
        if (holding) {
            x[end] = held.x
            y[end] = held.y
        }
        val length = segment * stretch
        repeat(PullCordDimens.ConstraintPasses) { pass ->
            for (k in 1 until size) {
                val i = if (pass % 2 == 0) k else size - k
                val dx = x[i] - x[i - 1]
                val dy = y[i] - y[i - 1]
                val distance = sqrt(dx * dx + dy * dy)
                if (distance <= length || distance == 0f) continue
                val over = (distance - length) / distance
                // The pinned top and a held end don't give; a free point gives half, or all of it
                // when the other end of its segment can't.
                val firstFree = i - 1 > 0
                val secondFree = i < end || !holding
                val first = if (!firstFree) 0f else if (secondFree) 0.5f else 1f
                val second = if (!secondFree) 0f else if (firstFree) 0.5f else 1f
                x[i - 1] += dx * over * first
                y[i - 1] += dy * over * first
                x[i] -= dx * over * second
                y[i] -= dy * over * second
            }
        }
        for (i in 1 until size) {
            moved = max(moved, max(abs(x[i] - lastX[i]), abs(y[i] - lastY[i])))
        }
        still = moved < PullCordDimens.StillStep
    }
}
