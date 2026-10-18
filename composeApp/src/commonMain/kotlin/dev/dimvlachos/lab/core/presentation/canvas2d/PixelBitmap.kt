package dev.dimvlachos.lab.core.presentation.canvas2d

import androidx.compose.ui.graphics.ImageBitmap

/**
 * A bitmap made from [pixels], ARGB packed as `0xAARRGGBB`, row by row, [width] to a row: for maps
 * worked out a pixel at a time, such as a baked light map.
 */
internal expect fun imageBitmapOf(pixels: IntArray, width: Int, height: Int): ImageBitmap
