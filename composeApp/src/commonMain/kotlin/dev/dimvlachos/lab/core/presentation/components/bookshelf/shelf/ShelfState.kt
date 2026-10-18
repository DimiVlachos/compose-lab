package dev.dimvlachos.lab.core.presentation.components.bookshelf.shelf

import androidx.compose.animation.core.LinearEasing
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
private const val VANISH_MS = 450
private const val ARCH_MS = 650
private const val ARCH_CLOSE_MS = 220
private const val OPEN_FROM_TRAVEL = 0.9f
private const val PAGES_WAIT_MS = 3000L

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
        ShelfMotion(activeBookId, travel, open, effectiveTilt, vanish, arch)
    }

    val isBookOut: Boolean
        get() = mode == ShelfMode.Out || mode == ShelfMode.Open

    val takeOutArmed: Boolean
        get() = mode == ShelfMode.Tip && TiltGesture.shouldTakeOut(tilt, 0f)

    fun beginTip(bookId: Int) {
        val start = if (mode == ShelfMode.Settle && activeBookId == bookId) tilt else 0f
        cancelMotion()
        activeBookId = bookId
        mode = ShelfMode.Tip
        travel = 0f
        open = 0f
        vanish = 0f
        arch = 0f
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
        beginTip(bookId)
        tilt = OPEN_DIRECT_TILT
        takeOut()
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
            animate(
                0f,
                1f,
                animationSpec = tween((CLOSE_MS * openStart).toInt() + 1, easing = LinearEasing),
            ) { p, _ ->
                val e = easeInOutCubic(p)
                open = openStart * (1f - e)
            }
            path = TiltPath.Back
            val travelStart = travel
            animate(
                travelStart,
                0f,
                animationSpec = tween((RETURN_MS * travelStart).toInt(), easing = LinearEasing),
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
                        tween((TRAVEL_MS * (1f - travelStart)).toInt(), easing = LinearEasing),
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
                animationSpec = tween((OPEN_MS * (1f - open)).toInt(), easing = LinearEasing),
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
        animate(0f, 1f, animationSpec = tween(ARCH_MS, easing = LinearEasing)) { p, _ ->
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
            animationSpec = tween((durationMs * span).toInt(), easing = LinearEasing),
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
                    dampingRatio = SETTLE_DAMPING,
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
