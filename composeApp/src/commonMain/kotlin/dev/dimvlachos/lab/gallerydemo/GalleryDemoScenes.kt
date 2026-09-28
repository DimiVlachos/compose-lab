package dev.dimvlachos.lab.gallerydemo

import dev.dimvlachos.lab.core.presentation.components.gallery.GalleryScene

private const val PhotoCount = 12
private const val Avatar = 100
private const val AvatarDialog = 101
private const val Search = 200

// The script speaks in select(n); these are the scenes n stands for. 1-12 open a photo, 100 zooms
// the avatar, 101 adds the dialog, 200 opens search, and 200 + n opens photo n from the results.
// The avatar and search numbers sit well clear of the photos, so adding photos never collides.
internal fun sceneForIndex(index: Int): GalleryScene =
    when (index) {
        in 1..PhotoCount -> GalleryScene(photo = index)
        Avatar -> GalleryScene(avatar = true)
        AvatarDialog -> GalleryScene(avatar = true, dialog = true)
        Search -> GalleryScene(search = true)
        in Search + 1..Search + PhotoCount -> GalleryScene(photo = index - Search, search = true)
        else -> GalleryScene()
    }

internal fun indexForScene(scene: GalleryScene): Int =
    when {
        scene.photo > 0 -> scene.photo + if (scene.search) Search else 0
        scene.avatar -> if (scene.dialog) AvatarDialog else Avatar
        scene.search -> Search
        else -> 0
    }
