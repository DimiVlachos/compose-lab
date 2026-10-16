package dev.dimvlachos.lab.core.presentation.canvas2d

import android.graphics.BlurMaskFilter
import androidx.compose.ui.graphics.Paint

// BlurMaskFilter takes a radius; Skia turns a radius r into a sigma of 0.57735 r + 0.5.
internal actual fun Paint.setBlur(sigma: Float) {
    asFrameworkPaint().maskFilter =
        if (sigma <= 0.5f) null
        else BlurMaskFilter((sigma - 0.5f) / 0.57735f, BlurMaskFilter.Blur.NORMAL)
}
