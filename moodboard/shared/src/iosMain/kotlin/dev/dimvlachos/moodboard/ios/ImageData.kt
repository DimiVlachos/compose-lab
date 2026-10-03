package dev.dimvlachos.moodboard.ios

import dev.dimvlachos.moodboard.MoodboardGraph
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Foundation.dataWithBytes

/** A bundled photo's bytes for SwiftUI (UIImage(data:)); null when it's missing. */
suspend fun imageData(path: String): NSData? = MoodboardGraph.photoBytes.bytes(path)?.toNSData()

@OptIn(ExperimentalForeignApi::class)
internal fun ByteArray.toNSData(): NSData =
    if (isEmpty()) NSData() else usePinned { NSData.dataWithBytes(it.addressOf(0), size.toULong()) }
