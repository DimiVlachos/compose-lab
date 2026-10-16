package dev.dimvlachos.lab.core.presentation.components.snowglobe

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.RenderEffect

/**
 * The [GlassShaderSource] as an effect on the interior's layer, for a sphere around [center] of
 * [radius] in the layer's pixels; null where the platform can't run it (Android before 13) or it
 * fails to compile, and the globe then draws [drawGlassFallback] instead.
 */
internal expect fun glassRenderEffect(center: Offset, radius: Float): RenderEffect?
