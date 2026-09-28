package dev.dimvlachos.lab.gallerydemo

import dev.dimvlachos.lab.core.presentation.components.gallery.GalleryScene

private const val PhotoCount = 6
private const val Avatar = 7
private const val AvatarDialog = 8
private const val Search = 9

// The script speaks in select(n); these are the scenes n stands for. 1-6 open a photo, 7 zooms the
// avatar, 8 adds the dialog, 9 opens search, and 10-15 open photo n - 9 from the search results.
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
