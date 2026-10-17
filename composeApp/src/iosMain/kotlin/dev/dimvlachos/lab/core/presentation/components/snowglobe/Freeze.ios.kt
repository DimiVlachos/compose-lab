package dev.dimvlachos.lab.core.presentation.components.snowglobe

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asSkiaBitmap

internal actual fun ImageBitmap.freeze() {
    asSkiaBitmap().setImmutable()
}
