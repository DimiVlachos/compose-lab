package dev.dimvlachos.lab.core.presentation.components.fog

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

/**
 * A drop of water on the glass: [at] as a fraction of it, [resting] once it has stopped, and
 * [stretch], from 0, a round bead, to 1, pulled long into a teardrop by its own weight as it runs.
 */
@Immutable
data class Bead(val at: Offset, val radius: Dp, val resting: Boolean, val stretch: Float = 0f)

/**
 * Condensation running down a [FogState]: every few seconds a drop gathers somewhere on the fogged
 * glass, grows heavy, and runs down in fits and starts, leaving a thin wet streak. Stopped, it
 * rests at the streak's end until a breath fogs it over or a wipe clears it. [advance] moves it all
 * on; [drip] starts a drop at a given spot, for a script.
 */
@Stable
class DripDriver(
    private val fog: FogState,
    private val random: Random = Random.Default,
    private val wipeRadius: Dp = FogDimens.BrushRadius,
) {
    /** The glass's size; nothing drips until it has one. */
    var glass: DpSize = DpSize.Zero

    /** Whether drops start by themselves every few seconds; off, only [drip] starts one. */
    var randomStarts: Boolean = true

    /** Whether a drop is growing or running, so the glass needs frames. */
    var moving: Boolean by mutableStateOf(false)
        private set

    /** Seconds until a drop starts by itself, or null while [randomStarts] is off. */
    val secondsToNextStart: Float?
        get() = if (randomStarts) untilNext else null

    // Bumped at every change, so a reader of [beads] redraws when the drops move.
    private var version by mutableIntStateOf(0)
    private val running = mutableListOf<Drip>()
    private val resting = mutableListOf<Drip>()
    // Drops that have stopped, oldest first: only the last few keep their streaks on the glass.
    private val finished = ArrayDeque<Drip>()

    /** How many streaks the driver still holds on to. */
    internal val trackedStreaks: Int
        get() = (finished + running).sumOf { it.streaks.size }

    // Every streak of a drop the driver still holds, so no drop is ever cleared by one.
    private fun ownStreaks(): Set<WipeStroke> =
        (finished + running).flatMapTo(HashSet()) { it.streaks }

    private var untilNext = nextGap()

    /**
     * The drops to draw. A resting drop that a breath has fogged over or a wipe has cleared is left
     * out at once, as the fog changes, not at the next step.
     */
    val beads: List<Bead>
        get() {
            version
            return resting.filter { it.stillShows() }.map { it.bead() } +
                running.filter { !it.growing || it.stillShows() }.map { it.bead() }
        }

    /** Starts a drop at [at], to run [length] of the glass's height. */
    fun drip(at: Offset, length: Float) {
        running += Drip(at, length)
        changed()
    }

    fun advance(seconds: Float) {
        if (seconds <= 0f || glass.width <= 0.dp || glass.height <= 0.dp) return
        if (randomStarts) {
            untilNext -= seconds
            if (untilNext <= 0f) {
                untilNext = nextGap()
                if (running.size < MaxRunning) {
                    spot()?.let { running += Drip(it, random.between(MinLength, MaxLength)) }
                }
            }
        }
        // However long since the last step, a drop moves at most a short step's worth.
        val stepSeconds = min(seconds, MaxStepSeconds)
        // A drop wiped away before it starts to run has not started at all.
        running.removeAll { it.growing && !it.stillShows() }
        for (drip in running.toList()) {
            drip.advance(stepSeconds)
            if (drip.stopped) {
                running -= drip
                resting += drip
                finished += drip
            }
        }
        // A long-idle mirror keeps only the latest drops' streaks, or the glass would slow down
        // under hundreds of them.
        while (finished.size > MaxResting) {
            val oldest = finished.removeFirst()
            oldest.streaks.forEach { fog.remove(it) }
            resting -= oldest
        }
        // Forget streaks a breath has already fogged over.
        for (drip in finished + running) drip.streaks.removeAll { s -> fog.marks.none { it === s } }
        for (drip in resting) drip.settle(stepSeconds)
        resting.removeAll { !it.stillShows() }
        while (resting.size > MaxResting) resting.removeAt(0)
        changed()
    }

    private fun changed() {
        version++
        moving = running.isNotEmpty()
    }

    private fun nextGap() = random.between(MinGapSeconds, MaxGapSeconds)

    // A fogged spot in the top two thirds, away from the sides; none after a few tries skips a
    // turn.
    private fun spot(): Offset? =
        (1..SpotTries)
            .asSequence()
            .map {
                Offset(
                    random.between(SideMargin, 1f - SideMargin),
                    random.between(TopMargin, HighestStart),
                )
            }
            .firstOrNull { fog.isFoggedAt(it, glass, wipeRadius) }

    private inner class Drip(private val start: Offset, length: Float) {
        // Mostly small drops, now and then a heavy one: the bigger, the faster it runs.
        private val size = random.nextFloat().let { it * it }
        private val diameter = MinBeadDp + (MaxBeadDp - MinBeadDp) * size
        private val topSpeed = MinSpeedDp + (MaxSpeedDp - MinSpeedDp) * size
        private val wobble = random.between(0.5f, MaxWobbleDp)
        private val phase = random.between(0f, 2 * PI.toFloat())
        // Where each burst ends, down to the drop's length or the bottom edge.
        private val stops: List<Float> = run {
            val end = min(start.y + length, 1f)
            val cuts = List(random.nextInt(MinBursts, MaxBursts + 1) - 1) { random.nextFloat() }
            (cuts.sorted() + 1f).map { start.y + (end - start.y) * it }
        }
        private var grown = 0f
        private var burst = 0
        private var speed = 0f
        private var stuck = 0f
        private var streak: WipeStroke? = null
        private var lastStreak: WipeStroke? = null
        private var head = start
        private var stretch = 0f
        var stopped = false
            private set

        /** This drop's streaks still on the glass, one per burst. */
        val streaks = mutableListOf<WipeStroke>()

        /** Still gathering where it formed, not yet running. */
        val growing: Boolean
            get() = grown < GrowSeconds

        fun advance(seconds: Float) {
            if (grown < GrowSeconds) {
                grown += seconds
                return
            }
            if (stuck > 0f) {
                stuck -= seconds
                settle(seconds)
                return
            }
            // A breath may have fogged over the streak so far: carry on with a fresh one.
            val current =
                streak?.takeIf { s -> fog.marks.any { it === s } }
                    ?: fog.beginStroke(head, streakRadius(), StreakClarity).also {
                        streak = it
                        lastStreak = it
                        streaks += it
                    }
            speed = min(topSpeed, speed + AccelerationDp * seconds)
            // Pulled long by its own weight, the more the faster it runs.
            ease(RunningStretch + (1f - RunningStretch) * speed / topSpeed, seconds)
            val y = min(head.y + speed * seconds / glass.height.value, stops[burst])
            // A gentle wave about the line it started on, never more than 2 × wobble off it.
            val travelled = (y - start.y) * glass.height.value
            val x =
                start.x +
                    (sin(phase + travelled / WaveLengthDp * 2 * PI.toFloat()) - sin(phase)) *
                        wobble / glass.width.value
            head = Offset(x.coerceIn(0f, 1f), y)
            fog.extendStroke(current, head)
            if (y >= stops[burst]) {
                burst++
                speed = 0f
                streak = null
                if (burst == stops.size) stopped = true
                else stuck = random.between(MinStickSeconds, MaxStickSeconds)
            }
        }

        // A trail as wide as the drop leaves, narrower where it first broke away.
        private fun streakRadius() =
            (diameter / 2 * if (burst == 0) TopStreakWidth else StreakWidth).dp

        /** Stuck or resting, a drop relaxes towards round, keeping a slight sag. */
        fun settle(seconds: Float) = ease(RestingStretch, seconds)

        private fun ease(target: Float, seconds: Float) {
            stretch += (target - stretch) * min(1f, seconds * StretchEasing)
        }

        fun bead() =
            Bead(
                head,
                radius =
                    when {
                        grown < GrowSeconds -> (diameter / 2 * (grown / GrowSeconds)).dp
                        else -> (diameter / 2).dp
                    },
                resting = stopped,
                stretch = stretch,
            )

        // Shows until a breath drops its streak or a real wipe after it passes over the drop.
        fun stillShows(): Boolean {
            val ours = ownStreaks()
            val last =
                lastStreak
                    ?: return fog.marks.none {
                        it is WipeStroke && it !in ours && it.covers(head, glass, wipeRadius)
                    }
            val index = fog.marks.indexOfFirst { it === last }
            if (index < 0) return false
            return fog.marks.drop(index + 1).none {
                it is WipeStroke && it !in ours && it.covers(head, glass, wipeRadius)
            }
        }
    }
}

