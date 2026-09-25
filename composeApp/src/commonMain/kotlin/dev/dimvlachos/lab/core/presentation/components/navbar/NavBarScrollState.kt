package dev.dimvlachos.lab.core.presentation.components.navbar

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
import androidx.compose.ui.unit.dp

@Stable
class NavBarScrollState internal constructor(private val collapseDistancePx: Float) {
    var collapse: Float by mutableFloatStateOf(0f)
        private set

    val nestedScrollConnection: NestedScrollConnection =
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                onScroll(available.y)
                return Offset.Zero
            }
        }

    fun onScroll(deltaY: Float) {
        collapse = (collapse - deltaY / collapseDistancePx).coerceIn(0f, 1f)
    }
}

@Composable
fun rememberNavBarScrollState(collapseDistance: Dp = 120.dp): NavBarScrollState {
    val distancePx = with(LocalDensity.current) { collapseDistance.toPx() }
    return remember(distancePx) { NavBarScrollState(distancePx) }
}
