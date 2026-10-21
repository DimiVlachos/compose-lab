package dev.dimvlachos.lab.core.presentation.components.bookshelf.pageturn

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.util.lerp
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

private const val Placement = 12
private const val SqrtPi = 1.7724539f
private const val PinScanStep = 0.02f
private const val PinSearchEnds = 0.12f
private const val PinSettled = 2e-4f
private const val PinBisections = 10
private const val PinProbe = 1e-3f
private const val PinMaxStep = 0.08f
private const val SpringStep = 1f / 240f

internal data class StripPose(val angle: Float, val hingeX: Float, val hingeZ: Float)

internal class PageGeometry(
    val spineX: Float,
    val originY: Float,
    val perspectivePx: Float,
    val page: Float,
    val height: Float,
    val restLift: Float = 0f,
)

internal class BookLayout(val left: Float, val top: Float, val geometry: PageGeometry)

internal data class Grip(val u: Float, val v: Float, val weight: Float = 1f)

internal class TurnFrame(
    val t: Float,
    val width: Float,
    val pageWidth: Float,
    val height: Float,
    val tilt: Float,
    private val spacing: Float,
    val stripWidth: Float,
    val poses: List<StripPose>,
    val lift: Float,
    val textureShift: Float,
    private val boundaryAngles: FloatArray,
    private val placements: FloatArray,
) {
    val wedges: Int
        get() = poses.size

    private val scratch = FloatArray(9)

    fun boundaryAngle(boundary: Int): Float = boundaryAngles[boundary]

    fun rulingX(k: Int, y: Float): Float =
        k * spacing * width * (1f + tilt * (y - height / 2f) / (height / 2f))

    fun wedgeAt(x: Float, y: Float): Int {
        val across = x / (width * (1f + tilt * (y - height / 2f) / (height / 2f)))
        return floor(across / spacing).toInt().coerceIn(0, wedges - 1)
    }

    fun facesReader(k: Int): Boolean = placements[k * Placement + 8] >= 0f

    fun bookX(k: Int, x: Float, y: Float): Float {
        val p = k * Placement
        return placements[p] * x + placements[p + 1] * y + placements[p + 9]
    }

    fun bookPoint(x: Float, y: Float): Triple<Float, Float, Float> {
        val p = wedgeAt(x, y) * Placement
        val r = placements
        return Triple(
            r[p] * x + r[p + 1] * y + r[p + 9],
            r[p + 3] * x + r[p + 4] * y + r[p + 10],
            r[p + 6] * x + r[p + 7] * y + r[p + 11],
        )
    }

    fun matrix(k: Int, geometry: PageGeometry, out: Matrix): Matrix =
        Homography.toMatrix(homography(k, geometry), out)

    fun project(x: Float, y: Float, geometry: PageGeometry): Offset =
        Homography.project(homography(wedgeAt(x, y), geometry), x, y)

    fun projectOn(k: Int, x: Float, y: Float, geometry: PageGeometry): Offset =
        Homography.project(homography(k, geometry), x, y)

    fun unproject(point: Offset, geometry: PageGeometry): Offset? {
        return (wedges - 1 downTo 0).firstNotNullOfOrNull { k ->
            Homography.unproject(homography(k, geometry), point)?.takeIf { holds(k, it) }
        }
    }

    private fun holds(k: Int, seen: Offset): Boolean {
        val onPage = seen.x in 0f..width && seen.y in 0f..height
        val across =
            seen.x / (width * (1f + tilt * (seen.y - height / 2f) / (height / 2f))) / spacing
        val ownPiece = across < k + 1 || k == wedges - 1
        return onPage && across >= k && ownPiece
    }

    private fun homography(k: Int, geometry: PageGeometry): FloatArray {
        val p = k * Placement
        val r = placements
        return Homography.of(
            ox = r[p + 9] + geometry.spineX,
            oy = r[p + 10],
            oz = r[p + 11],
            ux = r[p],
            uy = r[p + 3],
            uz = r[p + 6],
            vx = r[p + 1],
            vy = r[p + 4],
            vz = r[p + 7],
            cx = geometry.spineX,
            cy = geometry.originY,
            perspective = geometry.perspectivePx,
            out = scratch,
        )
    }
}