/**
 * Whether [point] is surely under fog: nothing has evaporated since the glass was last fogged over,
 * and no wipe passes over it. Partial breaths are not counted, which errs towards skipping a drop,
 * never towards one on clear glass.
 */
internal fun FogState.isFoggedAt(point: Offset, glass: DpSize, wipeRadius: Dp): Boolean =
    marks.none { it is Evaporation } &&
        marks.none { it is WipeStroke && it.covers(point, glass, wipeRadius, StartClearance) }

internal fun WipeStroke.covers(
    point: Offset,
    glass: DpSize,
    wipeRadius: Dp,
    clearance: Dp = 0.dp,
): Boolean {
    val reach = (radius ?: wipeRadius).value + clearance.value
    // In dp, so the reach is round however the glass is shaped; along each segment, not just at
    // the points a fast swipe leaves far apart.
    fun dp(p: Offset) = Offset(p.x * glass.width.value, p.y * glass.height.value)
    val target = dp(point)
    if (points.size == 1) return (dp(points[0]) - target).getDistance() <= reach
    return points.zipWithNext().any { (a, b) -> segmentDistance(target, dp(a), dp(b)) <= reach }
}

private fun segmentDistance(p: Offset, a: Offset, b: Offset): Float {
    val ab = b - a
    val lengthSquared = ab.getDistanceSquared()
    if (lengthSquared == 0f) return (p - a).getDistance()
    val t = (((p - a).x * ab.x + (p - a).y * ab.y) / lengthSquared).coerceIn(0f, 1f)
    return (p - (a + ab * t)).getDistance()
}

