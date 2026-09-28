package dev.dimvlachos.lab.imagemorphdemo.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import dev.dimvlachos.lab.core.presentation.components.imagemorph.MorphPhoto
import dev.dimvlachos.lab.resources.Res
import dev.dimvlachos.lab.resources.photo_corfu
import dev.dimvlachos.lab.resources.photo_corfu_caption
import dev.dimvlachos.lab.resources.photo_corfu_title
import dev.dimvlachos.lab.resources.photo_hydra
import dev.dimvlachos.lab.resources.photo_hydra_caption
import dev.dimvlachos.lab.resources.photo_hydra_title
import dev.dimvlachos.lab.resources.photo_milos
import dev.dimvlachos.lab.resources.photo_milos_caption
import dev.dimvlachos.lab.resources.photo_milos_title
import dev.dimvlachos.lab.resources.photo_naxos
import dev.dimvlachos.lab.resources.photo_naxos_caption
import dev.dimvlachos.lab.resources.photo_naxos_title
import dev.dimvlachos.lab.resources.photo_paxos
import dev.dimvlachos.lab.resources.photo_paxos_caption
import dev.dimvlachos.lab.resources.photo_paxos_title
import dev.dimvlachos.lab.resources.photo_santorini
import dev.dimvlachos.lab.resources.photo_santorini_caption
import dev.dimvlachos.lab.resources.photo_santorini_title
import org.jetbrains.compose.resources.stringResource

// The six bundled island photos with their titles and captions, shared by the morph demos.
@Composable
internal fun rememberIslandPhotos(): List<MorphPhoto> {
    val titles =
        listOf(
            stringResource(Res.string.photo_corfu_title),
            stringResource(Res.string.photo_paxos_title),
            stringResource(Res.string.photo_santorini_title),
            stringResource(Res.string.photo_milos_title),
            stringResource(Res.string.photo_naxos_title),
            stringResource(Res.string.photo_hydra_title),
        )
    val captions =
        listOf(
            stringResource(Res.string.photo_corfu_caption),
            stringResource(Res.string.photo_paxos_caption),
            stringResource(Res.string.photo_santorini_caption),
            stringResource(Res.string.photo_milos_caption),
            stringResource(Res.string.photo_naxos_caption),
            stringResource(Res.string.photo_hydra_caption),
        )
    return remember(titles, captions) {
        listOf(
                Res.drawable.photo_corfu,
                Res.drawable.photo_paxos,
                Res.drawable.photo_santorini,
                Res.drawable.photo_milos,
                Res.drawable.photo_naxos,
                Res.drawable.photo_hydra,
            )
            .mapIndexed { i, image -> MorphPhoto(image, titles[i], captions[i]) }
    }
}
