package dev.dimvlachos.lab.core.presentation.components.bookshelf.shelf

import dev.dimvlachos.lab.core.presentation.components.bookshelf.geometry.Vec3
import dev.dimvlachos.lab.core.presentation.components.bookshelf.geometry.smoothStep
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.sqrt

private const val POINTS = 40
private const val GRAVITY = 2_400f
private const val DAMPING = 5f
private const val PASSES = 14
private const val STEP_SECONDS = 1f / 120f
private const val MOST_FRAME_SECONDS = 0.05f
private const val STIFFNESS = 0.9f
private const val CURVE_SAMPLES = 64
private const val GRAB_BLEND = 0.15f
private const val SETTLE_FROM = 0.3f
private const val SETTLE_PULL = 0.3f
private const val LIFT_OUT = 0.3f
private const val LIFT_HIGH = 0.18f
private const val OVER_TOP = 0.08f
private const val OVER_BACK = 0.12f
private const val PAGE_GAP = 0.5f
private const val HAND_SAMPLES = 24
private const val EDGE_BAND = 0.06f
private const val EDGE_KEEP = 0.55f
private const val SIDE_MARGIN = 0.25f
private val SHADOW_EASE = 1f - exp(-STEP_SECONDS / 0.18f)
private const val HEAD_ABOVE = 0.01f
private const val BEHIND_LOW = 1.05f
private const val SNAP_STOW = 0.2f
private const val TIP_CURL = 0.12f
private const val TIP_LIFT = 0.03f
private const val CORD_GRAVITY = 3_200f
private const val CORD_DAMPING = 2.2f
private const val CORD_PASSES = 24
private const val HOLD_DOWN = 0.12f
private const val HOLD_SIDE = 0.07f
private const val LAND_DAMPING = 16f
private const val EASE_FROM = 0.5f
private const val GUTTER_GUIDE = 0.02f
private const val FIRM_FROM_SECONDS = 0.5f
private const val FIRM_TO_SECONDS = 1.4f
private const val APPEAR_SECONDS = 0.18f
private const val REST_MOTION = 0.02f

internal class RibbonFrame(
    val width: Float,
    val height: Float,
    val tail: Float,
    val depth: Float,
    val unit: Float = 1f,
) {
    val head: Vec3
        get() = Vec3(RIBBON_TOP_X * width, -HEAD_ABOVE * height, depth / 2f)

    fun restPath(): List<Vec3> {
        val curve =
            List(CURVE_SAMPLES + 1) { i ->
                val t = i / CURVE_SAMPLES.toFloat()
                val u = 1f - t
                val x =
                    u * u * u * RIBBON_TOP_X +
                        3f * u * u * t * RIBBON_C1_X +
                        3f * u * t * t * RIBBON_C2_X +
                        t * t * t * RIBBON_FOOT_X
                val y = 3f * u * u * t * RIBBON_C1_Y + 3f * u * t * t * RIBBON_C2_Y + t * t * t
                Vec3(x * width, y * height, 0f)
            }
        return listOf(head) + curve + Vec3(RIBBON_FOOT_X * width, height + tail, 0f)
    }

    fun handPath(u: Float, rest: Vec3): Vec3 {
        val keys =
            listOf(
                rest,
                Vec3(0.16f * width, 0.45f * height, -LIFT_OUT * width),
                Vec3(0.1f * width, -OVER_TOP * height, -LIFT_HIGH * width),
                Vec3(0.07f * width, -OVER_TOP * height, depth + OVER_BACK * width),
                Vec3(0.08f * width, BEHIND_LOW * height, depth + OVER_BACK * width),
            )
        val count = (keys.size - 1) * HAND_SAMPLES
        val samples = List(count + 1) { splineAt(keys, it / count.toFloat()) }
        val lengths = FloatArray(count + 1)
        for (i in 1..count) lengths[i] = lengths[i - 1] + (samples[i] - samples[i - 1]).length()
        val target = u.coerceIn(0f, 1f) * lengths[count]
        var i = 1
        while (i < count && lengths[i] < target) i++
        val span = lengths[i] - lengths[i - 1]
        val t = if (span == 0f) 0f else (target - lengths[i - 1]) / span
        return samples[i - 1] + (samples[i] - samples[i - 1]) * t
    }

    fun inside(p: Vec3): Boolean = p.x in 0f..width && p.y in 0f..height
}