@Suppress("LongParameterList")
internal fun turnFrame(
    t: Float,
    leafWidth: Float,
    bendDirection: Float = 1f,
    strips: Int = PageTurnDimens.Strips,
    height: Float = leafWidth,
    tilt: Float = 0f,
    grip: Grip? = null,
    restLift: Float = 0f,
): TurnFrame {
    val chain = LeafChain(t, leafWidth, bendDirection, strips, height, tilt, grip, restLift)
    val wedges = chain.wedges
    val boundaries = FloatArray(wedges + 1) { chain.boundaryAngle(it) }
    val poses = ArrayList<StripPose>(wedges)
    val placements = FloatArray(wedges * Placement)
    var hingeX = 0f
    var hingeZ = chain.shiftZ
    val r = floatArrayOf(1f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 1f)
    val move = FloatArray(3)
    var previous = 0f
    for (k in 0 until wedges) {
        val angle = chain.angle(k)
        poses += StripPose(angle, hingeX, hingeZ)
        hingeX += chain.spacing * chain.width * cos(angle)
        hingeZ += chain.spacing * chain.width * sin(angle)
        chain.fold(k, angle - previous, r, move)
        previous = angle
        r.copyInto(placements, k * Placement)
        move.copyInto(placements, k * Placement + 9)
        placements[k * Placement + 11] += chain.shiftZ
    }
    return TurnFrame(
        t,
        chain.width,
        leafWidth,
        height,
        chain.lean,
        chain.spacing,
        chain.width / strips,
        poses,
        chain.lift,
        chain.textureShift,
        boundaries,
        placements,
    )
}

private class LeafChain(
    t: Float,
    pageWidth: Float,
    bendDirection: Float,
    strips: Int,
    height: Float,
    tilt: Float,
    grip: Grip?,
    private val restLift: Float,
) {
    private val theta = (PI * t).toFloat()
    val lift = sin(PI * t).toFloat()
    private val hold = grip?.weight?.coerceIn(0f, 1f) ?: 0f
    private val gripShare = grip?.takeIf { hold > 0f }?.let { GripShare(it.u) }
    private val bend =
        PageTurnDimens.BendMax * lift * (grip?.let { lerp(1f, gripLever(it.u), hold) } ?: 1f)
    private val bow = bendDirection.coerceIn(-PageTurnDimens.BowMax, PageTurnDimens.BowMax)
    val lean = tilt.coerceIn(-PageTurnDimens.TiltLimit, PageTurnDimens.TiltLimit)
    private val reach = 1f / (1f - abs(lean))
    val spacing = max(1f / strips, reach / PageTurnDimens.MaxWedges)
    val wedges = max(strips, ceil(reach / spacing - 0.001f).toInt())
    private val half = height / 2f
    private val restSide =
        (1f - 2f * t) *
            (1f - smoothstep((abs(lean) / PageTurnDimens.TiltRestFade).coerceIn(0f, 1f)))
    private val rest = restShift(restLift, strips)
    val width = pageWidth / (1f - rest.x * abs(restSide))
    val textureShift = rest.x * width * abs(restSide)
    val shiftZ = rest.y * width * abs(restSide)

    private fun share(u: Float): Float = gripShare?.let { lerp(u, it.at(u), hold) } ?: u

    fun angle(k: Int): Float {
        val u0 = k * spacing
        val u1 = (k + 1) * spacing
        val rise = restLift * (restHeight(min(u1, 1f)) - restHeight(min(u0, 1f))) / spacing
        return -theta + bow * bend * (2f * share(u0) - 1f) - atan(rise) * restSide
    }

    fun boundaryAngle(b: Int): Float = -theta + bow * bend * (2f * share(b * spacing) - 1f)

    fun fold(k: Int, turn: Float, r: FloatArray, move: FloatArray) {
        val u0 = k * spacing
        val leanX = -u0 * width * lean / half
        val length = sqrt(leanX * leanX + 1f)
        foldAbout(r, move, u0 * width, half, leanX / length, -1f / length, turn)
    }

    fun wedgeAt(x: Float, y: Float): Int {
        val across = x / (width * (1f + lean * (y - half) / half))
        return floor(across / spacing).toInt().coerceIn(0, wedges - 1)
    }
}

