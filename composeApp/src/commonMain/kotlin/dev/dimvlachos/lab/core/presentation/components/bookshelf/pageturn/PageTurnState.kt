package dev.dimvlachos.lab.core.presentation.components.bookshelf.pageturn

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
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

private const val ReleaseLookahead = 0.03f
private const val MaxFrameSeconds = 0.05f
private const val VelocitySmoothing = 0.5f
private const val BowStill = 0.002f
private const val LeanUnseen = 0.004f
private const val TurningVelocity = 0.05f
private const val RiffleGapFrames = 5
private const val RiffleWaitFrames = 90
private const val HalfTurn = 0.5f

internal data class TurnPair(val forward: Boolean, val from: Int, val to: Int) {
    val leaf: Int
        get() = min(from, to)
}

internal sealed interface TurnPhase {
    data object Idle : TurnPhase

    data class Dragging(val pair: TurnPair) : TurnPhase

    data class Settling(val pair: TurnPair, val landing: Int) : TurnPhase
}

internal class Flight(
    val leaf: Int,
    val t: Float,
    val bend: Float,
    val tilt: Float = 0f,
    val grip: Grip? = null,
)

private typealias Anim = Animatable<Float, AnimationVector1D>

private fun newTurn(): Anim = Animatable(0f).apply { updateBounds(0f, 1f) }

private class Paper(var direction: Float) {
    var bow by mutableStateOf(Spring(direction * PageTurnDimens.BowRest))
    var tilt by mutableStateOf(Spring(0f))
    var grip: Grip? = null
    var hold by mutableStateOf(Spring(0f))
    var velocity = 0f
    var lastT = Float.NaN

    val heldGrip: Grip?
        get() = grip?.copy(weight = hold.value.coerceIn(0f, 1f))
}

internal data class Sheets(val left: Int, val right: Int)

private class Landing(val pair: TurnPair, val turn: Anim, val paper: Paper)

@Stable
internal class PageTurnState(private val scope: CoroutineScope) {
    var page by mutableIntStateOf(0)
        private set

    var livePage by mutableStateOf<Int?>(null)
        private set

    var following by mutableStateOf(true)
        private set

    var seeks by mutableIntStateOf(0)
        private set

    var canTurn: (leaf: Int) -> Boolean = { true }

    var onJump: ((from: Int, to: Int) -> Unit)? = null

    var riffleSpan by mutableStateOf<IntRange?>(null)
        private set

    var chapterKey by mutableStateOf<Int?>(null)
        private set

    var bridgeSheets: Pair<Sheets, Sheets>? = null

    private var riffleFrom by mutableStateOf<Int?>(null)

    var riffleCovered by mutableStateOf(false)
        private set

    val bookmarkPage: Int?
        get() = riffleFrom ?: livePage

    var layout: BookLayout? = null

    internal var phase: TurnPhase by mutableStateOf(TurnPhase.Idle)
        private set

    private var pageCount by mutableIntStateOf(0)
    private var turn by mutableStateOf(newTurn())
    private var paper by mutableStateOf(Paper(direction = 1f))
    private val landings = mutableStateListOf<Landing>()
    private var grab = Offset.Zero
    private var grabOffset = Offset.Zero
    private var target by mutableStateOf(Offset.Zero)
    private var pinned = 0f
    private var origin = Offset.Zero
    private var liftFrom = Float.NaN
    private var liftedFrom = Float.NaN
    private var furthest = 0f
    private var outwards = 1f
    private var pinnedFor = Offset.Unspecified
    private var pinnedBow = Float.NaN
    private var pinnedTilt = Float.NaN
    private var ticking: Job? = null
    private var riffling: Job? = null

    val lastPage: Int
        get() = (pageCount - 1).coerceAtLeast(0)

    private val pair: TurnPair?
        get() =
            when (val current = phase) {
                TurnPhase.Idle -> null
                is TurnPhase.Dragging -> current.pair
                is TurnPhase.Settling -> current.pair
            }

    private val leafProgress: Float
        get() {
            val pair = pair ?: return 0f
            return if (phase is TurnPhase.Dragging) placed() else leafTime(pair, turn.value)
        }

    val flights: List<Flight>
        get() = buildList {
            landings.forEach {
                add(it.paper.flight(it.pair.leaf, leafTime(it.pair, it.turn.value)))
            }
            pair?.let { add(paper.flight(it.leaf, leafProgress, paper.tilt.value)) }
        }