internal class RibbonCord {
    private val x = FloatArray(POINTS)
    private val y = FloatArray(POINTS)
    private val z = FloatArray(POINTS)
    private val lastX = FloatArray(POINTS)
    private val lastY = FloatArray(POINTS)
    private val lastZ = FloatArray(POINTS)
    private val behind = BooleanArray(POINTS)
    private val shade = FloatArray(POINTS)
    private var rest: List<Vec3> = emptyList()
    private var segment = 0f
    private var lastTime = -1L
    private var carried = 0f
    private var lastTuck = 0f
    private var grab: Vec3? = null
    private var grabTuck = 0f
    private var stowing = true
    private var laidWidth = 0f
    private var laidHeight = 0f
    private var released = false
    private var releasedFor = 0f
    private var motion = 0f
    private var touched = false

    val size: Int
        get() = POINTS

    val appear: Float
        get() = if (released) smoothStep(0f, APPEAR_SECONDS, releasedFor) else 1f

    val isLaid: Boolean
        get() = rest.isNotEmpty()

    operator fun get(i: Int): Vec3 = Vec3(x[i], y[i], z[i])

    fun isBehind(i: Int): Boolean = behind[i]

    fun shadowLift(i: Int): Float = shade[i]

    fun lay(frame: RibbonFrame) {
        laidWidth = frame.width
        laidHeight = frame.height
        rest = resample(frame.restPath())
        segment = arcLength(rest) / (POINTS - 1)
        for (i in 0 until POINTS) {
            val p = rest[i]
            x[i] = p.x
            y[i] = p.y
            z[i] = p.z
            lastX[i] = p.x
            lastY[i] = p.y
            lastZ[i] = p.z
            behind[i] = false
            shade[i] = 0f
        }
        lastTuck = 0f
        grab = null
        stowing = true
        carried = 0f
        lastTime = -1L
    }

    fun stow(frame: RibbonFrame) {
        if (rest.isEmpty() || frame.width != laidWidth || frame.height != laidHeight) lay(frame)
        layPose(frame, 0f)
        lastTuck = 1f
        grab = null
        stowing = true
        carried = 0f
    }

    fun takeMoving(): Boolean {
        val moving =
            touched && (motion > REST_MOTION || (released && releasedFor < FIRM_TO_SECONDS))
        touched = false
        return moving
    }

    fun update(frame: RibbonFrame, tuck: Float, timeMillis: Long) {
        touched = true
        if (rest.isEmpty() || frame.width != laidWidth || frame.height != laidHeight) lay(frame)
        if (lastTime < 0L) lastTime = timeMillis
        if (tuck >= 1f && 1f - lastTuck > SNAP_STOW) stow(frame)
        val heading =
            when {
                tuck <= 0f -> false
                tuck > lastTuck -> true
                tuck < lastTuck -> false
                else -> stowing
            }
        if (heading) {
            released = false
        } else if (stowing && lastTuck >= 1f && tuck < 1f) {
            drop(frame)
        }
        if (heading != stowing || grab == null) {
            grab = this[POINTS - 1]
            grabTuck = tuck
        }
        stowing = heading
        lastTuck = tuck
        carried += ((timeMillis - lastTime) / 1000f).coerceIn(0f, MOST_FRAME_SECONDS)
        lastTime = timeMillis
        val hand =
            when {
                released -> null
                tuck > 0f -> handAt(frame, tuck)
                else -> rest.last()
            }
        val settle = if (stowing) 0f else (1f - tuck / SETTLE_FROM).coerceIn(0f, 1f)
        while (carried >= STEP_SECONDS) {
            carried -= STEP_SECONDS
            val pull =
                if (released) {
                    releasedFor += STEP_SECONDS
                    SETTLE_PULL * smoothStep(FIRM_FROM_SECONDS, FIRM_TO_SECONDS, releasedFor)
                } else {
                    settle * settle * SETTLE_PULL
                }
            if (released) dropStep(frame, pull) else step(frame, hand, pull)
        }
    }

