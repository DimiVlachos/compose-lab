package dev.dimvlachos.moodboard.data

import androidx.compose.ui.graphics.ImageBitmap

/** Decodes [bytes] scaled down so the longer side is at most [maxPixel]; null if unreadable. */
internal expect fun decodeSampled(bytes: ByteArray, maxPixel: Int): ImageBitmap?
