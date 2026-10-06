package dev.dimvlachos.lab.core.presentation.components.pageturn

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.geometry.Offset
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sign
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** A turn from spread [from] to its neighbour [to]. */
internal data class TurnPair(val forward: Boolean, val from: Int, val to: Int) {
    // The leaf lies between spread [leaf] and the next one.
    val leaf: Int
        get() = min(from, to)
}

internal sealed interface TurnPhase {
    data object Idle : TurnPhase

    data class Dragging(val pair: TurnPair) : TurnPhase

    // Released, on its way to [landing]: the pair's [TurnPair.to] on a commit, its from on a
    // cancel.
    data class Settling(val pair: TurnPair, val landing: Int) : TurnPhase
}

/**
 * A leaf in the air, as drawn: [t] is 0 lying on the right, 1 on the left; [bend] its bow, [tilt]
 * the lean of its rulings and [grip] where a hand holds or held it.
 */
internal class Flight(
    val leaf: Int,
    val t: Float,
    val bend: Float,
    val tilt: Float = 0f,
    val grip: Grip? = null,
)

private typealias Anim = Animatable<Float, AnimationVector1D>

// Progress of a pair, 0 at its start and 1 at its end, bounded to a turn: a flicked page keeps its
// speed until it meets the spine, and stops there rather than sailing past it and coming back.
private fun newTurn(): Anim = Animatable(0f).apply { updateBounds(0f, 1f) }

/**
 * A leaf's paper as it flies: its bow, a spring pulled by how fast the leaf turns, the lean of its
 * rulings and the hand's hold, both easing out once it is let go. [direction] is the way it last
 * moved, in its own progress: 1 towards the left page.
 */
private class Paper(var direction: Float) {
    var bow by mutableStateOf(Spring(direction * PageTurnDimens.BowRest))
    var tilt by mutableStateOf(Spring(0f))
    // Plain, not state: a hand's hold slides as the leaf is drawn, and the finger it follows is
    // what the drawing watches.
    var grip: Grip? = null
    var hold by mutableStateOf(Spring(0f))
    // The leaf's speed in its own progress a second, smoothed over a couple of frames.
    var velocity = 0f
    var lastT = Float.NaN

    val heldGrip: Grip?
        get() = grip?.copy(weight = hold.value.coerceIn(0f, 1f))
}

/** A leaf let go and on its way down by itself, while the hand has moved on to the next one. */
private class Landing(val pair: TurnPair, val turn: Anim, val paper: Paper)

/**
 * The open book's state: which spread is open, the leaf in hand and any leaves still coming down. A
 * finger and a script drive the same calls ([next], [previous], and [dragStart], [dragTo],
 * [dragEnd]), so a scripted drag turns the page exactly as a finger would.
 *
 * Leafing through quickly, a page let go is still landing when the next one is taken: it goes on
 * landing by itself, so several leaves can be in the air at once, as in a real book.
 */