    private fun handAt(frame: RibbonFrame, tuck: Float): Vec3? {
        if (tuck >= 1f) return null
        val target = frame.handPath(tuck, rest.last())
        val from = grab ?: return target
        val blend = smoothStep(0f, GRAB_BLEND, abs(tuck - grabTuck))
        return from + (target - from) * blend
    }

    private fun step(frame: RibbonFrame, hand: Vec3?, settle: Float) {
        val keep = 1f - DAMPING * STEP_SECONDS
        val fall = GRAVITY * STEP_SECONDS * STEP_SECONDS
        val edge = EDGE_BAND * frame.height
        for (i in 1 until POINTS) {
            val drag = if (abs(y[i]) < edge) keep * EDGE_KEEP else keep
            val vx = (x[i] - lastX[i]) * drag
            val vy = (y[i] - lastY[i]) * drag
            val vz = (z[i] - lastZ[i]) * drag
            lastX[i] = x[i]
            lastY[i] = y[i]
            lastZ[i] = z[i]
            x[i] += vx
            y[i] += vy + fall
            z[i] += vz
        }
        repeat(PASSES) { pass ->
            pin(0, frame.head)
            if (hand != null) pin(POINTS - 1, hand)
            for (k in 1 until POINTS) {
                val i = if (pass % 2 == 0) k else POINTS - k
                link(frame, i - 1, i, hand != null)
            }
            for (i in 2 until POINTS) {
                keepApart(i - 2, i, segment * 2f * STIFFNESS, stretch = false, last = false)
            }
            if (settle > 0f) pull(frame, settle / PASSES)
            collide(frame)
        }
        if (hand != null) pin(POINTS - 1, hand)
        pin(0, frame.head)
        for (i in 0 until POINTS) shade[i] += ((-z[i]).coerceAtLeast(0f) - shade[i]) * SHADOW_EASE
        measureMotion()
    }

    private fun measureMotion() {
        motion = 0f
        for (i in 1 until POINTS) {
            val moved = abs(x[i] - lastX[i]) + abs(y[i] - lastY[i]) + abs(z[i] - lastZ[i])
            if (moved > motion) motion = moved
        }
    }

    private fun drop(frame: RibbonFrame) {
        val head = topEdge(frame)
        val hand =
            Vec3(
                rest[0].x + HOLD_SIDE * frame.width,
                HOLD_DOWN * frame.height,
                0f,
            )
        for (i in 1 until POINTS) {
            val f = i / (POINTS - 1f)
            pin(i, Vec3(head.x + (hand.x - head.x) * f, head.y + (hand.y - head.y) * f, 0f))
            behind[i] = false
            shade[i] = 0f
        }
        pin(0, head)
        behind[0] = false
        released = true
        releasedFor = 0f
        stowing = false
        grab = null
    }

    private fun topEdge(frame: RibbonFrame): Vec3 = Vec3(frame.head.x, 0f, 0f)

    private fun dropStep(frame: RibbonFrame, settle: Float) {
        val keep = 1f - fallDamping(frame) * STEP_SECONDS
        val fall = CORD_GRAVITY * STEP_SECONDS * STEP_SECONDS
        for (i in 1 until POINTS) {
            val vx = (x[i] - lastX[i]) * keep
            val vy = (y[i] - lastY[i]) * keep
            lastX[i] = x[i]
            lastY[i] = y[i]
            lastZ[i] = z[i]
            x[i] += vx + (rest[i].x - x[i]) * GUTTER_GUIDE
            y[i] += vy + fall
            z[i] = 0f
        }
        pin(0, topEdge(frame))
        repeat(CORD_PASSES) { pass ->
            for (k in 1 until POINTS) {
                val i = if (pass % 2 == 0) k else POINTS - k
                keepApart(i - 1, i, dropSegment(frame), stretch = true, last = false)
            }
        }
        if (settle > 0f) pull(frame, settle)
        holdLengths(dropSegment(frame))
        for (i in 0 until POINTS) {
            behind[i] = false
            shade[i] = 0f
        }
        measureMotion()
    }