    val flyingLeaves: List<Int>
        get() = landings.map { it.pair.leaf } + listOfNotNull(pair?.leaf)

    val isTurning: Boolean
        get() = phase != TurnPhase.Idle || landings.isNotEmpty()

    val isDragging: Boolean
        get() = phase is TurnPhase.Dragging

    val destination: Int
        get() = (phase as? TurnPhase.Settling)?.landing ?: page

    val isBrowsing: Boolean
        get() = !following && livePage.let { it != null && it != destination }

    val isRiffling: Boolean
        get() = riffleSpan != null

    fun restingPages(flights: List<Flight>): Pair<Int, Int> {
        val flying = flights.mapTo(HashSet()) { it.leaf }
        val leaves = lastPage
        val left = (page - 1 downTo 0).firstOrNull { it !in flying } ?: -1
        val right = (page until leaves).firstOrNull { it !in flying } ?: leaves
        return left to right
    }

    fun sync(key: Int, count: Int, live: Int?) {
        val clamped = live?.coerceIn(0, (count - 1).coerceAtLeast(0))
        if (key != chapterKey) {
            stop()
            chapterKey = key
            pageCount = count
            page = clamped ?: 0
            livePage = clamped
            following = true
            return
        }
        pageCount = count
        livePage = clamped
        if (page > lastPage) page = lastPage
        followNarration()
    }

    fun seekRequested() {
        seeks++
    }

    fun reset() {
        stop()
        chapterKey = null
        following = true
    }

    fun adopt(key: Int, count: Int, at: Int, live: Int?) {
        chapterKey = key
        pageCount = count
        page = at.coerceIn(0, lastPage)
        livePage = live?.coerceIn(0, lastPage)
        followNarration()
    }

    fun riffle(key: Int, count: Int, from: Int, to: Int, onLanded: () -> Unit) {
        stop()
        chapterKey = key
        pageCount = count
        page = from.coerceIn(0, lastPage)
        val target = to.coerceIn(0, lastPage)
        livePage = target
        following = true
        riffleSpan = min(page, target)..max(page, target)
        riffleFrom = page
        riffling = scope.launch {
            val forward = target > page
            val leaves = min(page, target) until max(page, target)
            var waited = 0
            while (waited++ < RiffleWaitFrames && !leaves.all { canTurn(it) }) withFrameNanos {}
            launch {
                while (flights.none { if (forward) it.t >= HalfTurn else it.t <= HalfTurn }) {
                    if (!isTurning && destination == target) break
                    withFrameNanos {}
                }
                riffleFrom = null
                riffleCovered = true
            }
            while (destination != target) {
                val next = destination + if (forward) 1 else -1
                if (!awaitTurnable(min(destination, next)) || !request(forward)) {
                    halt()
                    page = target
                    break
                }
                repeat(RiffleGapFrames) { withFrameNanos {} }
            }
            while (isTurning) withFrameNanos {}
            riffleFrom = null
            riffleCovered = false
            riffleSpan = null
            onLanded()
        }
    }

    private suspend fun awaitTurnable(leaf: Int): Boolean {
        repeat(RiffleWaitFrames) {
            if (canTurn(leaf)) return true
            withFrameNanos {}
        }
        return canTurn(leaf)
    }

    fun tap(forward: Boolean) {
        if (isRiffling) return
        val wasFollowing = following
        following = false
        if (!request(forward)) following = wasFollowing
    }

    fun backToNarration() {
        following = true
        followNarration()
    }

