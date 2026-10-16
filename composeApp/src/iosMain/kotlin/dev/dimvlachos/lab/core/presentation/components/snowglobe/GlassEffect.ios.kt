package dev.dimvlachos.lab.core.presentation.components.snowglobe

import androidx.compose.ui.graphics.RenderEffect
import androidx.compose.ui.graphics.asComposeRenderEffect
import co.touchlab.kermit.Logger
import org.jetbrains.skia.ImageFilter
import org.jetbrains.skia.RuntimeEffect
import org.jetbrains.skia.RuntimeShaderBuilder

private val glassEffect: RuntimeEffect? by lazy {
    try {
        RuntimeEffect.makeForShader(GlassShaderSource)
    } catch (e: RuntimeException) {
        Logger.w(e) { "The snow globe's glass shader didn't compile; drawing the plain glass" }
        null
    }
}

internal actual fun compileGlassShader(): GlassShader? = glassEffect?.let { effect ->
    object : GlassShader {
        override fun effect(look: GlassLook): RenderEffect =
            glassFilter(effect, look).asComposeRenderEffect()
    }
}

/** The glass as a Skia image filter, for one frame's [look]; null if it didn't compile. */
internal fun glassImageFilter(look: GlassLook): ImageFilter? = glassEffect?.let {
    glassFilter(it, look)
}

private fun glassFilter(effect: RuntimeEffect, look: GlassLook): ImageFilter {
    val b = RuntimeShaderBuilder(effect)
    b.uniform("center", look.center.x, look.center.y)
    b.uniform("radius", look.radius)
    b.uniform("uPx", look.px)
    b.uniform("uWinW", look.winW)
    b.uniform("uWinH", look.winH)
    b.uniform("uMull", look.mull)
    b.vec3("uL", look.light)
    b.vec3("uEnvTop", look.envTop)
    b.vec3("uEnvBot", look.envBot)
    b.vec3("uWin", look.win)
    b.vec3("uCaus", look.caustics)
    b.vec3("uRim", look.rim)
    b.uniform("uBub", look.bubble.x, look.bubble.y)
    return ImageFilter.makeRuntimeShader(b, "content", null)
}

private fun RuntimeShaderBuilder.vec3(name: String, v: FloatArray) = uniform(name, v[0], v[1], v[2])
