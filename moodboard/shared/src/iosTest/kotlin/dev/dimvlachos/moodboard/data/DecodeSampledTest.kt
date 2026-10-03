package dev.dimvlachos.moodboard.data

import kotlin.test.Test
import kotlin.test.assertEquals
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Surface

class DecodeSampledTest {
    private fun png(width: Int, height: Int): ByteArray =
        Surface.makeRasterN32Premul(width, height)
            .makeImageSnapshot()
            .encodeToData(EncodedImageFormat.PNG)!!
            .bytes

    @Test
    fun largeImagesShrinkToFitTheLongerSide() {
        val bitmap = decodeSampled(png(400, 200), maxPixel = 100)!!
        assertEquals(100, bitmap.width)
        assertEquals(50, bitmap.height)
    }

    @Test
    fun smallImagesKeepTheirSize() {
        val bitmap = decodeSampled(png(40, 20), maxPixel = 100)!!
        assertEquals(40, bitmap.width)
    }
}
