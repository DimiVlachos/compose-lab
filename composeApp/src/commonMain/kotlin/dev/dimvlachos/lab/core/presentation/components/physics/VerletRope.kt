package dev.dimvlachos.lab.core.presentation.components.physics

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.isSpecified
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sqrt

/**
 * A rope as Verlet points: [points] points [segment] apart, the first pinned where something holds
 * it, as a lamp holds its cord or a rod its line, the rest falling under [gravity]. Each point
 * keeps where it was a step ago as well as where it is, so its speed is the difference: put a point
 * somewhere and it carries on from there. A rope only pulls: a segment longer than its length is
 * drawn back to it, a shorter one is left slack, so the rope crumples when it is thrown up. It
 * loses [damping] of its speed each second, its lengths are put right [passes] times a step, and
 * moving less than [stillStep] a step it is still. Positions are in dp.
 */
internal class VerletRope(
    points: Int,
    private val segment: Float,
    private val gravity: Float,
    private val damping: Float,
    private val passes: Int,
    private val stillStep: Float,
) {
    private val x = FloatArray(points)
    private val y = FloatArray(points)
    private val lastX = FloatArray(points)
    private val lastY = FloatArray(points)

    // Where a finger holds the end, or unspecified while nothing does: a plain value, so stepping
    // a held rope boxes nothing.
    private var held: Offset = Offset.Unspecified

    /** How far each segment is stretched, 1 at its own length. */
    var stretch = 1f

    /** Whether nothing moved more than a hair in the last step. */
    var still = true
        private set

    val size: Int
        get() = x.size

    operator fun get(i: Int): Offset = Offset(x[i], y[i])

    /** Hangs the rope at rest straight down from [top]. */
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

    /**
     * Sets the rope moving sideways at [speed] dp a second at its end, less towards the top where
     * it hangs from, as a breath of air would: steps of [dt] then carry it on.
     */
    fun push(speed: Float, dt: Float) {
        val end = size - 1
        for (i in 1 until size) {
            lastX[i] = x[i] - speed * (i / end.toFloat()) * dt
        }
        still = false
    }

    /** Moves every point by [by], as it is, speed and all: what holds it has been moved. */
    fun shift(by: Offset) {
        for (i in 0 until size) {
            x[i] += by.x
            y[i] += by.y
            lastX[i] += by.x
            lastY[i] += by.y
        }
    }

    fun step(dt: Float) {
        val keep = 1f - damping * dt
        val fall = gravity * dt * dt
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
        repeat(passes) { pass ->
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
        still = moved < stillStep
    }
}
