package dev.dimvlachos.lab.frostdemo.presentation.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.toSize
import dev.dimvlachos.lab.core.demo.DemoState
import dev.dimvlachos.lab.core.presentation.components.frost.FrostState
import dev.dimvlachos.lab.core.presentation.components.frost.FrostedWindow
import dev.dimvlachos.lab.frostdemo.FrostDemos
import dev.dimvlachos.lab.frostdemo.clipFrameToWindow
import dev.dimvlachos.lab.frostdemo.pointAt
import dev.dimvlachos.lab.resources.Res
import dev.dimvlachos.lab.resources.photo_santorini
import org.jetbrains.compose.resources.painterResource

@Composable
internal fun FrostDemo(state: DemoState, frost: FrostState = remember { FrostState() }) {
    var window by remember { mutableStateOf(Size.Zero) }
    // The script's wipe plays its finger back sample by sample: the path already holds the hand's
    // speed, so the playback itself is linear. The path is drawn in the clip's frame, placed on
    // whatever window this is.
    DisposableEffect(state, frost) {
        state.setWipeHandler { path, duration ->
            if (window.isEmpty()) return@setWipeHandler
            val stroke = frost.beginStroke(clipFrameToWindow(path.first(), window))
            animate(
                0f,
                1f,
                animationSpec = tween(duration.inWholeMilliseconds.toInt(), easing = LinearEasing),
            ) { t, _ ->
                frost.extendStroke(stroke, clipFrameToWindow(pointAt(path, t), window))
            }
        }
        onDispose { state.setWipeHandler(null) }
    }
    // Back on 0, the loop's start, the glass frosts over again.
    val selected = state.selectedIndex
    LaunchedEffect(selected) { if (selected == 0) frost.clear() }
    FrostedWindow(
        photo = painterResource(Res.drawable.photo_santorini),
        state = frost,
        modifier = Modifier.fillMaxSize().onSizeChanged { window = it.toSize() },
        brushRadius = FrostDemos.ScrubBrush,
    )
}
