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
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * A drop of water on the glass: [at] as a fraction of it, [resting] once it has stopped, and
 * [stretch], from 0, a round bead, to 1, pulled long into a teardrop by its own weight as it runs,
 * [alpha], how much of it still shows as fog covers it or it is smeared away, and [softness], from
 * 0, a fresh bead, to 1, settled into the condensation around it once it has stopped, and [spread],
 * from 0 to 1 as a drop meeting a wiped patch slumps and spreads along its edge, merging in.
 */
@Immutable
data class Bead(
    val at: Offset,
    val radius: Dp,
    val resting: Boolean,
    val stretch: Float = 0f,
    val alpha: Float = 1f,
    val softness: Float = 0f,
    val spread: Float = 0f,
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
            return resting.any { !it.stillShows(ours) } || running.any { !it.stillShows(ours) }
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
        // A wipe over a drop, growing, running or between bursts, smears it away.
        val ours = ownStreaks()
        for (drip in running.filter { !it.stillShows(ours) }) {
            running -= drip
            fadeOut(drip, WipedFadeSeconds)
        }
        if (stepSeconds > 0f) {
            for (drip in running.toList()) {
                drip.advance(stepSeconds)
                if (drip.blending) {
                    // Reached a wiped patch: it merges into the clear glass at its edge.
                    running -= drip
                    finished += drip
                    fadeOut(drip, BlendSeconds)
                } else if (drip.stopped) {
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
            // As clear as it shows now: a drop a breath has fogged over stays out of sight.
            val shown = oldest.visibility()
            oldest.streaks.forEach { fog.remove(it) }
            if (resting.remove(oldest)) fadeOut(oldest, PushedOutFadeSeconds, from = shown)
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

    private fun fadeOut(drip: Drip, seconds: Float, from: Float = 1f) {
        drip.startFading(seconds, from)
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
        // How far each breath under way when it stopped had already fogged it.
        private var fogAtStop: Map<FogMark, Float> = emptyMap()
        private var head = start
        private var stretch = 0f
        var stopped = false
            private set

        /** This drop's streaks still on the glass, one per burst. */
        val streaks = mutableListOf<WipeStroke>()

        /** Still gathering where it formed, not yet running. */
        val growing: Boolean
            get() = grown < GrowSeconds

        /** Slid onto a wiped patch and now dissolving into its film. */
        var blending = false
            private set

        // Sliding on onto a wiped patch: how fast it went in, how far it has gone, how far it will.
        private var sliding = false
        private var slideSpeed = 0f
        private var slid = 0f
        private val slideLength: Float
            get() = diameter * SlideLengths

        fun advance(seconds: Float) {
            if (grown < GrowSeconds) {
                grown += seconds
                return
            }
            if (sliding) {
                slide(seconds)
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
            val next = Offset(x.coerceIn(0f, 1f), y)
            // Feel down in small steps: at a wiped patch's edge, its trail ends and it slides on
            // onto the wet, clear glass.
            val edge = edgeBefore(head, next)
            head = edge ?: next
            fog.extendStroke(current, head)
            if (edge != null) {
                streak = null
                sliding = true
                slideSpeed = maxOf(speed, MinSlideSpeedDp)
                noteWipesSoFar()
                return
            }
            if (y >= stops[burst]) {
                burst++
                speed = 0f
                streak = null
                if (burst == stops.size) {
                    stopped = true
                    noteWipesSoFar()
                    fogAtStop =
                        fog.marks
                            .filter { it is Breath || it is Mist }
                            .associateWith { it.fogAt(head.y) }
                } else stuck = random.between(MinStickSeconds, MaxStickSeconds)
            }
        }

        // On clear, wet glass: it brakes steadily to a stop over a couple of its own sizes,
        // relaxing out of its teardrop, then dissolves into the film.
        private fun slide(seconds: Float) {
            val left = (1f - slid / slideLength).coerceAtLeast(0f)
            val speedNow = slideSpeed * sqrt(left)
            val move = min(speedNow * seconds, slideLength - slid)
            slid += move
            head = Offset(head.x, (head.y + move / glass.height.value).coerceAtMost(1f))
            ease(0f, seconds)
            if (slid >= slideLength - 0.01f || speedNow < 1f || head.y >= 1f) {
                sliding = false
                blending = true
            }
        }

        // How far each wipe had got: from here on, only newer points can clear it.
        private fun noteWipesSoFar() {
            val ours = ownStreaks()
            wipedBefore =
                fog.marks
                    .filterIsInstance<WipeStroke>()
                    .filter { it !in ours }
                    .associateWith { it.points.size }
        }

        // A trail as wide as the drop leaves, narrower where it first broke away.
        // The last point on fog on the way from [from] to [to], if wiped glass lies between.
        private fun edgeBefore(from: Offset, to: Offset): Offset? {
            val steps =
                ((to - from).let { hypot(it.x * glass.width.value, it.y * glass.height.value) } /
                        EdgeStepDp)
                    .toInt()
                    .coerceAtLeast(1)
            var last = from
            for (i in 1..steps) {
                val point = from + (to - from) * (i / steps.toFloat())
                if (fog.isWipedAt(point, glass, wipeRadius)) return last
                last = point
            }
            return null
        }

        private fun streakRadius() =
            (diameter / 2 * if (burst == 0) TopStreakWidth else StreakWidth).dp

        private var fadeLeft = 1f
        private var fadeRate = 0f

        private fun slideProgress() = (slid / slideLength).coerceIn(0f, 1f)

        private fun easeOut(t: Float) = 1f - (1f - t) * (1f - t)

        // Spreading into the film: a little as it slides, the rest as it dissolves.
        private fun spreadNow() =
            when {
                sliding -> SlideSpread * easeOut(slideProgress())
                blending -> SlideSpread + (1f - SlideSpread) * easeOut(merged)
                else -> 0f
            }

        // How far through merging into a wipe's edge, from 0 to 1.
        private val merged: Float
            get() = if (blending) 1f - fadeLeft else 0f

        /** Faded right out. */
        val gone: Boolean
            get() = fadeLeft <= 0f

        /** Fogged over for good: a full breath has taken its streak off the glass. */
        val fogged: Boolean
            get() = lastStreak.let { last -> last != null && fog.marks.none { it === last } }

        /** Starts fading out over [seconds], from [from] at most; none, and it is gone at once. */
        fun startFading(seconds: Float, from: Float = 1f) {
            fadeLeft = minOf(fadeLeft, from)
            if (seconds <= 0f || fadeLeft <= 0f) fadeLeft = 0f else fadeRate = 1f / seconds
        }

        fun fade(seconds: Float) {
            fadeLeft = (fadeLeft - fadeRate * seconds).coerceAtLeast(0f)
            // Merging into the clear glass, it lets go of its teardrop.
            if (blending) ease(0f, seconds)
        }

        /**
         * How much of the drop shows: fading away, and, once it rests, fogged over by any breath
         * since its streak as the breath's front passes it. A running drop clears its own way.
         */
        fun visibility(): Float {
            // Onto a wipe, it thins as it slides, then dissolves with no snap at either end.
            if (sliding) return 1f - SlideThinning * slideProgress()
            if (blending) return (1f - SlideThinning) * (1f - smoothstep(merged))
            if (!stopped || fadeLeft <= 0f) return fadeLeft
            val last = lastStreak ?: return fadeLeft
            val index = fog.marks.indexOfFirst { it === last }
            // Its streak gone: fogged over by a full breath, unless it is being pushed out, when
            // its streak went first and the drop fades after it.
            if (index < 0) return if (fadeRate > 0f) fadeLeft else 0f
            var cover = 0f
            for (i in index + 1 until fog.marks.size) {
                cover = maxOf(cover, fog.marks[i].fogAt(head.y))
            }
            // A breath already under way when it stopped fogs it over as its front rises past.
            for ((breath, before) in fogAtStop) {
                if (before >= 1f || fog.marks.none { it === breath }) continue
                val now = breath.fogAt(head.y)
                cover = maxOf(cover, ((now - before) / (1f - before)).coerceIn(0f, 1f))
            }
            return fadeLeft * (1f - cover)
        }

        // How long it has rested: over [SoftenSeconds] it settles into the fog around it.
        private var rested = 0f

        private val softness: Float
            get() = (rested / SoftenSeconds).coerceIn(0f, 1f)

        /** Stopped, but not yet relaxed into its resting shape and settled into the fog. */
        val relaxing: Boolean
            get() = abs(stretch - RestingStretch) > RelaxedWithin || softness < 1f

        /**
         * Stuck or resting, a drop relaxes towards round, keeping a slight sag; at rest, it
         * softens.
         */
        fun settle(seconds: Float) {
            ease(RestingStretch, seconds)
            if (stopped) rested += seconds
        }

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
                softness =
                    when {
                        // On the wet glass it loses its gloss as it slides.
                        sliding -> (slideProgress() * 2f).coerceAtMost(1f)
                        blending -> 1f
                        stopped -> softness
                        else -> 0f
                    },
                spread = spreadNow(),
            )

        // Shows until a breath drops its streak or a real wipe after it passes over the drop.
        fun stillShows(ours: Set<WipeStroke>): Boolean {
            // Stopped, its streak fogged over by a full breath takes it too; running, it carries
            // on.
            val last = lastStreak
            if (stopped && last != null && fog.marks.none { it === last }) return false
            // Stopped, only a wipe's points since then clear it: it may rest on glass it ran into.
            // A wipe a breath has fogged back over clears nothing.
            val marks = fog.marks
            for (i in marks.indices) {
                val mark = marks[i]
                if (mark !is WipeStroke || mark in ours) continue
                // Stopped, or on a wiped patch by design: only a wipe's newer points clear it.
                val settledOn = stopped || sliding || blending
                val from = if (settledOn) ((wipedBefore[mark] ?: 0) - 1).coerceAtLeast(0) else 0
                if (
                    mark.covers(head, glass, wipeRadius, from = from) &&
                        !fog.foggedOverSince(i, head)
                ) {
                    return false
                }
            }
            return true
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

/**
 * Whether [point] is on glass a wipe has cleared and nothing has fogged over since: a real wipe,
 * not a drop's part-clear trail, and no later breath's fog over it.
 */
internal fun FogState.isWipedAt(point: Offset, glass: DpSize, wipeRadius: Dp): Boolean {
    for (i in marks.indices) {
        val mark = marks[i]
        if (mark !is WipeStroke || mark.clarity < 1f) continue
        if (!mark.covers(point, glass, wipeRadius)) continue
        if (!foggedOverSince(i, point)) return true
    }
    return false
}

/** Whether a breath after the mark at [index] has fogged [point] back over. */
internal fun FogState.foggedOverSince(index: Int, point: Offset): Boolean {
    for (j in index + 1 until marks.size) {
        if (marks[j].fogAt(point.y) >= FoggedOver) return true
    }
    return false
}

/** How much this mark fogs the glass at height [y]: a breath from below, a mist evenly. */
internal fun FogMark.fogAt(y: Float): Float =
    when (this) {
        is Breath -> fogCoverAt(y, level)
        is Mist -> amount
        else -> 0f
    }

// How much of a breath's fog makes glass count as fogged over again.
private const val FoggedOver = 0.9f

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
private const val BlendSeconds = 3.5f
private const val SoftenSeconds = 1.5f
private const val SlideLengths = 1f
private const val MinSlideSpeedDp = 60f
private const val SlideThinning = 0.15f
private const val SlideSpread = 0.45f
private const val EdgeStepDp = 2f

// A wipe's soft edge still looks clear: a drop starts at least this far beyond its brush.
private val StartClearance = 12.dp

private fun smoothstep(t: Float): Float {
    val x = t.coerceIn(0f, 1f)
    return x * x * (3f - 2f * x)
}
