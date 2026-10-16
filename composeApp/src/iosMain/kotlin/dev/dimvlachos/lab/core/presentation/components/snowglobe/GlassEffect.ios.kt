package dev.dimvlachos.lab.core.presentation.components.snowglobe

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.RenderEffect
import androidx.compose.ui.graphics.asComposeRenderEffect
import co.touchlab.kermit.Logger
import org.jetbrains.skia.ImageFilter
import org.jetbrains.skia.RuntimeEffect
import org.jetbrains.skia.RuntimeShaderBuilder

internal actual fun glassRenderEffect(center: Offset, radius: Float): RenderEffect? =
    try {
        val builder = RuntimeShaderBuilder(RuntimeEffect.makeForShader(GlassShaderSource))
        builder.uniform("center", center.x, center.y)
        builder.uniform("radius", radius)
        ImageFilter.makeRuntimeShader(builder, "content", null).asComposeRenderEffect()
    } catch (e: RuntimeException) {
        Logger.w(e) { "The snow globe's glass shader didn't compile; drawing the plain glass" }
        null
    }
