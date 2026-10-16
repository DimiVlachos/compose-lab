package dev.dimvlachos.lab.core.presentation.components.snowglobe

import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.ui.graphics.RenderEffect
import androidx.compose.ui.graphics.asComposeRenderEffect
import co.touchlab.kermit.Logger

internal actual fun compileGlassShader(): GlassShader? {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return null
    return try {
        AgslGlass(RuntimeShader(GlassShaderSource))
    } catch (e: RuntimeException) {
        Logger.w(e) { "The snow globe's glass shader didn't compile; drawing the plain glass" }
        null
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private class AgslGlass(private val shader: RuntimeShader) : GlassShader {
    override fun effect(look: GlassLook): RenderEffect {
        shader.setFloatUniform("center", look.center.x, look.center.y)
        shader.setFloatUniform("radius", look.radius)
        shader.setFloatUniform("uPx", look.px)
        shader.setFloatUniform("uWinW", look.winW)
        shader.setFloatUniform("uWinH", look.winH)
        shader.setFloatUniform("uMull", look.mull)
        shader.setFloatUniform("uL", look.light)
        shader.setFloatUniform("uEnvTop", look.envTop)
        shader.setFloatUniform("uEnvBot", look.envBot)
        shader.setFloatUniform("uWin", look.win)
        shader.setFloatUniform("uCaus", look.caustics)
        shader.setFloatUniform("uRim", look.rim)
        shader.setFloatUniform("uBub", look.bubble.x, look.bubble.y)
        return android.graphics.RenderEffect.createRuntimeShaderEffect(shader, "content")
            .asComposeRenderEffect()
    }
}
