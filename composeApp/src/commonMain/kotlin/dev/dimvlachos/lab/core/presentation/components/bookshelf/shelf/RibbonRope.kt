package dev.dimvlachos.lab.core.presentation.components.bookshelf.shelf

import dev.dimvlachos.lab.core.presentation.components.bookshelf.geometry.Vec3
import kotlin.math.abs
import kotlin.math.sqrt

private const val POINTS = 6
private const val GRAVITY = 3_200f
private const val DAMPING = 9f
private const val PASSES = 12
private const val STEP_SECONDS = 1f / 120f
private const val MOST_FRAME_SECONDS = 0.05f
private const val JUMP = 60f
private const val STIR_SPEED = 36f
private const val STIFFNESS = 0.85f
private const val REHANG_STRETCH = 1.5f
private const val REST_MOTION = 0.02f

internal enum class RopeSpace {
    Screen,
    Book,
}

internal class RibbonRope {
    private val x = FloatArray(POINTS)
    private val y = FloatArray(POINTS)
    private val z = FloatArray(POINTS)
    private val lastX = FloatArray(POINTS)
    private val lastY = FloatArray(POINTS)
    private val lastZ = FloatArray(POINTS)
    private var segment = 0f
    private var pin: Vec3? = null
    private var space: RopeSpace? = null
    private var lastTime = -1L
    private var carried = 0f
    private var quiet = false
    private var motion = 0f
    private var touched = false

    val size: Int
        get() = POINTS

    operator fun get(i: Int): Vec3 = Vec3(x[i], y[i], z[i])

    fun takeMoving(): Boolean {
        val moving = touched && motion > REST_MOTION
        touched = false
        return moving
    }

    fun follow(top: Vec3, length: Float, timeMillis: Long, space: RopeSpace) {
        touched = true
        val held = pin
        if (held == null || needsHang(top, held, length, space)) {
            hang(top, length)
            this.space = space
            lastTime = timeMillis
            motion = if (quiet) 0f else Float.MAX_VALUE
            if (!quiet) push(STIR_SPEED)
            quiet = false
            return
        }
        segment = length / (POINTS - 1)
        carried += ((timeMillis - lastTime) / 1000f).coerceIn(0f, MOST_FRAME_SECONDS)
        lastTime = timeMillis
        var steps = 0
        while (carried >= STEP_SECONDS) {
            carried -= STEP_SECONDS
            steps++
        }
        if (steps == 0) {
            if (top != held) motion = Float.MAX_VALUE
            shift(top.x - held.x, top.y - held.y, top.z - held.z)
        } else {
            for (k in 1..steps) {
                val f = k / steps.toFloat()
                step(
                    held.x + (top.x - held.x) * f,
                    held.y + (top.y - held.y) * f,
                    held.z + (top.z - held.z) * f,
                )
            }
        }
        pin = top
    }

    fun calm() {
        pin = null
        quiet = true
        motion = 0f
    }

    private fun needsHang(top: Vec3, held: Vec3, length: Float, space: RopeSpace): Boolean {
        val moved = space != this.space || distance(top, held) > JUMP
        return moved || lastTime < 0L || length / (POINTS - 1) > segment * REHANG_STRETCH
    }

    private fun hang(top: Vec3, length: Float) {
        segment = length / (POINTS - 1)
        for (i in 0 until POINTS) {
            x[i] = top.x
            y[i] = top.y + i * segment
            z[i] = top.z
            lastX[i] = x[i]
            lastY[i] = y[i]
            lastZ[i] = z[i]
        }
        pin = top
        carried = 0f
    }

    private fun push(speed: Float) {
        val end = POINTS - 1
        for (i in 1 until POINTS) lastX[i] = x[i] - speed * (i / end.toFloat()) * STEP_SECONDS
    }

    private fun shift(dx: Float, dy: Float, dz: Float) {
        for (i in 0 until POINTS) {
            x[i] += dx
            y[i] += dy
            z[i] += dz
            lastX[i] += dx
            lastY[i] += dy
            lastZ[i] += dz
        }
    }

    private fun step(topX: Float, topY: Float, topZ: Float) {
        x[0] = topX
        y[0] = topY
        z[0] = topZ
        lastX[0] = topX
        lastY[0] = topY
        lastZ[0] = topZ
        val keep = 1f - DAMPING * STEP_SECONDS
        val fall = GRAVITY * STEP_SECONDS * STEP_SECONDS
        for (i in 1 until POINTS) {
            val vx = (x[i] - lastX[i]) * keep
            val vy = (y[i] - lastY[i]) * keep
            val vz = (z[i] - lastZ[i]) * keep
            lastX[i] = x[i]
            lastY[i] = y[i]
            lastZ[i] = z[i]
            x[i] += vx
            y[i] += vy + fall
            z[i] += vz
        }
        repeat(PASSES) { pass ->
            for (k in 1 until POINTS) {
                val i = if (pass % 2 == 0) k else POINTS - k
                keepApart(i - 1, i, segment, stretch = true)
            }
            for (i in 2 until POINTS) keepApart(i - 2, i, segment * 2f * STIFFNESS, stretch = false)
        }
        motion = 0f
        for (i in 1 until POINTS) {
            val moved = abs(x[i] - lastX[i]) + abs(y[i] - lastY[i]) + abs(z[i] - lastZ[i])
            if (moved > motion) motion = moved
        }
    }

    private fun keepApart(a: Int, b: Int, length: Float, stretch: Boolean) {
        val dx = x[b] - x[a]
        val dy = y[b] - y[a]
        val dz = z[b] - z[a]
        val distance = sqrt(dx * dx + dy * dy + dz * dz)
        val satisfied = if (stretch) distance <= length else distance >= length
        if (distance == 0f || satisfied) return
        val off = (distance - length) / distance
        val first = if (a > 0) 0.5f else 0f
        val second = if (a > 0) 0.5f else 1f
        x[a] += dx * off * first
        y[a] += dy * off * first
        z[a] += dz * off * first
        x[b] -= dx * off * second
        y[b] -= dy * off * second
        z[b] -= dz * off * second
    }
}

private fun distance(a: Vec3, b: Vec3): Float {
    val dx = a.x - b.x
    val dy = a.y - b.y
    val dz = a.z - b.z
    return sqrt(dx * dx + dy * dy + dz * dz)
}
