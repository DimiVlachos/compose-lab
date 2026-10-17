package dev.dimvlachos.lab.core.presentation.components.snowglobe

import androidx.compose.ui.graphics.ImageBitmap

// Android's renderer caches a bitmap's texture by its generation id, so nothing is copied.
internal actual fun ImageBitmap.freeze() = Unit
