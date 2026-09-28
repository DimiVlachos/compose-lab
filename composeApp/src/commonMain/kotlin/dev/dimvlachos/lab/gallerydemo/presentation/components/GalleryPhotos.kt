package dev.dimvlachos.lab.gallerydemo.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import dev.dimvlachos.lab.core.presentation.components.imagemorph.MorphPhoto
import dev.dimvlachos.lab.imagemorphdemo.presentation.components.rememberIslandPhotos
import dev.dimvlachos.lab.resources.Res
import dev.dimvlachos.lab.resources.photo_crete
import dev.dimvlachos.lab.resources.photo_crete_caption
import dev.dimvlachos.lab.resources.photo_crete_title
import dev.dimvlachos.lab.resources.photo_folegandros
import dev.dimvlachos.lab.resources.photo_folegandros_caption
import dev.dimvlachos.lab.resources.photo_folegandros_title
import dev.dimvlachos.lab.resources.photo_kefalonia
import dev.dimvlachos.lab.resources.photo_kefalonia_caption
import dev.dimvlachos.lab.resources.photo_kefalonia_title
import dev.dimvlachos.lab.resources.photo_mykonos
import dev.dimvlachos.lab.resources.photo_mykonos_caption
import dev.dimvlachos.lab.resources.photo_mykonos_title
import dev.dimvlachos.lab.resources.photo_rhodes
import dev.dimvlachos.lab.resources.photo_rhodes_caption
import dev.dimvlachos.lab.resources.photo_rhodes_title
import dev.dimvlachos.lab.resources.photo_zakynthos
import dev.dimvlachos.lab.resources.photo_zakynthos_caption
import dev.dimvlachos.lab.resources.photo_zakynthos_title
import org.jetbrains.compose.resources.stringResource

// The image morph's six islands and six more, so the gallery has a grid worth scrolling. The
// layer demos keep their six: their grid fills the stage, and twelve cards would be thumbnails.
@Composable
internal fun rememberGalleryPhotos(): List<MorphPhoto> {
    val islands = rememberIslandPhotos()
    val titles =
        listOf(
            stringResource(Res.string.photo_mykonos_title),
            stringResource(Res.string.photo_crete_title),
            stringResource(Res.string.photo_rhodes_title),
            stringResource(Res.string.photo_zakynthos_title),
            stringResource(Res.string.photo_kefalonia_title),
            stringResource(Res.string.photo_folegandros_title),
        )
    val captions =
        listOf(
            stringResource(Res.string.photo_mykonos_caption),
            stringResource(Res.string.photo_crete_caption),
            stringResource(Res.string.photo_rhodes_caption),
            stringResource(Res.string.photo_zakynthos_caption),
            stringResource(Res.string.photo_kefalonia_caption),
            stringResource(Res.string.photo_folegandros_caption),
        )
    return remember(islands, titles, captions) {
        islands +
            listOf(
                    Res.drawable.photo_mykonos,
                    Res.drawable.photo_crete,
                    Res.drawable.photo_rhodes,
                    Res.drawable.photo_zakynthos,
                    Res.drawable.photo_kefalonia,
                    Res.drawable.photo_folegandros,
                )
                .mapIndexed { i, image -> MorphPhoto(image, titles[i], captions[i]) }
    }
}
