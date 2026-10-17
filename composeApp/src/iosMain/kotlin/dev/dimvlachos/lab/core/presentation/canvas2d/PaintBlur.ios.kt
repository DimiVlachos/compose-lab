package dev.dimvlachos.lab.core.presentation.canvas2d

import androidx.compose.ui.graphics.Paint
import org.jetbrains.skia.FilterBlurMode
import org.jetbrains.skia.MaskFilter

internal actual fun Paint.setBlur(sigma: Float) {
    asFrameworkPaint().maskFilter =
        if (sigma <= 0f) null else MaskFilter.makeBlur(FilterBlurMode.NORMAL, sigma)
}