    fun dragStart(down: Offset, position: Offset): Boolean {
        val layout = layout ?: return false
        if (phase is TurnPhase.Dragging || isRiffling) return false
        val forward = position.x < down.x
        handOffIfLandingTowards(forward)
        val inFlight = phase as? TurnPhase.Settling
        val pair: TurnPair
        val start: Float
        if (inFlight != null) {
            pair = inFlight.pair
            start = turn.value
        } else {
            val target = page + if (forward) 1 else -1
            if (target !in 0..lastPage || !canTurn(min(page, target))) return false
            val landing = landings.firstOrNull { it.pair.leaf == min(page, target) }
            if (landing != null) {
                catch(landing)
                pair = landing.pair
                start = turn.value
            } else {
                pair = TurnPair(forward, page, target)
                start = 0f
                paper = Paper(direction = if (forward) 1f else -1f)
                paper.bow = Spring(-paper.direction * PageTurnDimens.HoldBow)
            }
        }
        following = false
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
                restLift = geometry.restLift,
            )
        grab =
            frame.unproject(finger, geometry)
                ?: Offset(
                    abs(finger.x - geometry.spineX).coerceIn(0f, geometry.page),
                    finger.y.coerceIn(0f, geometry.height),
                )
        val offTheStack = inFlight == null && start == 0f
        liftedFrom = if (offTheStack) t else Float.NaN
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
        origin = finger
        furthest = finger.x
        liftFrom = if (offTheStack) finger.x else Float.NaN
        outwards = if (t < 0.5f) 1f else -1f
        paper.grip = Grip(grab.x / geometry.page, grab.y / geometry.height)
        paper.hold = Spring(1f)
        pinned = t
        pinnedFor = Offset.Unspecified
        phase = TurnPhase.Dragging(pair)
        val caught = turn
        scope.launch { caught.stop() }
        dragTo(position)
        tick()
        return true
    }

    fun dragTo(position: Offset) {
        val layout = layout ?: return
        if (phase !is TurnPhase.Dragging) return
        val finger = position - Offset(layout.left, layout.top)
        val width = layout.geometry.page
        val leftwards = paper.direction > 0f
        furthest = if (leftwards) min(furthest, finger.x) else max(furthest, finger.x)
        if (abs(finger.x - furthest) > PageTurnDimens.TurnBack * width) {
            paper.direction = -paper.direction
            furthest = finger.x
        }
        target = finger + grabOffset
    }

    fun dragEnd(velocity: Offset, flingReach: Offset = Offset.Zero) {
        val pair = (phase as? TurnPhase.Dragging)?.pair ?: return
        val layout = layout
        val placed = placed()
        val progress = if (pair.forward) placed else 1f - placed
        fun turnAt(x: Float): Float =
            layout?.let {
                pinTurn(
                    grab,
                    x,
                    placed,
                    paper.bow.value,
                    paper.tilt.value,
                    paper.heldGrip ?: Grip(1f, 0.5f),
                    it.geometry,
                    liftedFrom = liftedFrom,
                )
            } ?: placed
        val base = turnAt(target.x)
        val leafVelocity =
            (turnAt((target + velocity * ReleaseLookahead).x) - base) / ReleaseLookahead
        val flung =
            progress +
                (turnAt(target.x + flingReach.x) - base).let {
                    if (pair.forward) it else -it
                }
        val pairVelocity = if (pair.forward) leafVelocity else -leafVelocity
        val commit = shouldCommit(progress, pairVelocity, flung)
        val leafTurn = turn
        scope.launch {
            leafTurn.snapTo(progress)
            if (commit) complete(pair, pairVelocity, leafTurn)
            else cancel(pair, pairVelocity, leafTurn)
        }
    }

    private fun followNarration() {
        val live = livePage ?: return
        if (!following || phase is TurnPhase.Dragging || isRiffling) return
        val from = destination
        if (live == from) return
        val forward = live > from
        val jump = onJump
        if (jump != null && abs(live - from) > 1) {
            if (phase == TurnPhase.Idle && landings.isEmpty()) jump(page, live)
            return
        }
        if (abs(live - from) > PageTurnDimens.FollowRiffle) {
            stop()
            page = live + if (forward) -1 else 1
        }
        if (!request(forward)) {
            stop()
            page = live
        }
    }

    private fun pull(to: Offset): Offset {
        val finger = to - grabOffset
        return Offset((finger.x - origin.x) * outwards, finger.y - origin.y)
    }

    private fun liftedX(x: Float, geometry: PageGeometry): Float {
        if (liftFrom.isNaN()) return x
        val travel = (liftFrom - (x - grabOffset.x)) * outwards
        return x + outwards * liftLag(travel, PageTurnDimens.LiftOff * geometry.page)
    }

    private fun placed(): Float {
        val layout = layout ?: return pinned
        val geometry = layout.geometry
        val target = target
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

    private fun request(forward: Boolean): Boolean {
        if (phase is TurnPhase.Dragging) return false
        handOffIfLandingTowards(forward)
        val inFlight = phase as? TurnPhase.Settling
        val target = page + if (forward) 1 else -1
        val landing = landings.firstOrNull { it.pair.leaf == min(page, target) }
        when {
            inFlight != null -> settle(inFlight.pair, towards = forward)
            target !in 0..lastPage || !canTurn(min(page, target)) -> return false
            landing != null -> {
                catch(landing)
                settle(landing.pair, towards = forward)
            }
            else -> {
                paper = Paper(direction = if (forward) 1f else -1f)
                settle(TurnPair(forward, page, target), towards = forward)
            }
        }
        return true
    }

    private fun settle(pair: TurnPair, towards: Boolean) {
        val commit = pair.forward == towards
        phase = TurnPhase.Settling(pair, landing = if (commit) pair.to else pair.from)
        val leafTurn = turn
        tick()
        scope.launch {
            if (commit) complete(pair, 0f, leafTurn) else cancel(pair, 0f, leafTurn)
        }
    }

    private fun handOffIfLandingTowards(forward: Boolean) {
        val inFlight = phase as? TurnPhase.Settling ?: return
        val pair = inFlight.pair
        val headingLeft = (inFlight.landing == pair.to) == pair.forward
        if (headingLeft != forward) return
        landings += Landing(inFlight.pair, turn, paper)
        page = inFlight.landing
        phase = TurnPhase.Idle
        turn = newTurn()
        paper = Paper(direction = if (forward) 1f else -1f)
    }

    private fun catch(landing: Landing) {
        landings.remove(landing)
        turn = landing.turn
        paper = landing.paper
        page = landing.pair.from
    }

    private suspend fun complete(pair: TurnPair, velocity: Float, leafTurn: Anim) {
        if (leafTurn === turn) phase = TurnPhase.Settling(pair, landing = pair.to)
        leafTurn.animateTo(
            1f,
            spring(dampingRatio = 1f, stiffness = PageTurnDimens.CommitStiffness),
            initialVelocity = velocity,
        )
        landed(leafTurn) {
            page = pair.to
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

    private suspend fun landed(leafTurn: Anim, inHand: suspend () -> Unit) {
        if (leafTurn === turn) inHand() else landings.removeAll { it.turn === leafTurn }
        followNarration()
    }

    private fun tick() {
        if (ticking?.isActive == true) return
        ticking = scope.launch {
            var last = withFrameNanos { it }
            while (isTurning) {
                val now = withFrameNanos { it }
                val dt = ((now - last) / 1e9f).coerceIn(0f, MaxFrameSeconds)
                last = now
                if (dt > 0f) step(dt)
            }
        }
    }

    private fun step(dt: Float) {
        landings.forEach { it.paper.step(leafTime(it.pair, it.turn.value), dt, held = false) }
        val pair = pair ?: return
        val dragging = phase is TurnPhase.Dragging
        val t = leafProgress
        paper.step(t, dt, held = dragging)
        val layout = layout
        val grip = paper.heldGrip
        if (dragging && layout != null && grip != null) {
            val lean = leanFor(grip, pull(target), t, layout.geometry)
            paper.tilt = paper.tilt.step(lean, dt, PageTurnDimens.TiltFrequency, damping = 1f)
        }
    }

    private fun stop() {
        riffling?.cancel()
        riffleFrom = null
        riffleCovered = false
        riffleSpan = null
        halt()
    }

    private fun halt() {
        ticking?.cancel()
        val stopped = turn
        scope.launch { stopped.stop() }
        landings.forEach { landing -> scope.launch { landing.turn.stop() } }
        landings.clear()
        phase = TurnPhase.Idle
        turn = newTurn()
        paper = Paper(direction = 1f)
    }
}

private fun Paper.step(t: Float, dt: Float, held: Boolean) {
    if (!lastT.isNaN()) {
        val now = (t - lastT) / dt
        velocity += (now - velocity) * VelocitySmoothing
    }
    lastT = t
    if (!held && abs(velocity) > TurningVelocity) direction = sign(velocity)
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
    Flight(leaf, t, bow.value, if (abs(lean) < LeanUnseen) 0f else lean, heldGrip)

private fun leafTime(pair: TurnPair, progress: Float): Float =
    if (pair.forward) progress else 1f - progress

@Composable
internal fun rememberPageTurnState(): PageTurnState {
    val scope = rememberCoroutineScope()
    return remember(scope) { PageTurnState(scope) }
}