    private fun fallDamping(frame: RibbonFrame): Float {
        val top = frame.head.y
        val reach = (y[POINTS - 1] - top) / (rest.last().y - top).coerceAtLeast(1f)
        return CORD_DAMPING + (LAND_DAMPING - CORD_DAMPING) * smoothStep(EASE_FROM, 1f, reach)
    }

    private fun dropSegment(frame: RibbonFrame): Float = segment + frame.head.y / (POINTS - 1)

    private fun holdLengths(length: Float) {
        z[0] = 0f
        for (i in 1 until POINTS) {
            val dx = x[i] - x[i - 1]
            val dy = y[i] - y[i - 1]
            val dz = z[i] - z[i - 1]
            val distance = sqrt(dx * dx + dy * dy + dz * dz)
            if (distance > length) {
                val k = length / distance
                x[i] = x[i - 1] + dx * k
                y[i] = y[i - 1] + dy * k
                z[i] = z[i - 1] + dz * k
            }
        }
    }

    private fun layPose(frame: RibbonFrame, progress: Float) {
        val total = segment * (POINTS - 1)
        val front = progress.coerceIn(0f, 1f) * total
        val hidden = total - front
        val curl = TIP_CURL * frame.height
        val lift = TIP_LIFT * frame.width * (1f - progress).coerceIn(0f, 1f)
        val back = frame.depth + PAGE_GAP
        val head = frame.head
        for (i in 0 until POINTS) {
            val arc = front - (POINTS - 1 - i) * segment
            if (arc > -segment) {
                val p = restAt(arc.coerceAtLeast(0f))
                val tip = smoothStep(0f, curl, curl - (front - arc))
                pin(i, Vec3(p.x, p.y, -lift * tip))
                behind[i] = false
                shade[i] = lift * tip
            } else {
                val d = -arc
                val y = head.y + if (d < hidden / 2f) d else hidden - d
                pin(i, Vec3(head.x, y, back))
                behind[i] = true
                shade[i] = 0f
            }
        }
        pin(0, head)
        behind[0] = behind[1]
    }

    private fun restAt(arc: Float): Vec3 {
        val f = (arc / segment).coerceIn(0f, (POINTS - 1).toFloat())
        val i = f.toInt().coerceAtMost(POINTS - 2)
        return rest[i] + (rest[i + 1] - rest[i]) * (f - i)
    }

    private fun pin(i: Int, p: Vec3) {
        x[i] = p.x
        y[i] = p.y
        z[i] = p.z
        lastX[i] = p.x
        lastY[i] = p.y
        lastZ[i] = p.z
    }

    private fun pull(frame: RibbonFrame, amount: Float) {
        for (i in 1 until POINTS) {
            if (behind[i] && y[i] < EDGE_BAND * frame.height) {
                behind[i] = false
                place(i, Vec3(x[i], -PAGE_GAP, -PAGE_GAP))
            }
            val p = rest[i]
            x[i] += (p.x - x[i]) * amount
            y[i] += (p.y - y[i]) * amount
            z[i] += (p.z - z[i]) * amount
        }
    }

    private fun collide(frame: RibbonFrame) {
        for (i in 1 until POINTS) {
            val p = this[i]
            if (!frame.inside(p)) {
                val margin = frame.depth * SIDE_MARGIN
                if (p.z > frame.depth / 2f + margin) behind[i] = true
                if (p.z < frame.depth / 2f - margin) behind[i] = false
                continue
            }
            if (behind[i]) {
                if (z[i] < frame.depth + PAGE_GAP) z[i] = frame.depth + PAGE_GAP
            } else if (z[i] > 0f) {
                z[i] = 0f
            }
        }
        behind[0] = behind[1]
    }

