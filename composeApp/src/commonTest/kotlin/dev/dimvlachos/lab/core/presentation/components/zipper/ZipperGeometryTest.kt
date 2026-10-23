package dev.dimvlachos.lab.core.presentation.components.zipper

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ZipperGeometryTest {
    @Test
    fun nearThePullTheGapIsAsItWas() {
        // Up to where the gap is fully prised open, nothing splays further.
        for (d in listOf(0f, 10f, 20f, 26f)) assertEquals(minOf(9f, 1f + d * .3f), zipperHalfGap(d))
        assertEquals(16f, zipperOuterHalfWidth(-10f))
    }

    @Test
    fun theOpenedEndFlaresOnlyALittleWiderTowardTheTop() {
        var last = zipperOuterHalfWidth(0f)
        for (i in 1..100) {
            val w = zipperOuterHalfWidth(i * 5f)
            assertTrue(w >= last, "narrower at ${i * 5f} dp")
            last = w
        }
        val fullyPrised = zipperOuterHalfWidth(30f)
        val top = zipperOuterHalfWidth(500f)
        assertTrue(top > fullyPrised + 2.5f, "it hardly flares: $fullyPrised to $top")
        // Only a little: the whole opened end is at most a fifth wider than where it is prised.
        assertTrue(top <= fullyPrised * 1.2f, "it flares too far: $fullyPrised to $top")
        // And it stays inside the list area, which ends ZipperInset to the right of the middle.
        assertTrue(top < ZipperInset - 2f)
    }

    @Test
    fun theTapeKeepsItsWidthAsItSplays() {
        // From the lining's edge (the gap plus 3) out to the outer edge.
        val at = { d: Float -> zipperOuterHalfWidth(d) - (zipperHalfGap(d) + 3f) }
        assertEquals(at(30f), at(500f), .001f)
    }

    @Test
    fun theLiningReachesTheFlaredHalves() {
        assertTrue(zipperHalfGap(1000f) + 3f <= 12f + ZipperFlare + .001f)
    }
}