@Suppress("LongParameterList")
internal fun heldPoint(
    grab: Offset,
    t: Float,
    leafWidth: Float,
    bendDirection: Float,
    height: Float,
    tilt: Float,
    grip: Grip?,
    out: FloatArray,
    restLift: Float = 0f,
    strips: Int = PageTurnDimens.Strips,
) {
    val chain = LeafChain(t, leafWidth, bendDirection, strips, height, tilt, grip, restLift)
    val r = floatArrayOf(1f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 1f)
    val move = FloatArray(3)
    var previous = 0f
    for (k in 0..chain.wedgeAt(grab.x, grab.y)) {
        val angle = chain.angle(k)
        chain.fold(k, angle - previous, r, move)
        previous = angle
    }
    out[0] = r[0] * grab.x + r[1] * grab.y + move[0]
    out[1] = r[3] * grab.x + r[4] * grab.y + move[1]
    out[2] = r[6] * grab.x + r[7] * grab.y + move[2] + chain.shiftZ
}

internal fun restHeight(u: Float): Float {
    val gutter = PageTurnDimens.GutterSpan
    val curl = PageTurnDimens.CurlSpan
    return when {
        u < gutter -> -PageTurnDimens.GutterSink * (1f - u / gutter).let { it * it }
        u > 1f - curl -> -PageTurnDimens.CurlDrop * ((u - 1f + curl) / curl).let { it * it }
        else -> 0f
    }
}

private fun restShift(restLift: Float, strips: Int): Offset {
    if (restLift == 0f) return Offset.Zero
    val spacing = 1f / strips
    val plateau = ceil(PageTurnDimens.GutterSpan * strips - 0.001f).toInt()
    var x = 0f
    var z = 0f
    for (k in 0 until plateau) {
        val rise = restLift * (restHeight((k + 1) * spacing) - restHeight(k * spacing)) / spacing
        val angle = -atan(rise)
        x += spacing * cos(angle)
        z += spacing * sin(angle)
    }
    return Offset(plateau * spacing - x, -z)
}

@Suppress("LongParameterList")
private fun foldAbout(
    r: FloatArray,
    move: FloatArray,
    ax: Float,
    ay: Float,
    ex: Float,
    ey: Float,
    angle: Float,
) {
    val c = cos(angle)
    val s = sin(angle)
    val d = 1f - c
    val q0 = c + d * ex * ex
    val q1 = d * ex * ey
    val q2 = s * ey
    val q3 = d * ex * ey
    val q4 = c + d * ey * ey
    val q5 = -s * ex
    val q6 = -s * ey
    val q7 = s * ex
    val q8 = c
    val dx = ax - (q0 * ax + q1 * ay)
    val dy = ay - (q3 * ax + q4 * ay)
    val dz = -(q6 * ax + q7 * ay)
    move[0] += r[0] * dx + r[1] * dy + r[2] * dz
    move[1] += r[3] * dx + r[4] * dy + r[5] * dz
    move[2] += r[6] * dx + r[7] * dy + r[8] * dz
    for (row in 0 until 3) {
        val a = r[row * 3]
        val b = r[row * 3 + 1]
        val e = r[row * 3 + 2]
        r[row * 3] = a * q0 + b * q3 + e * q6
        r[row * 3 + 1] = a * q1 + b * q4 + e * q7
        r[row * 3 + 2] = a * q2 + b * q5 + e * q8
    }
}

internal fun gripLever(u: Float): Float =
    lerp(PageTurnDimens.SpineLever, PageTurnDimens.EdgeLever, smoothstep(u.coerceIn(0f, 1f)))

internal fun gripShare(u: Float, held: Float): Float = GripShare(held).at(u)

private class GripShare(private val held: Float) {
    private val spread = PageTurnDimens.GripSpread
    private val atSpine = erf(-held / spread)
    private val total = gathered(1f)

    private fun gathered(x: Float): Float =
        x + PageTurnDimens.GripPeak * spread * (SqrtPi / 2f) * (erf((x - held) / spread) - atSpine)

    fun at(u: Float): Float = gathered(u) / total
}

@Suppress("MagicNumber")
private fun erf(x: Float): Float {
    val sign = if (x < 0f) -1f else 1f
    val a = abs(x)
    val k = 1f / (1f + 0.3275911f * a)
    val poly =
        k *
            (0.2548296f +
                k * (-0.28449672f + k * (1.4214138f + k * (-1.4531521f + k * 1.0614054f))))
    return sign * (1f - poly * exp(-a * a))
}

internal fun stripShadeAlphas(frame: TurnFrame, index: Int): Pair<Float, Float> =
    (1f - edgeLight(frame, index)) * PageTurnDimens.ShadowMax to
        (1f - edgeLight(frame, index + 1)) * PageTurnDimens.ShadowMax