@Stable
public class PageTurnState
internal constructor(
    /** How many spreads the book has. */
    public val spreadCount: Int,
    initialSpread: Int,
    private val scope: CoroutineScope,
) {
    private val lastSpread = (spreadCount - 1).coerceAtLeast(0)

    /**
     * The spread lying open, counting leaves still landing as landed; it changes when a turn lands
     * or a landing leaf is left to land by itself.
     */
    public var spread: Int by mutableIntStateOf(initialSpread.coerceIn(0, lastSpread))
        private set

    internal var phase: TurnPhase by mutableStateOf(TurnPhase.Idle)
        private set

    // The leaf in hand: its turn and paper. A leaf handed off to land by itself takes its own.
    private var turn by mutableStateOf(newTurn())
    private var paper by mutableStateOf(Paper(direction = 1f))
    private val landings = mutableStateListOf<Landing>()

    /** How the book lies in its box, which a finger is measured against; set on layout. */
    internal var layout: BookLayout? = null

    // Under a finger the leaf is plain state, set by every move as it comes: nothing a coroutine
    // can be cancelled before writing. The animations take over only once the finger lets go.
    // The paper the finger holds (x from the spine, y down the leaf), and how far from the finger
    // it was taken: it stays that far from it, under it when the finger took it.
    private var grab = Offset.Zero
    private var grabOffset = Offset.Zero
    // Where the finger wants the held paper; the leaf is placed under it when it is next looked
    // at, at most once a frame however many moves come in between.
    private var target by mutableStateOf(Offset.Zero)
    private var pinned = 0f
    // A point trailing the finger by a short way, so the finger's recent way is the line from it;
    // the furthest the finger has gone the way it turns the leaf; and which side the leaf's outer
    // edge is on (1: the right).
    private var trail = Offset.Zero
    // Where across the finger took the leaf, if it took it off its stack, and the end of the turn
    // it lay at: it lifts off softly.
    private var liftFrom = Float.NaN
    private var liftedFrom = Float.NaN
    private var furthest = 0f
    private var outwards = 1f
    private var pinnedFor = Offset.Unspecified
    private var pinnedBow = Float.NaN
    private var pinnedTilt = Float.NaN
    private var ticking = false

    internal val pair: TurnPair?
        get() =
            when (val current = phase) {
                TurnPhase.Idle -> null
                is TurnPhase.Dragging -> current.pair
                is TurnPhase.Settling -> current.pair
            }

    /**
     * How far the leaf in hand has turned: 0 lying on the right page of spread [TurnPair.leaf], 1
     * on the left page of the next. A backward turn is a forward one played in reverse.
     */
    internal val leafProgress: Float
        get() {
            val pair = pair ?: return 0f
            return if (phase is TurnPhase.Dragging) placed() else leafTime(pair, turn.value)
        }

    internal val bendDirection: Float
        get() = paper.bow.value

    internal val tilt: Float
        get() = paper.tilt.value

    /** Every leaf in the air: those still landing by themselves, then the one in hand. */
    internal val flights: List<Flight>
        get() = buildList {
            landings.forEach {
                add(it.paper.flight(it.pair.leaf, leafTime(it.pair, it.turn.value)))
            }
            pair?.let { add(paper.flight(it.leaf, leafProgress, tilt)) }
        }

    /** Whether a page is up: under a finger, or on its way down. */
    public val isTurning: Boolean
        get() = phase != TurnPhase.Idle || landings.isNotEmpty()

    /** Whether a finger holds a page; taps wait until it lets go. */
    public val isDragging: Boolean
        get() = phase is TurnPhase.Dragging

    /**
     * The spread the book comes to rest at once every page in the air lands: where the next tap
     * turns from, so taps can follow each other before the last page is down.
     */
    public val destination: Int
        get() = (phase as? TurnPhase.Settling)?.landing ?: spread

    /**
     * Turns to the next spread, as a tap on the right page does; nothing while a finger holds a
     * page, or on the last spread.
     */
    public fun next() {
        request(forward = true)
    }

    /**
     * Turns back to the previous spread, as a tap on the left page does; nothing while a finger
     * holds a page, or on the first spread.
     */
    public fun previous() {
        request(forward = false)
    }

    /**
     * Starts a drag that went down at [down] and has now reached [position], in the book's box (a
     * leftwards drag turns towards the next spread). The paper under [down] is taken and from then
     * on stays under the finger, wherever it goes. A page landing the way the drag goes is left to
     * land and the drag takes the next leaf; a page in flight any other way, or the leaf the drag
     * would turn while it is still coming down, is caught where it is. Returns false with no page
     * that way.
     */
    public fun dragStart(down: Offset, position: Offset): Boolean {
        val layout = layout ?: return false
        if (phase is TurnPhase.Dragging) return false
        val forward = position.x < down.x
        handOffIfLandingTowards(forward)
        val inFlight = phase as? TurnPhase.Settling
        val pair: TurnPair
        var start: Float
        if (inFlight != null) {
            pair = inFlight.pair
            start = turn.value
        } else {
            val target = spread + if (forward) 1 else -1
            if (target !in 0..lastSpread) return false
            val landing = landings.firstOrNull { it.pair.leaf == min(spread, target) }
            if (landing != null) {
                catch(landing)
                pair = landing.pair
                start = turn.value
            } else {
                pair = TurnPair(forward, spread, target)
                start = 0f
                paper = Paper(direction = if (forward) 1f else -1f)
                // Taken by hand, it leaves its page with the hand leading.
                paper.bow = Spring(-paper.direction * PageTurnDimens.HoldBow)
            }
        }
        val geometry = layout.geometry
        val finger = down - Offset(layout.left, layout.top)
        val t = leafTime(pair, start)
        val holding = paper.heldGrip
        val frame =
            turnFrame(
                t,
                geometry.page,
                paper.bow.value,
                height = geometry.height,
                tilt = paper.tilt.value,
                grip = holding,
            )
        // The paper under the finger; or, if the finger is off the leaf, the paper as far from
        // the spine as the finger is, at its height.
        grab =
            frame.unproject(finger, geometry)
                ?: Offset(
                    abs(finger.x - geometry.spineX).coerceIn(0f, geometry.page),
                    finger.y.coerceIn(0f, geometry.height),
                )
        val offTheStack = inFlight == null && start == 0f
        liftedFrom = if (offTheStack) t else Float.NaN
        // Measured as the finger's place is solved for, so the leaf stays put until it moves.
        grabOffset =
            Offset(
                heldSeenX(
                    grab,
                    t,
                    paper.bow.value,
                    paper.tilt.value,
                    holding,
                    geometry,
                    liftedFrom = liftedFrom,
                ) - finger.x,
                frame.project(grab.x, grab.y, geometry).y - finger.y,
            )
        target = finger + grabOffset
        trail = finger
        furthest = finger.x
        liftFrom = if (offTheStack) finger.x else Float.NaN
        // The leaf's outer edge lies right of the spine while the leaf is on the right.
        outwards = if (t < 0.5f) 1f else -1f
        paper.grip = Grip(grab.x / geometry.page, grab.y / geometry.height)
        paper.hold = Spring(1f)
        pinned = t
        pinnedFor = Offset.Unspecified
        phase = TurnPhase.Dragging(pair)
        // A caught leaf stops where it is.
        val caught = turn
        scope.launch { caught.stop() }
        dragTo(position)
        tick()
        return true
    }

    /** Moves the finger to [position], in the book's box: the paper it holds follows. */
    public fun dragTo(position: Offset) {
        val layout = layout ?: return
        if (phase !is TurnPhase.Dragging) return
        val finger = position - Offset(layout.left, layout.top)
        val page = layout.geometry.page
        // Under a hand the paper leads the way the finger goes; it turns the other way only once
        // the finger has come back a little, not for every wobble of a finger moving up or down.
        val leftwards = paper.direction > 0f
        furthest = if (leftwards) min(furthest, finger.x) else max(furthest, finger.x)
        if (abs(finger.x - furthest) > PageTurnDimens.TurnBack * page) {
            paper.direction = -paper.direction
            furthest = finger.x
        }
        // The trailing point follows the finger on a string.
        val behind = finger - trail
        val reach = PageTurnDimens.PullWindow * page
        if (behind.getDistance() > reach) trail = finger - behind * (reach / behind.getDistance())
        target = finger + grabOffset
    }

    /**
     * Lets go, moving at [velocity] px a second: the page finishes or falls back, by where it is
     * and how fast it goes the way it turns.
     */
    public fun dragEnd(velocity: Offset) {
        val pair = (phase as? TurnPhase.Dragging)?.pair ?: return
        val layout = layout
        val placed = placed()
        val progress = if (pair.forward) placed else 1f - placed
        // Its speed in turns a second: where the paper would be a moment on, held on its way.
        val leafVelocity =
            if (layout == null) 0f
            else {
                val ahead =
                    pinTurn(
                        grab,
                        liftedX((target + velocity * ReleaseLookahead).x, layout.geometry),
                        placed,
                        paper.bow.value,
                        paper.tilt.value,
                        paper.heldGrip ?: Grip(1f, 0.5f),
                        layout.geometry,
                        liftedFrom = liftedFrom,
                    )
                (ahead - placed) / ReleaseLookahead
            }
        val pairVelocity = if (pair.forward) leafVelocity else -leafVelocity
        val commit = shouldCommit(progress, pairVelocity)
        val leafTurn = turn
        // One coroutine hands the leaf to its animation and lands it: the drag's state shows
        // until it runs, so nothing in between can leave a stale frame.
        scope.launch {
            leafTurn.snapTo(progress)
            if (commit) complete(pair, pairVelocity, leafTurn)
            else cancel(pair, pairVelocity, leafTurn)
        }
    }

    // The finger's recent way, for a finger whose held paper goes to [to]: x towards the leaf's
    // outer edge, y down.
    private fun pull(to: Offset): Offset {
        val finger = to - grabOffset
        return Offset((finger.x - trail.x) * outwards, finger.y - trail.y)
    }

    // The leaf in hand, turned and leant so the paper held lies under the finger: solved again
    // only once the finger or the paper's bow has moved. It changes nothing a frame reads, so it
    // can be worked out while one is drawn.
    // Where across the held paper goes for a finger whose paper would go to [x]: a leaf taken off
    // its stack trails the finger a little as it lifts off (see liftLag).
    private fun liftedX(x: Float, geometry: PageGeometry): Float {
        if (liftFrom.isNaN()) return x
        val travel = (liftFrom - (x - grabOffset.x)) * outwards
        return x + outwards * liftLag(travel, PageTurnDimens.LiftOff * geometry.page)
    }

    private fun placed(): Float {
        val layout = layout ?: return pinned
        val geometry = layout.geometry
        val target = target
        // The finger's height is where along the edge the hand holds the leaf: it slides there.
        val y = target.y.coerceIn(0f, geometry.height)
        if (y != grab.y) {
            grab = Offset(grab.x, y)
            paper.grip = Grip(grab.x / geometry.page, y / geometry.height)
        }
        val grip = paper.heldGrip ?: return pinned
        val bow = paper.bow.value
        val tilt = paper.tilt.value
        if (target == pinnedFor && abs(bow - pinnedBow) <= BowStill && tilt == pinnedTilt) {
            return pinned
        }
        pinned =
            pinTurn(
                grab,
                liftedX(target.x, geometry),
                pinned,
                bow,
                tilt,
                grip,
                geometry,
                liftedFrom = liftedFrom,
            )
        pinnedFor = target
        pinnedBow = bow
        pinnedTilt = tilt
        return pinned
    }

    private fun request(forward: Boolean) {
        if (phase is TurnPhase.Dragging) return
        handOffIfLandingTowards(forward)
        (phase as? TurnPhase.Settling)?.let { inFlight ->
            // A page in the air the other way: the tap sends it where it points.
            settle(inFlight.pair, towards = forward)
            return
        }
        val target = spread + if (forward) 1 else -1
        if (target !in 0..lastSpread) return
        val landing = landings.firstOrNull { it.pair.leaf == min(spread, target) }
        if (landing != null) {
            catch(landing)
            settle(landing.pair, towards = forward)
            return
        }
        paper = Paper(direction = if (forward) 1f else -1f)
        settle(TurnPair(forward, spread, target), towards = forward)
    }

    // Sends [pair], the leaf in hand, the way a tap points: on if it runs that way, back if not.
    // The phase is set now, so a drag or tap before the coroutine runs sees the turn.
    private fun settle(pair: TurnPair, towards: Boolean) {
        val commit = pair.forward == towards
        phase = TurnPhase.Settling(pair, landing = if (commit) pair.to else pair.from)
        val leafTurn = turn
        tick()
        scope.launch {
            if (commit) complete(pair, 0f, leafTurn) else cancel(pair, 0f, leafTurn)
        }
    }

    // A page already on its way to the side the hand now sends pages to (a forward turn landing on
    // the left, or a backward one falling back there; and the mirror) is left to land by itself,
    // and the hand is free for the next leaf. The spread moves on now; the leaf follows in the air.
    private fun handOffIfLandingTowards(forward: Boolean) {
        val inFlight = phase as? TurnPhase.Settling ?: return
        val pair = inFlight.pair
        val headingLeft = (inFlight.landing == pair.to) == pair.forward
        if (headingLeft != forward) return
        landings += Landing(inFlight.pair, turn, paper)
        spread = inFlight.landing
        phase = TurnPhase.Idle
        turn = newTurn()
        paper = Paper(direction = if (forward) 1f else -1f)
    }

    // Takes a leaf that is still landing back into hand, where it is.
    private fun catch(landing: Landing) {
        landings.remove(landing)
        turn = landing.turn
        paper = landing.paper
        spread = landing.pair.from
    }

    // [velocity] is the release speed in turns a second, so a flicked page keeps going. The leaf's
    // own animations come along: by the time this runs, the hand may have moved on from it.
    private suspend fun complete(pair: TurnPair, velocity: Float, leafTurn: Anim) {
        if (leafTurn === turn) phase = TurnPhase.Settling(pair, landing = pair.to)
        leafTurn.animateTo(
            1f,
            spring(dampingRatio = 1f, stiffness = PageTurnDimens.CommitStiffness),
            initialVelocity = velocity,
        )
        landed(leafTurn) {
            spread = pair.to
            phase = TurnPhase.Idle
            leafTurn.snapTo(0f)
        }
    }

    private suspend fun cancel(pair: TurnPair, velocity: Float, leafTurn: Anim) {
        if (leafTurn === turn) phase = TurnPhase.Settling(pair, landing = pair.from)
        leafTurn.animateTo(
            0f,
            spring(dampingRatio = 1f, stiffness = PageTurnDimens.CancelStiffness),
            initialVelocity = velocity,
        )
        landed(leafTurn) { phase = TurnPhase.Idle }
    }

    // A leaf has come down: still in hand, the book settles on it; handed off, it just leaves the
    // air, the spread having moved on when it was let go.
    private suspend fun landed(leafTurn: Anim, inHand: suspend () -> Unit) {
        if (leafTurn === turn) inHand() else landings.removeAll { it.turn === leafTurn }
    }

    // Steps the paper of every leaf in the air, a frame at a time, while any is.
    private fun tick() {
        if (ticking) return
        ticking = true
        scope.launch {
            try {
                var last = withFrameNanos { it }
                while (isTurning) {
                    val now = withFrameNanos { it }
                    val dt = ((now - last) / 1e9f).coerceIn(0f, MaxFrameSeconds)
                    last = now
                    if (dt > 0f) step(dt)
                }
            } finally {
                ticking = false
            }
        }
    }

    private fun step(dt: Float) {
        landings.forEach { it.paper.step(leafTime(it.pair, it.turn.value), dt, held = false) }
        val pair = pair ?: return
        val dragging = phase is TurnPhase.Dragging
        val t = leafProgress
        paper.step(t, dt, held = dragging)
        // In hand, the rulings lean after the hold and the finger's pull, as a spring.
        val layout = layout
        val grip = paper.heldGrip
        if (dragging && layout != null && grip != null) {
            val lean = leanFor(grip, pull(target), t, layout.geometry)
            paper.tilt = paper.tilt.step(lean, dt, PageTurnDimens.TiltFrequency, damping = 1f)
        }
    }
}

