package dev.dimvlachos.moodboard.data

import androidx.compose.ui.graphics.ImageBitmap

/**
 * Decodes [bytes] scaled down toward [maxPixel] on the longer side; null if unreadable. iOS scales
 * to exactly [maxPixel]. Android subsamples while decoding, by powers of two, so its longer side
 * lands between [maxPixel] and twice that: never softer than asked, never the full image.
 */
internal expect fun decodeSampled(bytes: ByteArray, maxPixel: Int): ImageBitmap?