internal fun stripGlareAlpha(frame: TurnFrame, index: Int): Float {
    val light = edgeLight(frame, index)
    return frame.lift * frame.lift * light * light * PageTurnDimens.GlareMax
}

internal fun shouldCommit(progress: Float, velocity: Float, flung: Float = progress): Boolean =
    when {
        velocity > PageTurnDimens.CommitVelocity -> true
        velocity < -PageTurnDimens.CommitVelocity -> false
        else -> flung > PageTurnDimens.CommitProgress
    }

internal fun bendTowards(forward: Boolean, progressIncreasing: Boolean): Float =
    if (forward == progressIncreasing) 1f else -1f

internal fun gripTilt(grip: Grip): Float {
    val fromMiddle = abs(grip.v - 0.5f)
    val corner =
        smoothstep(
            ((fromMiddle - PageTurnDimens.TiltDeadZone) / (0.5f - PageTurnDimens.TiltDeadZone))
                .coerceIn(0f, 1f),
        )
    val side = if (grip.v < 0.5f) 1f else -1f
    return side * PageTurnDimens.TiltMax * corner * smoothstep(grip.u.coerceIn(0f, 1f))
}

@Suppress("LongParameterList")
internal fun pinTurn(
    grab: Offset,
    targetX: Float,
    from: Float,
    bow: Float,
    tilt: Float,
    grip: Grip,
    geometry: PageGeometry,
    liftedFrom: Float = Float.NaN,
): Float {
    val nearAnEnd = from < PinSearchEnds || from > 1f - PinSearchEnds
    var t =
        if (nearAnEnd) turnUnder(grab, targetX, from, tilt, bow, grip, geometry, liftedFrom)
        else from.coerceIn(0f, 1f)
    val point = FloatArray(3)
    fun miss(t: Float): Float =
        (heldSeenX(grab, t, bow, tilt, grip, geometry, point, liftedFrom) - targetX) / geometry.page
    repeat(PageTurnDimens.PinSteps) {
        val here = miss(t)
        val probe = if (t > 1f - PinProbe) -PinProbe else PinProbe
        val slope = (miss(t + probe) - here) / probe
        if (abs(slope) < 1e-6f) return t
        val step = (-here / slope).coerceIn(-PinMaxStep, PinMaxStep)
        t = (t + step).coerceIn(0f, 1f)
        if (abs(step) < PinSettled) return t
    }
    return t
}

@Suppress("LongParameterList")
internal fun heldSeenX(
    grab: Offset,
    t: Float,
    bow: Float,
    tilt: Float,
    grip: Grip?,
    geometry: PageGeometry,
    point: FloatArray = FloatArray(3),
    liftedFrom: Float = Float.NaN,
): Float {
    fun seen(t: Float): Float {
        heldPoint(
            grab,
            t,
            geometry.page,
            bow,
            geometry.height,
            tilt,
            grip,
            point,
            geometry.restLift,
        )
        val (x, _, z) = point
        val share =
            lerp(
                PageTurnDimens.PinPerspective,
                1f,
                smoothstep((min(t, 1f - t) / PageTurnDimens.LiftSwing).coerceIn(0f, 1f)),
            )
        val scale = share * (geometry.perspectivePx / (geometry.perspectivePx + z) - 1f)
        return x * (1f + scale)
    }
    val real = seen(t)
    if (liftedFrom.isNaN()) return geometry.spineX + real
    val risen = smoothstep((abs(t - liftedFrom) / PageTurnDimens.LiftSwing).coerceIn(0f, 1f))
    if (risen >= 1f) return geometry.spineX + real
    val resting = if (liftedFrom < 0.5f) seen(0f) else -seen(1f)
    val swing = resting * cos(PI.toFloat() * t)
    return geometry.spineX + lerp(swing, real, risen)
}

internal fun liftLag(travel: Float, span: Float): Float =
    when {
        travel <= 0f -> 0f
        travel < span -> travel - travel * travel / (2f * span)
        else -> span / 2f * (1f - smoothstep(((travel - span) / (2f * span)).coerceIn(0f, 1f)))
    }

