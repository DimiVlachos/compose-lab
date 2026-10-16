package dev.dimvlachos.lab.core.presentation.components.snowglobe

import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.RenderEffect
import androidx.compose.ui.graphics.asComposeRenderEffect
import co.touchlab.kermit.Logger

internal actual fun glassRenderEffect(center: Offset, radius: Float): RenderEffect? {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return null
    return try {
        glass(center, radius)
    } catch (e: RuntimeException) {
        Logger.w(e) { "The snow globe's glass shader didn't compile; drawing the plain glass" }
        null
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private fun glass(center: Offset, radius: Float): RenderEffect {
    val shader = RuntimeShader(GlassShaderSource)
    shader.setFloatUniform("center", center.x, center.y)
    shader.setFloatUniform("radius", radius)
    return android.graphics.RenderEffect.createRuntimeShaderEffect(shader, "content")
        .asComposeRenderEffect()
}
