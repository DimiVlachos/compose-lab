package dev.dimvlachos.lab.core.presentation.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot

/**
 * The app's theme switching as the pull-cord lamp switches its screen: the new theme spreads out
 * over the old in a growing circle, from the button that switched it. The old theme isn't composed
 * again for it, as the lamp's screen is: the app is drawn into a layer, and as the theme switches,
 * a picture of the screen as it was is laid over the new one, outside the circle.
 */
@Stable
internal class ThemeRevealState {
    /** Whether the new theme is still spreading over the old. */
    var revealing by mutableStateOf(false)
        private set

    /** How far the circle has spread, 0 to 1 of the way to the screen's farthest corner. */
    internal val progress = Animatable(1f)

    /**
     * Where the circle spreads from, in the root: the theme button's middle, once it is laid out.
     */
    internal var origin: Offset? = null

    /** Where the app's layer lies in the root, to find the button's middle in it. */
    internal var at = Offset.Zero

    // The screen as it was before the switch, laid over the new one outside the circle, while the
    // circle spreads; and the layer the app is drawn into, where it is taken from.
    internal var snapshot: ImageBitmap? by mutableStateOf(null)
        private set

    internal var layer: GraphicsLayer? = null

    // Which switch has the circle: a second one, tapped while the first spreads, takes it over,
    // and the first, called off, must not then take the picture away.
    private var switches = 0

    /**
     * Switches the theme with [change], spreading the new one over the old if [animate] says so and
     * there is a screen and a button to spread from; at once otherwise. A switch made while the
     * last one spreads starts again from the screen as it is, half one theme, half the other.
     */
    suspend fun switch(animate: Boolean, change: () -> Unit) {
        val layer = layer
        val from = origin
        if (!animate || layer == null || from == null) {
            change()
            return
        }
        val mine = ++switches
        // The screen as it is, before the new theme is composed: never a frame of it uncovered.
        val picture = layer.toImageBitmap()
        try {
            snapshot = picture
            revealing = true
            progress.snapTo(0f)
            change()
            progress.animateTo(1f, tween(RevealMs, easing = FastOutSlowInEasing))
        } finally {
            if (mine == switches) {
                snapshot = null
                revealing = false
            }
        }
    }

    private companion object {
        // As long, and eased the same, as the lamp's own reveal.
        const val RevealMs = 520
    }
}

/** The app's [ThemeRevealState], for the theme button to say where it is; none outside the app. */
internal val LocalThemeReveal = staticCompositionLocalOf<ThemeRevealState?> { null }

/**
 * [content], drawn into a layer [state] takes its picture of the screen from, with the old theme
 * laid over it outside the spreading circle while the theme switches. Touches go to [content], in
 * the new theme, all the while.
 */
@Composable
internal fun ThemeReveal(state: ThemeRevealState, content: @Composable () -> Unit) {
    val layer = rememberGraphicsLayer()
    val circle = remember { Path() }
    DisposableEffect(state, layer) {
        state.layer = layer
        onDispose { if (state.layer === layer) state.layer = null }
    }
    Box(
        Modifier.fillMaxSize()
            .onGloballyPositioned { state.at = it.positionInRoot() }
            .drawWithContent {
                layer.record {
                    this@drawWithContent.drawContent()
                    val old = state.snapshot
                    val from = state.origin
                    if (old != null && from != null) {
                        clipToReveal(
                            circle,
                            from - state.at,
                            state.progress.value,
                            inside = false,
                        ) {
                            drawImage(old)
                        }
                    }
                }
                drawLayer(layer)
            },
        propagateMinConstraints = true,
    ) {
        CompositionLocalProvider(LocalThemeReveal provides state, content = content)
    }
}
