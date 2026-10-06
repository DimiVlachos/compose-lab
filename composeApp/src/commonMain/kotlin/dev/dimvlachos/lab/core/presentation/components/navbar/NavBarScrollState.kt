package dev.dimvlachos.lab.core.presentation.components.navbar

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

private val SettleSpec =
    spring<Float>(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)

/**
 * How far an [AnimatedNavBar] has collapsed as its content scrolls. Hook [nestedScrollConnection]
 * to the scrolling content with `Modifier.nestedScroll`; on release, the bar settles fully open or
 * fully collapsed. Made by [rememberNavBarScrollState].
 */
@Stable
public class NavBarScrollState internal constructor(private val collapseDistancePx: Float) {
    /** How far the bar has collapsed: 0 open, 1 a floating pill. */
    public var collapse: Float by mutableFloatStateOf(0f)
        private set

    private var settleJob: Job? = null
    private var lastDeltaY = 0f

    /** Collapses the bar as the content scrolls, and settles it once a fling ends. */
    public val nestedScrollConnection: NestedScrollConnection =
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                onScroll(available.y)
                return Offset.Zero
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                settle()
                return Velocity.Zero
            }
        }

    /**
     * Moves the bar by a scroll of [deltaY] px: a negative delta, the content moving up, collapses
     * it, and a positive one opens it. Stops a settle in progress.
     */
    public fun onScroll(deltaY: Float) {
        settleJob?.cancel()
        settleJob = null
        collapse = (collapse - deltaY / collapseDistancePx).coerceIn(0f, 1f)
        lastDeltaY = deltaY
    }

    internal suspend fun settle() {
        val start = collapse
        if (start <= 0f || start >= 1f) return
        val target =
            when {
                start > 0.5f -> 1f
                start < 0.5f -> 0f
                lastDeltaY < 0f -> 1f
                else -> 0f
            }
        settleJob?.cancel()
        coroutineScope {
            val job = launch {
                animate(initialValue = start, targetValue = target, animationSpec = SettleSpec) {
                    value,
                    _ ->
                    collapse = value
                }
            }
            settleJob = job
            job.join()
            if (settleJob === job) settleJob = null
        }
    }
}

/** A [NavBarScrollState] that collapses the bar fully over [collapseDistance] of scrolling. */
@Composable
public fun rememberNavBarScrollState(collapseDistance: Dp = 120.dp): NavBarScrollState {
    val distancePx = with(LocalDensity.current) { collapseDistance.toPx() }
    return remember(distancePx) { NavBarScrollState(distancePx) }
}
