package dev.dimvlachos.lab.core.presentation.components.pageturn

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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

/**
 * The open book's state: which spread is open and the leaf being turned, if any. A finger and a
 * script drive the same calls ([next], [previous], and [dragStart], [dragBy], [dragEnd]), so a
 * scripted drag turns the page exactly as a finger would.
 */
@Stable
class PageTurnState
internal constructor(
    val spreadCount: Int,
    initialSpread: Int,
    private val scope: CoroutineScope,
) {
    private val lastSpread = (spreadCount - 1).coerceAtLeast(0)

    /** The spread lying open; it changes when a turn lands. */
    var spread: Int by mutableIntStateOf(initialSpread.coerceIn(0, lastSpread))
        private set

    internal var phase: TurnPhase by mutableStateOf(TurnPhase.Idle)
        private set

    // Progress of the current pair, 0 at its start and 1 at its end, whichever way it runs.
    private val turn = Animatable(0f)
    private val bend = Animatable(1f)

    // The book's width in pixels, which a drag's distance is measured against; set on layout.
    internal var bookWidthPx: Float = 0f

    // Set between a tap's turn starting and its progress snapping back to 0: the pair is already
    // the new one, so it draws from 0 rather than from where the last turn was.
    private var restarting by mutableStateOf(false)

    private var dragBase = 0f
    private var dragDx = 0f
    private var lastDragProgress = -1f
    private var dragBend = 1f

    internal val pair: TurnPair?
        get() =
            when (val current = phase) {
                TurnPhase.Idle -> null
                is TurnPhase.Dragging -> current.pair
                is TurnPhase.Settling -> current.pair
            }

    /**
     * How far the leaf has turned: 0 lying on the right page of spread [TurnPair.leaf], 1 on the
     * left page of the next. A backward turn is a forward one played in reverse.
     */
    internal val leafProgress: Float
        get() {
            val pair = pair ?: return 0f
            val progress = if (restarting) 0f else turn.value
            return if (pair.forward) progress else 1f - progress
        }

    /** Whether a page is up: under a finger or on its way down. */
    val isTurning: Boolean
        get() = phase != TurnPhase.Idle

    internal val bendDirection: Float
        get() = bend.value

    fun next() = request(forward = true)

    fun previous() = request(forward = false)

    /**
     * Starts a drag once it has moved [dx] px (negative is leftwards, towards the next spread). A
     * page still settling is caught where it is, whichever way the finger goes. Returns false with
     * no page that way.
     */
    fun dragStart(dx: Float): Boolean {
        if (bookWidthPx <= 0f || phase is TurnPhase.Dragging) return false
        val inFlight = phase as? TurnPhase.Settling
        val pair =
            inFlight?.pair
                ?: run {
                    val forward = dx < 0f
                    val target = spread + if (forward) 1 else -1
                    if (target !in 0..lastSpread) return false
                    TurnPair(forward, spread, target)
                }
        phase = TurnPhase.Dragging(pair)
        dragDx = 0f
        lastDragProgress = -1f
        if (inFlight != null) {
            dragBase = if (restarting) 0f else turn.value
            dragBend = bend.value
            scope.launch { turn.stop() }
        } else {
            dragBase = 0f
            dragBend = bendTowards(pair.forward, progressIncreasing = true)
            scope.launch {
                turn.snapTo(0f)
                bend.snapTo(dragBend)
            }
        }
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
        val bendNow = dragBend
        scope.launch {
            turn.snapTo(progress)
            bend.snapTo(bendNow)
        }
    }

    /** Lets go, moving at [velocityPxPerSecond] horizontally: the page finishes or falls back. */
    fun dragEnd(velocityPxPerSecond: Float) {
        val pair = (phase as? TurnPhase.Dragging)?.pair ?: return
        val signed = if (pair.forward) -velocityPxPerSecond else velocityPxPerSecond
        val velocity = dragToProgress(signed, bookWidthPx)
        // Launched after the drag's own snaps, so the progress read here is the last one.
        scope.launch {
            if (shouldCommit(turn.value, velocity)) complete(pair, velocity)
            else cancel(pair, velocity)
        }
    }

    private fun request(forward: Boolean) {
        if (phase is TurnPhase.Dragging) return
        // A page tapped while one is still landing: that one lands at once, this one starts.
        val basis = (phase as? TurnPhase.Settling)?.landing ?: spread
        val target = basis + if (forward) 1 else -1
        if (target !in 0..lastSpread) return
        // The phase changes now, not in the coroutine, so a drag or a tap in between sees this
        // turn and nothing can slip in while the last one's animation is being cancelled.
        val pair = TurnPair(forward, basis, target)
        spread = basis
        phase = TurnPhase.Settling(pair, landing = target)
        restarting = true
        scope.launch {
            turn.snapTo(0f)
            restarting = false
            complete(pair, velocity = 0f)
        }
    }

    // [velocity] is the release speed in turns a second, so a flicked page keeps going.
    private suspend fun complete(pair: TurnPair, velocity: Float) {
        releaseBend(bendTowards(pair.forward, progressIncreasing = true))
        phase = TurnPhase.Settling(pair, landing = pair.to)
        turn.animateTo(
            1f,
            spring(dampingRatio = 1f, stiffness = PageTurnDimens.CommitStiffness),
            initialVelocity = velocity,
        )
        spread = pair.to
        phase = TurnPhase.Idle
        turn.snapTo(0f)
    }

    private suspend fun cancel(pair: TurnPair, velocity: Float) {
        releaseBend(bendTowards(pair.forward, progressIncreasing = false))
        phase = TurnPhase.Settling(pair, landing = pair.from)
        turn.animateTo(
            0f,
            spring(dampingRatio = 1f, stiffness = PageTurnDimens.CancelStiffness),
            initialVelocity = velocity,
        )
        phase = TurnPhase.Idle
    }

    // Off the finger, the bow eases over to the way the page is now going.
    private fun releaseBend(target: Float) {
        scope.launch { bend.animateTo(target, tween(PageTurnDimens.BendReleaseMs)) }
    }
}

@Composable
fun rememberPageTurnState(spreadCount: Int, initialSpread: Int = 0): PageTurnState {
    val scope = rememberCoroutineScope()
    return remember(spreadCount) { PageTurnState(spreadCount, initialSpread, scope) }
}
