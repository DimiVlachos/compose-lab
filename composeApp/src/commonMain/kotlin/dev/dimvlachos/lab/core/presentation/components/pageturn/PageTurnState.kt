package dev.dimvlachos.lab.core.presentation.components.pageturn

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlin.math.min
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

/** A leaf in the air, as drawn: [t] is 0 lying on the right, 1 on the left; [bend] its bow. */
internal class Flight(val leaf: Int, val t: Float, val bend: Float)

private typealias Anim = Animatable<Float, AnimationVector1D>

// Progress of a pair, 0 at its start and 1 at its end, bounded to a turn: a flicked page keeps its
// speed until it meets the spine, and stops there rather than sailing past it and coming back.
private fun newTurn(): Anim = Animatable(0f).apply { updateBounds(0f, 1f) }

/** A leaf let go and on its way down by itself, while the hand has moved on to the next one. */
private class Landing(val pair: TurnPair, val turn: Anim, val bend: Anim)

/**
 * The open book's state: which spread is open, the leaf in hand and any leaves still coming down. A
 * finger and a script drive the same calls ([next], [previous], and [dragStart], [dragBy],
 * [dragEnd]), so a scripted drag turns the page exactly as a finger would.
 *
 * Leafing through quickly, a page let go is still landing when the next one is taken: it goes on
 * landing by itself, so several leaves can be in the air at once, as in a real book.
 */