private fun Random.between(from: Float, until: Float) = from + nextFloat() * (until - from)

private const val MinGapSeconds = 4f
private const val MaxGapSeconds = 8f
private const val MaxRunning = 2
private const val MaxResting = 12
private const val SpotTries = 8
private const val SideMargin = 0.08f
private const val TopMargin = 0.05f
private const val HighestStart = 2f / 3f
private const val MinLength = 0.15f
private const val MaxLength = 0.4f
private const val MinBursts = 2
private const val MaxBursts = 4
private const val GrowSeconds = 0.6f
private const val MinStickSeconds = 0.2f
private const val MaxStickSeconds = 0.8f
private const val MaxStepSeconds = 0.1f
private const val MinSpeedDp = 90f
private const val MaxSpeedDp = 190f
private const val AccelerationDp = 600f
private const val MinBeadDp = 6f
private const val MaxBeadDp = 16f
private const val MaxWobbleDp = 1.4f
private const val WaveLengthDp = 60f
private const val StreakClarity = 0.9f
private const val StreakWidth = 0.7f
private const val TopStreakWidth = 0.55f
private const val RunningStretch = 0.3f
private const val RestingStretch = 0.2f
private const val StretchEasing = 6f

// A wipe's soft edge still looks clear: a drop starts at least this far beyond its brush.
private val StartClearance = 12.dp