internal fun leanFor(grip: Grip, pull: Offset, t: Float, geometry: PageGeometry): Float {
    val pulled = pullTilt(pull, geometry)
    val hold = gripTilt(grip) * (1f - abs(pulled) / PageTurnDimens.TiltMax)
    return ((hold + pulled) * tiltRamp(t)).coerceIn(
        -PageTurnDimens.TiltLimit,
        PageTurnDimens.TiltLimit,
    )
}

internal fun pullTilt(pull: Offset, geometry: PageGeometry): Float {
    val settle = PageTurnDimens.PullSettle * geometry.page
    val towardsSpine = -pull.x
    if (towardsSpine <= 0f) return 0f
    val lean = pull.y / (towardsSpine + settle / 4f) * geometry.height / (2f * geometry.page)
    val going = smoothstep((towardsSpine / settle).coerceIn(0f, 1f))
    return lean.coerceIn(-PageTurnDimens.TiltMax, PageTurnDimens.TiltMax) * going
}

@Suppress("LongParameterList", "CognitiveComplexMethod", "NestedBlockDepth")
private fun turnUnder(
    grab: Offset,
    targetX: Float,
    from: Float,
    tilt: Float,
    bow: Float,
    grip: Grip,
    geometry: PageGeometry,
    liftedFrom: Float,
): Float {
    val point = FloatArray(3)
    fun miss(t: Float) = heldSeenX(grab, t, bow, tilt, grip, geometry, point, liftedFrom) - targetX
    val start = from.coerceIn(0f, 1f)
    val here = miss(start)
    if (here == 0f) return start
    var nearest = start
    var nearestMiss = abs(here)
    val edge = floatArrayOf(start, start)
    val edgeMiss = floatArrayOf(here, here)
    var step = 1
    while (edge[0] < 1f || edge[1] > 0f) {
        for (side in 0..1) {
            val a = edge[side]
            val b = (start + (if (side == 0) 1f else -1f) * step * PinScanStep).coerceIn(0f, 1f)
            if (b == a) continue
            val missB = miss(b)
            if (abs(missB) < nearestMiss) {
                nearest = b
                nearestMiss = abs(missB)
            }
            if ((edgeMiss[side] < 0f) != (missB < 0f)) {
                var lo = a
                var hi = b
                var missLo = edgeMiss[side]
                repeat(PinBisections) {
                    val mid = (lo + hi) / 2f
                    val missMid = miss(mid)
                    if ((missMid < 0f) == (missLo < 0f)) {
                        lo = mid
                        missLo = missMid
                    } else hi = mid
                }
                return (lo + hi) / 2f
            }
            edge[side] = b
            edgeMiss[side] = missB
        }
        step++
    }
    return nearest
}

private fun tiltRamp(t: Float): Float =
    smoothstep((min(t, 1f - t) / PageTurnDimens.TiltRamp).coerceIn(0f, 1f))

internal data class Spring(val value: Float, val velocity: Float = 0f) {
    fun step(target: Float, dt: Float, frequency: Float, damping: Float): Spring {
        val omega = 2f * PI.toFloat() * frequency
        var value = value
        var velocity = velocity
        var left = dt
        while (left > 0f) {
            val h = min(left, SpringStep)
            velocity += (-omega * omega * (value - target) - 2f * damping * omega * velocity) * h
            value += velocity * h
            left -= h
        }
        return Spring(value, velocity)
    }
}

internal fun bowTarget(direction: Float, velocity: Float, held: Boolean): Float =
    direction * (if (held) -PageTurnDimens.HoldBow else PageTurnDimens.BowRest) +
        (velocity * PageTurnDimens.FlexGain).coerceIn(
            -PageTurnDimens.FlexMax,
            PageTurnDimens.FlexMax,
        )

internal fun smoothstep(x: Float): Float = x * x * (3f - 2f * x)

internal fun sheetLift(depth: Float): Float =
    PageTurnDimens.RestLift *
        (1f - PageTurnDimens.SheetFlatten * depth).coerceAtLeast(PageTurnDimens.FlattestShare)

internal fun stackRise(t: Float): Float =
    smoothstep((t / PageTurnDimens.SettleSpan).coerceIn(0f, 1f))

internal fun stackLand(t: Float): Float =
    smoothstep(
        ((t - (1f - PageTurnDimens.SettleSpan)) / PageTurnDimens.SettleSpan).coerceIn(0f, 1f),
    )

private fun edgeLight(frame: TurnFrame, boundary: Int): Float =
    abs(cos(frame.boundaryAngle(boundary)))
