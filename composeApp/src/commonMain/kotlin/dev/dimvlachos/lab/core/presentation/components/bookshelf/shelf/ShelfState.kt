package dev.dimvlachos.lab.core.presentation.components.bookshelf.shelf

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import dev.dimvlachos.lab.core.presentation.components.bookshelf.geometry.TiltGesture
import dev.dimvlachos.lab.core.presentation.components.bookshelf.geometry.TiltPaths
import dev.dimvlachos.lab.core.presentation.components.bookshelf.geometry.easeInOutCubic
import dev.dimvlachos.lab.core.presentation.components.bookshelf.geometry.easeOutCubic
import dev.dimvlachos.lab.core.presentation.components.bookshelf.geometry.smoothStep
import kotlin.math.abs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

private const val SETTLE_STIFFNESS = 160f
private const val SETTLE_DAMPING = 0.435f
private const val SETTLE_THRESHOLD = 0.0015f
private const val NUDGE_VELOCITY = 0.9f
private const val OPEN_DIRECT_TILT = 0.2f
private const val TRAVEL_MS = 1150
private const val OPEN_MS = 1000
private const val CLOSE_MS = 600
private const val RETURN_MS = 900
private const val PUSH_RELAX_MS = 300
private const val VANISH_MS = 450
private const val ARCH_MS = 650
private const val ARCH_CLOSE_MS = 220
private const val OPEN_FROM_TRAVEL = 0.9f
private const val PAGES_WAIT_MS = 3000L
private const val REDUCED_MOTION_TIME = 0.4f

internal enum class ShelfMode {
    Shelf,
    Tip,
    Settle,
    Out,
    Open,
    Back,
}

private enum class TiltPath {
    Direct,
    Out,
    Back,
}