    private fun link(frame: RibbonFrame, a: Int, b: Int, held: Boolean) {
        val wraps = behind[a] != behind[b] && (frame.inside(this[a]) || frame.inside(this[b]))
        if (!wraps) {
            keepApart(a, b, segment, stretch = true, last = held)
            return
        }
        val top = (y[a] + y[b]) / 2f < frame.height / 2f
        val corner =
            Vec3(
                (x[a] + x[b]) / 2f,
                if (top) -PAGE_GAP else frame.height + PAGE_GAP,
                frame.depth / 2f,
            )
        val toA = this[a] - corner
        val toB = this[b] - corner
        val da = toA.length()
        val db = toB.length()
        val excess = da + db - segment
        if (excess <= 0f) return
        val pinnedA = a == 0
        val pinnedB = held && b == POINTS - 1
        val (cutA, cutB) =
            when {
                pinnedA -> 0f to excess
                pinnedB -> excess to 0f
                else -> excess / 2f to excess / 2f
            }
        slide(a, b, corner, toA, toB, cutA)
        slide(b, a, corner, toB, toA, cutB)
    }

    @Suppress("LongParameterList")
    private fun slide(i: Int, other: Int, corner: Vec3, toSelf: Vec3, toOther: Vec3, cut: Float) {
        if (cut <= 0f) return
        val own = toSelf.length()
        if (cut < own) {
            place(i, corner + toSelf * ((own - cut) / own))
            return
        }
        val past = (cut - own).coerceAtMost(toOther.length())
        place(i, corner + toOther.normalized() * past)
        behind[i] = behind[other]
    }

    private fun place(i: Int, p: Vec3) {
        x[i] = p.x
        y[i] = p.y
        z[i] = p.z
    }

    private fun keepApart(a: Int, b: Int, length: Float, stretch: Boolean, last: Boolean) {
        val dx = x[b] - x[a]
        val dy = y[b] - y[a]
        val dz = z[b] - z[a]
        val distance = sqrt(dx * dx + dy * dy + dz * dz)
        val satisfied = if (stretch) distance <= length else distance >= length
        if (distance == 0f || satisfied) return
        val off = (distance - length) / distance
        val pinnedA = a == 0
        val pinnedB = last && b == POINTS - 1
        val first =
            when {
                pinnedA -> 0f
                pinnedB -> 1f
                else -> 0.5f
            }
        val second = if (pinnedB) 0f else 1f - first
        x[a] += dx * off * first
        y[a] += dy * off * first
        z[a] += dz * off * first
        x[b] -= dx * off * second
        y[b] -= dy * off * second
        z[b] -= dz * off * second
    }
}

internal const val RIBBON_TOP_X = 0.055f
private const val RIBBON_C1_X = 0.07f
private const val RIBBON_C1_Y = 0.35f
private const val RIBBON_C2_X = 0.1f
private const val RIBBON_C2_Y = 0.62f
private const val RIBBON_FOOT_X = 0.086f

private fun resample(path: List<Vec3>): List<Vec3> {
    val total = arcLength(path)
    val out = ArrayList<Vec3>(POINTS)
    var walked = 0f
    var i = 0
    for (k in 0 until POINTS) {
        val target = total * k / (POINTS - 1)
        while (i < path.size - 2 && walked + (path[i + 1] - path[i]).length() < target) {
            walked += (path[i + 1] - path[i]).length()
            i++
        }
        val span = (path[i + 1] - path[i]).length()
        val t = if (span == 0f) 0f else ((target - walked) / span).coerceIn(0f, 1f)
        out += path[i] + (path[i + 1] - path[i]) * t
    }
    return out
}

private fun arcLength(path: List<Vec3>): Float =
    path.zipWithNext().sumOf { (a, b) -> (b - a).length().toDouble() }.toFloat()

private fun splineAt(keys: List<Vec3>, v: Float): Vec3 {
    val scaled = v.coerceIn(0f, 1f) * (keys.size - 1)
    val i = scaled.toInt().coerceAtMost(keys.size - 2)
    return catmullRom(
        keys[(i - 1).coerceAtLeast(0)],
        keys[i],
        keys[i + 1],
        keys[(i + 2).coerceAtMost(keys.lastIndex)],
        scaled - i,
    )
}

private fun catmullRom(p0: Vec3, p1: Vec3, p2: Vec3, p3: Vec3, t: Float): Vec3 {
    val t2 = t * t
    val t3 = t2 * t
    return (p1 * 2f +
        (p2 - p0) * t +
        (p0 * 2f - p1 * 5f + p2 * 4f - p3) * t2 +
        (p1 * 3f - p0 - p2 * 3f + p3) * t3) * 0.5f
}
