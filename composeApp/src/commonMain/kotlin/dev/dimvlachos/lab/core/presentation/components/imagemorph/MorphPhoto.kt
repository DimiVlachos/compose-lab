package dev.dimvlachos.lab.core.presentation.components.imagemorph

import androidx.compose.runtime.Immutable
import org.jetbrains.compose.resources.DrawableResource

/**
 * A photo that morphs between its grid card and its full-screen detail.
 *
 * @property image The picture.
 * @property title Shown on the card and the detail.
 * @property caption Shown under the title on the detail.
 */
@Immutable
public data class MorphPhoto(val image: DrawableResource, val title: String, val caption: String)