@Stable
class PageTurnState
internal constructor(
    val spreadCount: Int,
    initialSpread: Int,
    private val scope: CoroutineScope,
) {
    private val lastSpread = (spreadCount - 1).coerceAtLeast(0)

    /**
     * The spread lying open, counting leaves still landing as landed; it changes when a turn lands
     * or a landing leaf is left to land by itself.
     */
    var spread: Int by mutableIntStateOf(initialSpread.coerceIn(0, lastSpread))
        private set

    internal var phase: TurnPhase by mutableStateOf(TurnPhase.Idle)
        private set

    // The leaf in hand: its turn and bow. A leaf handed off to land by itself takes its own along.
    private var turn by mutableStateOf(newTurn())
    private var bend by mutableStateOf(Animatable(1f))
    private val landings = mutableStateListOf<Landing>()

    // The book's width in pixels, which a drag's distance is measured against; set on layout.
    internal var bookWidthPx: Float = 0f

    // Under a finger the leaf is plain state, set by every move as it comes: nothing a coroutine
    // can be cancelled before writing. The animations take over only once the finger lets go.
    private var dragProgress by mutableFloatStateOf(0f)
    private var dragBend by mutableFloatStateOf(1f)
    private var dragBase = 0f
    private var dragDx = 0f
    private var lastDragProgress = -1f

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
            return leafTime(pair, if (phase is TurnPhase.Dragging) dragProgress else turn.value)
        }

    internal val bendDirection: Float
        get() = if (phase is TurnPhase.Dragging) dragBend else bend.value

    /** Every leaf in the air: those still landing by themselves, then the one in hand. */
    internal val flights: List<Flight>
        get() = buildList {
            landings.forEach {
                add(Flight(it.pair.leaf, leafTime(it.pair, it.turn.value), it.bend.value))
            }
            pair?.let { add(Flight(it.leaf, leafProgress, bendDirection)) }
        }

    /** Whether a page is up: under a finger, or on its way down. */
    val isTurning: Boolean
        get() = phase != TurnPhase.Idle || landings.isNotEmpty()

    fun next() = request(forward = true)

    fun previous() = request(forward = false)

    /**
     * Starts a drag once it has moved [dx] px (negative is leftwards, towards the next spread). A
     * page landing the way the drag goes is left to land and the drag takes the next leaf; a page
     * in flight any other way, or the leaf the drag would turn while it is still coming down, is
     * caught where it is. Returns false with no page that way.
     */
    fun dragStart(dx: Float): Boolean {
        if (bookWidthPx <= 0f || phase is TurnPhase.Dragging) return false
        val forward = dx < 0f
        handOffIfLandingTowards(forward)
        val inFlight = phase as? TurnPhase.Settling
        val pair: TurnPair
        if (inFlight != null) {
            pair = inFlight.pair
            dragBase = turn.value
            dragBend = bend.value
        } else {
            val target = spread + if (forward) 1 else -1
            if (target !in 0..lastSpread) return false
            val landing = landings.firstOrNull { it.pair.leaf == min(spread, target) }
            if (landing != null) {
                catch(landing)
                pair = landing.pair
                dragBase = turn.value
                dragBend = bend.value
            } else {
                pair = TurnPair(forward, spread, target)
                dragBase = 0f
                dragBend = bendTowards(forward, progressIncreasing = true)
            }
        }
        phase = TurnPhase.Dragging(pair)
        dragDx = 0f
        lastDragProgress = -1f
        dragProgress = dragBase
        // A caught leaf stops where it is.
        val held = turn
        scope.launch { held.stop() }
        return true
    }

    /** Moves the drag by [deltaPx] horizontally. */
    fun dragBy(deltaPx: Float) {
        val pair = (phase as? TurnPhase.Dragging)?.pair ?: return
        dragDx += deltaPx
        val signedDx = if (pair.forward) -dragDx else dragDx
        val progress = (dragBase + dragToProgress(signedDx, bookWidthPx)).coerceIn(0f, 1f)
        if (lastDragProgress >= 0f && progress != lastDragProgress) {
            dragBend = bendAfterDrag(dragBend, pair.forward, progress - lastDragProgress)
        }
        lastDragProgress = progress
        dragProgress = progress
    }

    /** Lets go, moving at [velocityPxPerSecond] horizontally: the page finishes or falls back. */
    fun dragEnd(velocityPxPerSecond: Float) {
        val pair = (phase as? TurnPhase.Dragging)?.pair ?: return
        val signed = if (pair.forward) -velocityPxPerSecond else velocityPxPerSecond
        val velocity = dragToProgress(signed, bookWidthPx)
        val progress = dragProgress
        val bendNow = dragBend
        val commit = shouldCommit(progress, velocity)
        val leafTurn = turn
        val leafBend = bend
        // One coroutine hands the leaf to its animations and lands it: the drag's state shows
        // until it runs, so nothing in between can leave a stale frame.
        scope.launch {
            leafTurn.snapTo(progress)
            leafBend.snapTo(bendNow)
            if (commit) complete(pair, velocity, leafTurn, leafBend)
            else cancel(pair, velocity, leafTurn, leafBend)
        }
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
        settle(TurnPair(forward, spread, target), towards = forward)
    }

    // Sends [pair], the leaf in hand, the way a tap points: on if it runs that way, back if not.
    // The phase is set now, so a drag or tap before the coroutine runs sees the turn.
    private fun settle(pair: TurnPair, towards: Boolean) {
        val commit = pair.forward == towards
        phase = TurnPhase.Settling(pair, landing = if (commit) pair.to else pair.from)
        val leafTurn = turn
        val leafBend = bend
        scope.launch {
            if (commit) complete(pair, 0f, leafTurn, leafBend)
            else cancel(pair, 0f, leafTurn, leafBend)
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
        landings += Landing(inFlight.pair, turn, bend)
        spread = inFlight.landing
        phase = TurnPhase.Idle
        turn = newTurn()
        bend = Animatable(1f)
    }

    // Takes a leaf that is still landing back into hand, where it is.
    private fun catch(landing: Landing) {
        landings.remove(landing)
        turn = landing.turn
        bend = landing.bend
        spread = landing.pair.from
    }

    // [velocity] is the release speed in turns a second, so a flicked page keeps going. The leaf's
    // own animations come along: by the time this runs, the hand may have moved on from it.
    private suspend fun complete(pair: TurnPair, velocity: Float, leafTurn: Anim, leafBend: Anim) {
        releaseBend(leafBend, bendTowards(pair.forward, progressIncreasing = true))
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

    private suspend fun cancel(pair: TurnPair, velocity: Float, leafTurn: Anim, leafBend: Anim) {
        releaseBend(leafBend, bendTowards(pair.forward, progressIncreasing = false))
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

    // Off the finger, the bow eases over to the way the page is now going.
    private fun releaseBend(leafBend: Anim, target: Float) {
        scope.launch { leafBend.animateTo(target, tween(PageTurnDimens.BendReleaseMs)) }
    }
}

// A pair's progress as the leaf's own: 0 on the right page, 1 on the left.
private fun leafTime(pair: TurnPair, progress: Float): Float =
    if (pair.forward) progress else 1f - progress

@Composable
fun rememberPageTurnState(spreadCount: Int, initialSpread: Int = 0): PageTurnState {
    val scope = rememberCoroutineScope()
    return remember(spreadCount) { PageTurnState(spreadCount, initialSpread, scope) }
}
