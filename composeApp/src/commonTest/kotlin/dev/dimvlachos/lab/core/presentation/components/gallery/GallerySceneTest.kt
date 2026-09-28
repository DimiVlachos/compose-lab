package dev.dimvlachos.lab.core.presentation.components.gallery

import kotlin.test.Test
import kotlin.test.assertEquals

class GallerySceneTest {
    @Test
    fun legalScenesAreKept() {
        for (scene in
            listOf(
                GalleryScene(),
                GalleryScene(photo = 6),
                GalleryScene(avatar = true),
                GalleryScene(avatar = true, dialog = true),
                GalleryScene(search = true),
                GalleryScene(photo = 5, search = true),
            )) {
            assertEquals(scene, scene.normalized(photoCount = 6))
        }
    }

    @Test
    fun anOutOfRangePhotoMeansNoPhoto() {
        assertEquals(GalleryScene(), GalleryScene(photo = 7).normalized(6))
        assertEquals(GalleryScene(), GalleryScene(photo = -1).normalized(6))
    }

    @Test
    fun aPhotoWinsOverTheAvatar() {
        assertEquals(
            GalleryScene(photo = 2),
            GalleryScene(photo = 2, avatar = true, dialog = true).normalized(6),
        )
    }

    @Test
    fun theDialogNeedsTheAvatar() {
        assertEquals(GalleryScene(), GalleryScene(dialog = true).normalized(6))
    }

    @Test
    fun theAvatarIsHiddenDuringSearch() {
        assertEquals(
            GalleryScene(search = true),
            GalleryScene(avatar = true, dialog = true, search = true).normalized(6),
        )
    }
}
