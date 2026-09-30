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
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

/**
 * A drop of water on the glass: [at] as a fraction of it, [resting] once it has stopped, and
 * [stretch], from 0, a round bead, to 1, pulled long into a teardrop by its own weight as it runs,
 * and [alpha], how much of it still shows as fog covers it or it is smeared away.
 */
@Immutable
data class Bead(
    val at: Offset,
    val radius: Dp,
    val resting: Boolean,
    val stretch: Float = 0f,
    val alpha: Float = 1f,
)

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

    /** Whether a drop is growing or running. */
    var moving: Boolean by mutableStateOf(false)
        private set

    /** Whether the glass needs frames: a drop is moving, or one that stopped is still relaxing. */
    var needsFrames: Boolean by mutableStateOf(false)
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
    // Drops on their way off the glass: smeared by a wipe, or pushed out by newer ones.
    private val fading = mutableListOf<Drip>()

    /** How many streaks the driver still holds on to. */
    internal val trackedStreaks: Int
        get() = (finished + running + fading).sumOf { it.streaks.size }

    // Every streak of a drop the driver still holds, so no drop is ever cleared by one.
    private fun ownStreaks(): Set<WipeStroke> =
        (finished + running + fading).flatMapTo(HashSet()) { it.streaks }

    private var untilNext = nextGap()

    /**
     * The drops to draw, each as clear as it still is: a resting drop is fogged over as a breath's
     * front passes it, and one that is wiped or pushed out fades away rather than vanishing.
     */
    val beads: List<Bead>
        get() {
            version
            return (resting + running + fading).map { it.bead(it.visibility()) }
        }

    /**
     * Whether the glass needs frames now, or will once a drop starts to fade: read in a snapshot,
     * so a sleeping loop wakes when a wipe or a breath takes a drop off the glass.
     */
    val wantsFrames: Boolean
        get() {
            version
            if (needsFrames) return true
            val ours = ownStreaks()
            return resting.any { !it.stillShows(ours) } ||
                running.any { it.growing && !it.stillShows(ours) }
        }

    /** Starts a drop at [at], to run [length] of the glass's height. */
    fun drip(at: Offset, length: Float) {
        running += Drip(at, length)
        changed()
    }

    /** Moves everything on by [seconds]; with none, only takes wiped drops off the glass. */
    fun advance(seconds: Float) {
        if (glass.width <= 0.dp || glass.height <= 0.dp) return
        if (seconds > 0f && randomStarts) {
            untilNext -= seconds
            if (untilNext <= 0f) {
                untilNext = nextGap()
                if (running.size < MaxRunning) {
                    spot()?.let { running += Drip(it, random.between(MinLength, MaxLength)) }
                }
            }
        }
        // However long since the last step, a drop moves at most a short step's worth.
        val stepSeconds = min(seconds.coerceAtLeast(0f), MaxStepSeconds)
        // A drop wiped away before it starts to run has not started at all.
        val ours = ownStreaks()
        for (drip in running.filter { it.growing && !it.stillShows(ours) }) {
            running -= drip
            fadeOut(drip, WipedFadeSeconds)
        }
        if (stepSeconds > 0f) {
            for (drip in running.toList()) {
                drip.advance(stepSeconds)
                if (drip.stopped) {
                    running -= drip
                    resting += drip
                    finished += drip
                }
            }
        }
        // A long-idle mirror keeps only the latest drops' streaks, or the glass would slow down
        // under hundreds of them.
        while (finished.size > MaxResting) {
            val oldest = finished.removeFirst()
            oldest.streaks.forEach { fog.remove(it) }
            if (resting.remove(oldest)) fadeOut(oldest, PushedOutFadeSeconds)
        }
        // Forget streaks a breath has already fogged over.
        for (drip in finished + running + fading) {
            drip.streaks.removeAll { s -> fog.marks.none { it === s } }
        }
        for (drip in resting) drip.settle(stepSeconds)
        for (drip in resting.filter { !it.stillShows(ours) }) {
            resting -= drip
            // Fogged over by a full breath, it is already out of sight; wiped, it smears away.
            fadeOut(drip, if (drip.fogged) 0f else WipedFadeSeconds)
        }
        for (drip in fading.toList()) {
            drip.fade(stepSeconds)
            if (drip.gone) fading -= drip
        }
        changed()
    }

    private fun fadeOut(drip: Drip, seconds: Float) {
        drip.startFading(seconds)
        if (!drip.gone) fading += drip
    }

    private fun changed() {
        version++
        moving = running.isNotEmpty()
        needsFrames = moving || resting.any { it.relaxing } || fading.isNotEmpty()
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
        private var wipedBefore: Map<WipeStroke, Int> = emptyMap()
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
                if (burst == stops.size) {
                    stopped = true
                    // How far each wipe had got: from here on, only newer points can clear it.
                    val ours = ownStreaks()
                    wipedBefore =
                        fog.marks
                            .filterIsInstance<WipeStroke>()
                            .filter { it !in ours }
                            .associateWith { it.points.size }
                } else stuck = random.between(MinStickSeconds, MaxStickSeconds)
            }
        }

        // A trail as wide as the drop leaves, narrower where it first broke away.
        private fun streakRadius() =
            (diameter / 2 * if (burst == 0) TopStreakWidth else StreakWidth).dp

        private var fadeLeft = 1f
        private var fadeRate = 0f

        /** Faded right out. */
        val gone: Boolean
            get() = fadeLeft <= 0f

        /** Fogged over for good: a full breath has taken its streak off the glass. */
        val fogged: Boolean
            get() = lastStreak.let { last -> last != null && fog.marks.none { it === last } }

        /** Starts fading out over [seconds]; none, and it is gone at once. */
        fun startFading(seconds: Float) {
            if (seconds <= 0f) fadeLeft = 0f else fadeRate = 1f / seconds
        }

        fun fade(seconds: Float) {
            fadeLeft = (fadeLeft - fadeRate * seconds).coerceAtLeast(0f)
        }

        /**
         * How much of the drop shows: fading away, and, once it rests, fogged over by any breath
         * since its streak as the breath's front passes it. A running drop clears its own way.
         */
        fun visibility(): Float {
            if (!stopped || fadeLeft <= 0f) return fadeLeft
            val last = lastStreak ?: return fadeLeft
            val index = fog.marks.indexOfFirst { it === last }
            // Its streak gone: fogged over by a full breath, unless it is being pushed out, when
            // its streak went first and the drop fades after it.
            if (index < 0) return if (fadeRate > 0f) fadeLeft else 0f
            var cover = 0f
            for (i in index + 1 until fog.marks.size) {
                val mark = fog.marks[i]
                if (mark is Breath) cover = maxOf(cover, fogCoverAt(head.y, mark.level))
            }
            return fadeLeft * (1f - cover)
        }

        /** Stopped, but not yet relaxed into its resting shape. */
        val relaxing: Boolean
            get() = abs(stretch - RestingStretch) > RelaxedWithin

        /** Stuck or resting, a drop relaxes towards round, keeping a slight sag. */
        fun settle(seconds: Float) = ease(RestingStretch, seconds)

        private fun ease(target: Float, seconds: Float) {
            stretch += (target - stretch) * min(1f, seconds * StretchEasing)
        }

        fun bead(alpha: Float) =
            Bead(
                head,
                radius =
                    when {
                        grown < GrowSeconds -> (diameter / 2 * (grown / GrowSeconds)).dp
                        else -> (diameter / 2).dp
                    },
                resting = stopped,
                stretch = stretch,
                alpha = alpha,
            )

        // Shows until a breath drops its streak or a real wipe after it passes over the drop.
        fun stillShows(ours: Set<WipeStroke>): Boolean {
            val last = lastStreak
            if (last != null && fog.marks.none { it === last }) return false
            // Stopped, only a wipe's points since then clear it: it may rest on glass it ran into.
            return fog.marks.none {
                it is WipeStroke &&
                    it !in ours &&
                    it.covers(
                        head,
                        glass,
                        wipeRadius,
                        from = if (stopped) ((wipedBefore[it] ?: 0) - 1).coerceAtLeast(0) else 0,
                    )
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
    from: Int = 0,
): Boolean {
    val reach = (radius ?: wipeRadius).value + clearance.value
    // In dp, so the reach is round however the glass is shaped; along each segment, not just at
    // the points a fast swipe leaves far apart. From point [from] on, and by index: this runs
    // while drawing.
    val width = glass.width.value
    val height = glass.height.value
    val target = Offset(point.x * width, point.y * height)
    if (from >= points.size) return false
    var previous = Offset(points[from].x * width, points[from].y * height)
    if (points.size - from == 1) return (previous - target).getDistance() <= reach
    for (i in from + 1 until points.size) {
        val next = Offset(points[i].x * width, points[i].y * height)
        if (segmentDistance(target, previous, next) <= reach) return true
        previous = next
    }
    return false
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
private const val RelaxedWithin = 0.01f
private const val WipedFadeSeconds = 0.2f
private const val PushedOutFadeSeconds = 1f

// A wipe's soft edge still looks clear: a drop starts at least this far beyond its brush.
private val StartClearance = 12.dp
