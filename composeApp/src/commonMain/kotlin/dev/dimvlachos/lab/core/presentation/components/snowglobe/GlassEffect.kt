package dev.dimvlachos.lab.core.presentation.components.snowglobe

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RenderEffect
import dev.dimvlachos.lab.core.presentation.ui.SnowGlobeRoom
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * One frame's uniforms for the [GlassShaderSource], as the prototype's `drawGlass` sets them: the
 * sphere's [center] and [radius] in the layer's pixels, an antialiasing pixel [px], the window's
 * size and mullions, the [light]'s direction, the room it reflects ([envTop] above, [envBot]
 * below), and the window's, caustic's and rim's colours, and where the air [bubble] floats.
 */
internal class GlassLook(
    val center: Offset,
    val radius: Float,
    val px: Float,
    val winW: Float,
    val winH: Float,
    val mull: Float,
    val light: FloatArray,
    val envTop: FloatArray,
    val envBot: FloatArray,
    val win: FloatArray,
    val caustics: FloatArray,
    val rim: FloatArray,
    val bubble: Offset,
)

/** The glass compiled once; [effect] wraps it, with one frame's [GlassLook], for the layer. */
internal interface GlassShader {
    fun effect(look: GlassLook): RenderEffect
}

/** The [GlassShaderSource] compiled, or null where it can't run (Android before 13) or fails. */
internal expect fun compileGlassShader(): GlassShader?

private val Light =
    floatArrayOf(-.68f, -.64f, .16f).let { l ->
        val n = sqrt(l[0] * l[0] + l[1] * l[1] + l[2] * l[2])
        floatArrayOf(l[0] / n, l[1] / n, l[2] / n)
    }

/** The prototype's `bubblePos`: at the top of the glass, drifting against the [tilt]. */
internal fun glassBubble(tilt: Float): Offset {
    val a = -PI.toFloat() / 2 - tilt * .9f
    return Offset(cos(a) * .9f, sin(a) * .9f)
}

private fun roomOf(dark: Boolean, scene: GlobeScene): SnowGlobeRoom =
    when (scene) {
        GlobeScene.Sunny -> if (dark) SnowGlobeRoom.DarkSunny else SnowGlobeRoom.LightSunny
        GlobeScene.Windy -> if (dark) SnowGlobeRoom.DarkWindy else SnowGlobeRoom.LightWindy
        GlobeScene.Shower -> if (dark) SnowGlobeRoom.DarkShower else SnowGlobeRoom.LightShower
    }

// The prototype's mixRGB: straight sRGB, as the browser mixes its colour strings.
private fun mix(a: Color, b: Color, t: Float) =
    Color(
        a.red + (b.red - a.red) * t,
        a.green + (b.green - a.green) * t,
        a.blue + (b.blue - a.blue) * t,
    )

/**
 * The prototype's `room()`: the room of [from] crossfading to [to]'s as the scene's [fade] eases.
 */
internal fun room(dark: Boolean, from: GlobeScene, to: GlobeScene, fade: Float): SnowGlobeRoom {
    val a = roomOf(dark, from)
    val b = roomOf(dark, to)
    val t = ease(fade)
    return SnowGlobeRoom(
        mix(a.wall0, b.wall0, t),
        mix(a.wall1, b.wall1, t),
        mix(a.floor0, b.floor0, t),
        mix(a.floor1, b.floor1, t),
    )
}

/** How bright the caustic the glass throws is in [scene]'s light. */
internal fun causticsOf(scene: GlobeScene): Float =
    when (scene) {
        GlobeScene.Shower -> .55f
        GlobeScene.Windy -> .8f
        GlobeScene.Sunny -> 1f
    }

internal fun glassLook(
    center: Offset,
    radius: Float,
    dark: Boolean,
    from: GlobeScene,
    to: GlobeScene,
    fade: Float,
    tilt: Float,
): GlassLook {
    val rm = room(dark, from, to, fade)
    val top = if (dark) 1.4f else 1.08f
    val bot = if (dark) 1.1f else .92f
    val k = causticsOf(to)
    return GlassLook(
        center = center,
        radius = radius,
        px = 1f / radius,
        winW = if (dark) .15f else .2f,
        winH = if (dark) .15f else .26f,
        mull = if (dark) 0f else 1f,
        light = Light,
        envTop =
            floatArrayOf(
                min(1.2f, rm.wall1.red * top),
                min(1.2f, rm.wall1.green * top),
                min(1.2f, rm.wall1.blue * top),
            ),
        envBot = floatArrayOf(rm.floor1.red * bot, rm.floor1.green * bot, rm.floor1.blue * bot),
        win = if (dark) floatArrayOf(1.5f, 1.26f, .96f) else floatArrayOf(1.9f, 1.86f, 1.8f),
        caustics = floatArrayOf(.34f * k, .27f * k, .17f * k),
        rim = if (dark) floatArrayOf(.7f, .66f, .62f) else floatArrayOf(1f, 1f, 1f),
        bubble = glassBubble(tilt),
    )
}
