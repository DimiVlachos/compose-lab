package dev.dimvlachos.lab.gallerydemo

import dev.dimvlachos.lab.core.presentation.components.gallery.GalleryScene
import kotlin.test.Test
import kotlin.test.assertEquals

class GalleryDemoScenesTest {
    @Test
    fun everyScriptIndexRoundTrips() {
        for (index in 0..15) assertEquals(index, indexForScene(sceneForIndex(index)))
    }

    @Test
    fun theTableMapsAsDocumented() {
        assertEquals(GalleryScene(photo = 3), sceneForIndex(3))
        assertEquals(GalleryScene(avatar = true), sceneForIndex(7))
        assertEquals(GalleryScene(avatar = true, dialog = true), sceneForIndex(8))
        assertEquals(GalleryScene(search = true), sceneForIndex(9))
        assertEquals(GalleryScene(photo = 5, search = true), sceneForIndex(14))
    }

    @Test
    fun anythingElseIsTheBaseScreen() {
        for (index in listOf(-1, 16, 99)) assertEquals(GalleryScene(), sceneForIndex(index))
    }
}
