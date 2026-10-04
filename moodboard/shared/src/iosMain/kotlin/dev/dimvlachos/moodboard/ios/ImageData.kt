package dev.dimvlachos.moodboard.ios

import dev.dimvlachos.moodboard.MoodboardGraph
import dev.dimvlachos.moodboard.ui.components.GridMaxPixel
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Foundation.dataWithBytes

/** A bundled photo's bytes for SwiftUI (UIImage(data:)); null when it's missing. */
suspend fun imageData(path: String): NSData? = MoodboardGraph.photoBytes.bytes(path)?.toNSData()

/**
 * Decodes a photo into the shared cache at the grid size, so a hosted Compose detail opening over
 * it (the zoom transition) has pixels on its first frame instead of a blank tile. SwiftUI's grid
 * decodes through its own cache, which Compose can't read.
 */
suspend fun warmPhoto(path: String) {
    MoodboardGraph.photoBytes.bitmap(path, GridMaxPixel)
}

@OptIn(ExperimentalForeignApi::class)
internal fun ByteArray.toNSData(): NSData =
    if (isEmpty()) NSData() else usePinned { NSData.dataWithBytes(it.addressOf(0), size.toULong()) }
