package dev.dimvlachos.lab.popupdemo.presentation.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import dev.dimvlachos.lab.core.presentation.ui.CycladesPaper
import dev.dimvlachos.lab.core.presentation.ui.CycladesSky

// How long the sky takes to turn with a page: about as long as the page itself.
private const val TurnMs = 900

// Where the sun glows, as fractions of the screen, and how far its light reaches.
private const val GlowX = 0.86f
private const val GlowY = 0.12f
private const val GlowReach = 0.75f

/**
 * The sky behind the pop-up tour, one for each spread, turning from one to the next with the page.
 * Its colours are read only while drawing, so the screen never recomposes as it fades.
 */
@Stable
internal class TourSky {
    private val skies = CycladesPaper.skies
    private val from = Animatable(0f)
    private var shownFrom = 0
    private var shownTo = 0

    /** Fades towards the sky of moment [index] of the tour. */
    suspend fun fadeTo(index: Int) {
        val target = index.coerceIn(0, skies.lastIndex)
        if (target == shownTo && from.value == 1f) return
        if (target != shownTo) {
            // Start from wherever the fade has got to.
            shownFrom = if (from.value >= 0.5f) shownTo else shownFrom
            shownTo = target
            from.snapTo(0f)
        }
        from.animateTo(1f, tween(TurnMs))
    }

    private fun mix(pick: (CycladesSky) -> Color): Color =
        lerp(pick(skies[shownFrom]), pick(skies[shownTo]), from.value)

    fun Modifier.drawSky(): Modifier = drawBehind {
        drawRect(
            Brush.verticalGradient(
                0f to mix { it.top },
                0.45f to mix { it.middle },
                1f to mix { it.bottom },
            )
        )
        drawRect(
            Brush.radialGradient(
                listOf(mix { it.glow }.copy(alpha = 0.55f), Color.Transparent),
                center = Offset(size.width * GlowX, size.height * GlowY),
                radius = size.maxDimension * GlowReach,
            )
        )
    }

    @Composable
    fun showFor(index: Int) {
        LaunchedEffect(index) { fadeTo(index) }
    }
}

@Composable internal fun rememberTourSky(): TourSky = remember { TourSky() }