@Stable
internal class ShelfState(
    private val scope: CoroutineScope,
    private val onOpened: (Int) -> Unit,
    private val onClosed: (Int) -> Unit,
) {
    /** The ring a shelf of [ShelfStyle.Ring] spins on, once it is shown: a script turns it. */
    internal var ringSpin: RingState? = null

    /** Whether the shelf's books are baked and drawn whole, so a screen can show it all at once. */
    var ready by mutableStateOf(false)
        internal set

    var mode by mutableStateOf(ShelfMode.Shelf)
        private set

    var activeBookId by mutableStateOf<Int?>(null)
        private set

    var pagesReadyFor by mutableStateOf<Int?>(null)

    var listening by mutableStateOf(false)
        private set

    private var tilt by mutableFloatStateOf(0f)
    private var travel by mutableFloatStateOf(0f)
    private var open by mutableFloatStateOf(0f)
    private var vanish by mutableFloatStateOf(0f)
    private var arch by mutableFloatStateOf(0f)
    private var push by mutableFloatStateOf(0f)
    // The cover is being closed from the first page, so it swings alone, without the read pages.
    private var coverOnly by mutableStateOf(false)
    private var releaseTilt by mutableFloatStateOf(0f)
    private var path by mutableStateOf(TiltPath.Direct)
    private var grabStartTilt = 0f
    private var job: Job? = null

    val motion: State<ShelfMotion> = derivedStateOf {
        val effectiveTilt =
            when {
                path == TiltPath.Back -> TiltPaths.back(travel)
                travel <= 0f -> tilt
                else -> TiltPaths.out(releaseTilt, travel)
            }
        ShelfMotion(activeBookId, travel, open, effectiveTilt, vanish, arch, push, coverOnly)
    }

    val isBookOut: Boolean
        get() = mode == ShelfMode.Out || mode == ShelfMode.Open

    val takeOutArmed: Boolean
        get() = mode == ShelfMode.Tip && TiltGesture.shouldTakeOut(tilt, 0f)

    /** Whether a book is in hand here: tipped, out, open or going back, not just settling. */
    val holdsABook: Boolean
        get() = activeBookId != null && mode != ShelfMode.Settle

    /** The other shelves on the same screen. */
    var others: () -> List<ShelfState> = { emptyList() }

    /**
     * Whether another shelf on the screen holds a book: while it does, no book here can be tipped
     * or taken out, so only one is ever in hand and Back always closes the one that shows.
     */
    val heldElsewhere: Boolean
        get() = others().any { it.holdsABook }

    /** Whether another shelf's book is out over this one, hiding it. */
    val coveredElsewhere: Boolean
        get() = others().any { it.isBookOut || it.mode == ShelfMode.Back }

    /** With reduced motion a tipped book settles without bouncing and every move is short. */
    var reducedMotion = false

    private fun ms(durationMs: Int): Int =
        if (reducedMotion) (durationMs * REDUCED_MOTION_TIME).toInt() else durationMs

    fun beginTip(bookId: Int) {
        if (heldElsewhere) return
        val start = if (mode == ShelfMode.Settle && activeBookId == bookId) tilt else 0f
        cancelMotion()
        activeBookId = bookId
        mode = ShelfMode.Tip
        travel = 0f
        open = 0f
        vanish = 0f
        arch = 0f
        coverOnly = false
        listening = false
        path = TiltPath.Direct
        grabStartTilt = maxOf(TiltGesture.MIN_GRAB_TILT, start)
        tilt = grabStartTilt
    }

    fun dragTip(dragY: Float, grabHeight: Float) {
        if (mode != ShelfMode.Tip) return
        tilt = TiltGesture.tiltFor(dragY, grabHeight, grabStartTilt)
    }

    fun releaseTip(velocityY: Float) {
        if (mode != ShelfMode.Tip) return
        if (TiltGesture.shouldTakeOut(tilt, velocityY)) takeOut() else settle(0f)
    }

    fun nudge(bookId: Int) {
        if (mode == ShelfMode.Settle && activeBookId != bookId) finishSettle()
        if (mode != ShelfMode.Shelf) return
        activeBookId = bookId
        tilt = 0f
        settle(NUDGE_VELOCITY)
    }

    fun openDirect(bookId: Int) {
        if (mode != ShelfMode.Shelf && mode != ShelfMode.Settle) return
        if (heldElsewhere) return
        beginTip(bookId)
        tilt = OPEN_DIRECT_TILT
        takeOut()
    }

    /** Pushes the open book down after a finger [dragY] dp down it. */
    fun dragPush(dragY: Float) {
        if (mode != ShelfMode.Open) return
        cancelMotion()
        // A cover let go just before is open again, not left ajar where its fall was cut off.
        open = 1f
        coverOnly = false
        push = TiltGesture.pushFor(dragY)
        arch = archFor(push)
    }

    /** Lets the push go at [velocityY] dp a second: far or fast enough, the book goes back. */
    fun releasePush(velocityY: Float) {
        if (mode != ShelfMode.Open) return
        if (TiltGesture.shouldPutBack(push, velocityY)) return putBack()
        val start = push
        run {
            animate(0f, 1f, animationSpec = tween(ms(PUSH_RELAX_MS), easing = LinearEasing)) { p, _
                ->
                push = start * (1f - easeOutCubic(p))
                arch = archFor(push)
            }
        }
    }

    // The pages lie arched until a push starts, and flatten over its first stretch, so the book
    // is flat when it begins to move.
    private fun archFor(push: Float) = 1f - smoothStep(0f, TiltGesture.PUSH_FLATTEN, push)

    /** Closes the open book's cover by [closed] of the way, with a finger on its first page. */
    fun dragCover(closed: Float) {
        if (mode != ShelfMode.Open) return
        cancelMotion()
        // A push let go just before is over, not left frozen where its relax was cut off.
        push = 0f
        // The first of the close flattens the arched pages; the cover closes over the rest.
        val c = closed.coerceIn(0f, 1f)
        coverOnly = true
        val flatten = TiltGesture.COVER_FLATTEN
        arch = 1f - smoothStep(0f, flatten, c)
        open = 1f - ((c - flatten) / (1f - flatten)).coerceIn(0f, 1f)
    }

    /**
     * Lets the cover go, closing at [velocity] of the way a second: shut far or fast enough, the
     * book goes back; else the cover falls open again.
     */
    fun releaseCover(velocity: Float) {
        if (mode != ShelfMode.Open) return
        if (TiltGesture.shouldCloseCover(open, velocity)) return putBack()
        val start = open
        run {
            animate(0f, 1f, animationSpec = tween(ms(PUSH_RELAX_MS), easing = LinearEasing)) { p, _
                ->
                open = start + (1f - start) * easeOutCubic(p)
            }
            coverOnly = false
            ease(arch, 1f, PUSH_RELAX_MS) { arch = it }
        }
    }

    fun listen() {
        if (mode != ShelfMode.Open || listening) return
        listening = true
        run {
            ease(arch, 0f, ARCH_CLOSE_MS) { arch = it }
            ease(open, 0f, OPEN_MS, linear = true) { open = it }
            ease(vanish, 1f, VANISH_MS) { vanish = it }
        }
    }

    fun read() {
        if (mode != ShelfMode.Open || !listening) return
        listening = false
        run {
            ease(vanish, 0f, VANISH_MS) { vanish = it }
            ease(open, 1f, OPEN_MS, linear = true) { open = it }
            relaxArch()
        }
    }

    fun putBack() {
        if (!isBookOut) return
        val bookId = activeBookId ?: return
        mode = ShelfMode.Back
        tilt = 0f
        run {
            ease(vanish, 0f, VANISH_MS) { vanish = it }
            ease(arch, 0f, ARCH_CLOSE_MS) { arch = it }
            val openStart = open
            val pushStart = push
            animate(
                0f,
                1f,
                animationSpec =
                    tween(ms((CLOSE_MS * openStart).toInt()) + 1, easing = LinearEasing),
            ) { p, _ ->
                val e = easeInOutCubic(p)
                open = openStart * (1f - e)
                push = pushStart * (1f - e)
            }
            push = 0f
            path = TiltPath.Back
            val travelStart = travel
            animate(
                travelStart,
                0f,
                animationSpec = tween(ms((RETURN_MS * travelStart).toInt()), easing = LinearEasing),
            ) { v, _ ->
                travel = v
            }
            travel = 0f
            path = TiltPath.Direct
            tilt = 0f
            listening = false
            onClosed(bookId)
            finishSettle()
        }
    }

    fun snapTo(pose: ShelfMotion) {
        cancelMotion()
        activeBookId = pose.activeBookId
        travel = pose.travel
        open = pose.open
        vanish = pose.vanish
        arch = pose.arch
        push = pose.push
        listening = false
        tilt = pose.tilt
        releaseTilt = pose.tilt
        path = if (pose.travel > 0f) TiltPath.Out else TiltPath.Direct
        mode =
            when {
                pose.activeBookId == null -> ShelfMode.Shelf
                pose.open >= 1f -> ShelfMode.Open
                pose.travel > 0f -> ShelfMode.Out
                else -> ShelfMode.Tip
            }
    }

    fun reset() {
        cancelMotion()
        finishSettle()
    }

    /**
     * The shelf moved on the screen (its list scrolled): a book still settling back drops straight
     * into place, since it is drawn where the shelf was.
     */
    fun shelfMoved() {
        if (mode != ShelfMode.Settle) return
        cancelMotion()
        finishSettle()
    }

    private fun takeOut() {
        val bookId = activeBookId ?: return
        mode = ShelfMode.Out
        releaseTilt = tilt
        path = TiltPath.Out
        val travelStart = travel
        run {
            val arriving = launch {
                animate(
                    travelStart,
                    1f,
                    animationSpec =
                        tween(ms((TRAVEL_MS * (1f - travelStart)).toInt()), easing = LinearEasing),
                ) { v, _ ->
                    travel = v
                }
                travel = 1f
            }
            snapshotFlow { travel >= OPEN_FROM_TRAVEL }.first { it }
            withTimeoutOrNull(PAGES_WAIT_MS) {
                snapshotFlow { pagesReadyFor == bookId }.first { it }
            }
            animate(
                open,
                1f,
                animationSpec = tween(ms((OPEN_MS * (1f - open)).toInt()), easing = LinearEasing),
            ) { v, _ ->
                open = v
            }
            open = 1f
            arriving.join()
            mode = ShelfMode.Open
            launch { relaxArch() }
            onOpened(bookId)
        }
    }

    private suspend fun relaxArch() {
        val from = arch
        animate(0f, 1f, animationSpec = tween(ms(ARCH_MS), easing = LinearEasing)) { p, _ ->
            arch = from + (1f - from) * easeOutCubic(p)
        }
        arch = 1f
    }

    private suspend fun ease(
        from: Float,
        to: Float,
        durationMs: Int,
        linear: Boolean = false,
        set: (Float) -> Unit,
    ) {
        val span = abs(to - from)
        if (span == 0f) return
        animate(
            0f,
            1f,
            animationSpec = tween(ms((durationMs * span).toInt()), easing = LinearEasing),
        ) { p, _ ->
            set(from + (to - from) * if (linear) p else easeInOutCubic(p))
        }
        set(to)
    }

    private fun settle(velocity: Float) {
        mode = ShelfMode.Settle
        run { spring(velocity) }
    }

    private suspend fun spring(velocity: Float) {
        animate(
            initialValue = tilt,
            targetValue = 0f,
            initialVelocity = velocity,
            animationSpec =
                spring(
                    // Reduced motion: back upright without swinging past it.
                    dampingRatio =
                        if (reducedMotion) Spring.DampingRatioNoBouncy else SETTLE_DAMPING,
                    stiffness = SETTLE_STIFFNESS,
                    visibilityThreshold = SETTLE_THRESHOLD,
                ),
        ) { v, _ ->
            tilt = v
        }
        finishSettle()
    }

    private fun finishSettle() {
        tilt = 0f
        mode = ShelfMode.Shelf
        activeBookId = null
    }

    private fun run(block: suspend CoroutineScope.() -> Unit) {
        job?.cancel()
        job = scope.launch(block = block)
    }

    private fun cancelMotion() {
        job?.cancel()
        job = null
    }
}

@Composable
internal fun rememberShelfState(
    onOpened: (Int) -> Unit = {},
    onClosed: (Int) -> Unit = {},
): ShelfState {
    val scope = rememberCoroutineScope()
    val opened by rememberUpdatedState(onOpened)
    val closed by rememberUpdatedState(onClosed)
    return remember(scope) { ShelfState(scope, { opened(it) }, { closed(it) }) }
}
