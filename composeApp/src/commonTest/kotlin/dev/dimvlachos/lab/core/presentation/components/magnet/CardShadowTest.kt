package dev.dimvlachos.lab.core.presentation.components.magnet

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CardShadowTest {
    private val lifts = (0..10).map { it / 10f }

    @Test
    fun liftedTheShadowsSpreadFallFurtherAndFade() {
        for ((low, high) in lifts.zipWithNext()) {
            assertTrue(CardShadow.ambientBlur(high) > CardShadow.ambientBlur(low))
            assertTrue(CardShadow.ambientDrop(high) > CardShadow.ambientDrop(low))
            assertTrue(CardShadow.ambientAlpha(high) < CardShadow.ambientAlpha(low))
            assertTrue(CardShadow.ambientSpread(high) >= CardShadow.ambientSpread(low))
            assertTrue(CardShadow.contactBlur(high) > CardShadow.contactBlur(low))
            assertTrue(CardShadow.contactDrop(high) > CardShadow.contactDrop(low))
            assertTrue(CardShadow.contactAlpha(high) < CardShadow.contactAlpha(low))
        }
    }

    @Test
    fun theContactShadowIsTighterCloserAndDarkerThanTheWideOne() {
        for (lift in lifts) {
            assertTrue(CardShadow.contactBlur(lift) < CardShadow.ambientBlur(lift), "$lift")
            assertTrue(CardShadow.contactDrop(lift) < CardShadow.ambientDrop(lift), "$lift")
        }
        assertTrue(CardShadow.contactAlpha(0f) > CardShadow.ambientAlpha(0f))
        // Lying flat, the shadow falls only a little way below the card, as from a light above.
        assertTrue(CardShadow.ambientDrop(0f) <= 4f && CardShadow.contactDrop(0f) <= 2f)
        assertTrue(CardShadow.Slant in 0f..0.5f)
    }

    @Test
    fun aCardInAFingerIsLiftedAllTheWayAndLandsAgainWhenLetGo() {
        val table = MagnetState(TestPhotos, TestTags).apply { layOut(720, 1440, 2f) }
        val body = table.bodies.bodies.first { it.id == "none" }
        assertEquals(0f, body.lift)
        assertTrue(table.grabPhoto(table.photoPosition("none")!!) != null)
        repeat(30) { table.advance(1f / 60f) }
        assertEquals(1f, body.lift, 0.01f)
        table.dropPhoto()
        repeat(6) { table.advance(1f / 60f) }
        assertTrue(body.lift in 0.01f..0.9f, "it eases down, not drops: ${body.lift}")
        repeat(60) { table.advance(1f / 60f) }
        assertEquals(0f, body.lift, 0.01f)
    }

    @Test
    fun anOpeningPhotoLetsGoOfItsCardsBorderClipAndShadowSmoothly() {
        assertEquals(1f, openBorder(0f))
        assertEquals(0f, openBorder(1f))
        assertEquals(1f, openClipAlpha(0f))
        assertEquals(0f, openClipAlpha(1f))
        for ((low, high) in lifts.zipWithNext()) {
            assertTrue(openBorder(high) < openBorder(low))
            assertTrue(openClipAlpha(high) <= openClipAlpha(low))
            assertTrue(openShadowAlpha(high) < openShadowAlpha(low))
        }
        // No jump on the first frames: a hundredth of the way open, nearly all of it still shows.
        assertTrue(openBorder(0.01f) > 0.95f && openClipAlpha(0.01f) > 0.95f)
    }
}