// How far ahead a release looks to measure its speed, in seconds.
private const val ReleaseLookahead = 0.03f

// A frame longer than this (a stall, a test clock's jump) is stepped as this long.
private const val MaxFrameSeconds = 0.05f

// The paper a frame on: the bow pulled by the leaf's speed, and once let go, the lean and the
// hand's hold easing out.
private fun Paper.step(t: Float, dt: Float, held: Boolean) {
    if (!lastT.isNaN()) {
        val now = (t - lastT) / dt
        velocity += (now - velocity) * VelocitySmoothing
    }
    lastT = t
    if (!held && abs(velocity) > TurningVelocity) direction = sign(velocity)
    // In hand the bow rolls over calmly when the finger turns back; let go, it whips and sways.
    bow =
        bow.step(
            bowTarget(direction, velocity, held),
            dt,
            if (held) PageTurnDimens.HeldFlexFrequency else PageTurnDimens.FlexFrequency,
            if (held) 1f else PageTurnDimens.FlexDamping,
        )
    if (!held) {
        tilt = tilt.step(0f, dt, PageTurnDimens.StraightenFrequency, damping = 1f)
        hold = hold.step(0f, dt, PageTurnDimens.StraightenFrequency, damping = 1f)
    }
}

private fun Paper.flight(leaf: Int, t: Float, lean: Float = tilt.value): Flight =
    // A lean too small to see is none: the leaf is drawn by its upright strips.
    Flight(leaf, t, bow.value, if (abs(lean) < LeanUnseen) 0f else lean, heldGrip)

private const val VelocitySmoothing = 0.5f
// A bow that moved less than this in a frame leaves the held paper where it is.
private const val BowStill = 0.002f
private const val LeanUnseen = 0.004f

// Slower than this (turns a second) the leaf is taken as still, and keeps its last direction.
private const val TurningVelocity = 0.05f

// A pair's progress as the leaf's own: 0 on the right page, 1 on the left.
private fun leafTime(pair: TurnPair, progress: Float): Float =
    if (pair.forward) progress else 1f - progress

/**
 * A [PageTurnState] for a book of [spreadCount] spreads, open at [initialSpread]. A new count makes
 * a new book.
 */
@Composable
public fun rememberPageTurnState(spreadCount: Int, initialSpread: Int = 0): PageTurnState {
    val scope = rememberCoroutineScope()
    return remember(spreadCount) { PageTurnState(spreadCount, initialSpread, scope) }
}
