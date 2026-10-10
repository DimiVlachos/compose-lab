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
import dev.dimvlachos.lab.core.presentation.components.popup.PopUpDimens
import dev.dimvlachos.lab.core.presentation.ui.CycladesPaper
import dev.dimvlachos.lab.core.presentation.ui.CycladesSky

// The sky turns with a page, over as long as the page takes.
private val TurnMs = (PopUpDimens.TurnSeconds * 1000).toInt()

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
    private val progress = Animatable(1f)
    private var from: CycladesSky = skies[0]
    private var to = 0

    /** Fades towards the sky of moment [index] of the tour. */
    suspend fun fadeTo(index: Int) {
        val target = index.coerceIn(0, skies.lastIndex)
        if (target != to) {
            // Set off from exactly where the sky is now, even halfway through another fade, so
            // it never jumps.
            from = mixed()
            to = target
            progress.snapTo(0f)
        }
        progress.animateTo(1f, tween(TurnMs))
    }

    private fun mixed(): CycladesSky =
        CycladesSky(mix { it.top }, mix { it.middle }, mix { it.bottom }, mix { it.glow })

    private fun mix(pick: (CycladesSky) -> Color): Color =
        lerp(pick(from), pick(skies[to]), progress.value)

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
