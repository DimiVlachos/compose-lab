package dev.dimvlachos.lab.gallerydemo

import dev.dimvlachos.lab.core.presentation.components.gallery.GalleryScene
import kotlin.test.Test
import kotlin.test.assertEquals

class GalleryDemoScenesTest {
    private val scriptIndexes = (0..12) + listOf(100, 101, 200) + (201..212)

    @Test
    fun everyScriptIndexRoundTrips() {
        for (index in scriptIndexes) assertEquals(index, indexForScene(sceneForIndex(index)))
    }

    @Test
    fun theTableMapsAsDocumented() {
        assertEquals(GalleryScene(photo = 3), sceneForIndex(3))
        assertEquals(GalleryScene(photo = 12), sceneForIndex(12))
        assertEquals(GalleryScene(avatar = true), sceneForIndex(100))
        assertEquals(GalleryScene(avatar = true, dialog = true), sceneForIndex(101))
        assertEquals(GalleryScene(search = true), sceneForIndex(200))
        assertEquals(GalleryScene(photo = 5, search = true), sceneForIndex(205))
    }

    @Test
    fun anythingElseIsTheBaseScreen() {
        for (index in listOf(-1, 13, 99, 102, 199, 213)) {
            assertEquals(GalleryScene(), sceneForIndex(index))
        }
    }
}
