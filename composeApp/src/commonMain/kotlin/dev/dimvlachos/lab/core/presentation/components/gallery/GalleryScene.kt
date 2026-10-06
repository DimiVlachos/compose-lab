package dev.dimvlachos.lab.core.presentation.components.gallery

import androidx.compose.runtime.Immutable

/**
 * What is open on the [ProfileGallery] screen: a photo ([photo] is 1-based, 0 for none), the zoomed
 * avatar and, over it, the change-photo [dialog], and the [search] bar. Any value is accepted; the
 * screen shows the nearest legal scene.
 */
@Immutable
public data class GalleryScene(
    val photo: Int = 0,
    val avatar: Boolean = false,
    val dialog: Boolean = false,
    val search: Boolean = false,
)

// A photo wins over the avatar, the avatar is hidden while searching (its header is folded away),
// and the dialog only exists over the avatar.
internal fun GalleryScene.normalized(photoCount: Int): GalleryScene {
    val photo = if (photo in 1..photoCount) photo else 0
    val avatar = avatar && photo == 0 && !search
    return GalleryScene(photo = photo, avatar = avatar, dialog = dialog && avatar, search = search)
}

/**
 * The scene one step back, closing the topmost layer: the dialog before the avatar under it, a
 * photo before the search it was opened from, then the avatar or search. The bare screen stays.
 */
internal fun GalleryScene.back(): GalleryScene =
    when {
        dialog -> copy(dialog = false)
        photo != 0 -> copy(photo = 0)
        avatar -> copy(avatar = false)
        search -> copy(search = false)
        else -> this
    }
