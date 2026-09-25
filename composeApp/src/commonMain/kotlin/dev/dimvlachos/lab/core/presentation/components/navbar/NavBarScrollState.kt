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

@Stable
class NavBarScrollState internal constructor(private val collapseDistancePx: Float) {
    var collapse: Float by mutableFloatStateOf(0f)
        private set

    private var settleJob: Job? = null
    private var lastDeltaY = 0f

    val nestedScrollConnection: NestedScrollConnection =
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

    fun onScroll(deltaY: Float) {
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

@Composable
fun rememberNavBarScrollState(collapseDistance: Dp = 120.dp): NavBarScrollState {
    val distancePx = with(LocalDensity.current) { collapseDistance.toPx() }
    return remember(distancePx) { NavBarScrollState(distancePx) }
}
