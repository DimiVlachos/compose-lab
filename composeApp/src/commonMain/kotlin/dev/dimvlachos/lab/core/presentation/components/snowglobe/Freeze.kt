package dev.dimvlachos.lab.core.presentation.components.snowglobe

import androidx.compose.ui.graphics.ImageBitmap

/**
 * Marks a bitmap that is done being painted as unchanging, so it is drawn from its own pixels: Skia
 * copies a changeable bitmap into a new image every time it is drawn, which a scene, base and
 * sprites drawn every frame can't afford. Nothing may paint into it afterwards.
 */
internal expect fun ImageBitmap.freeze()
