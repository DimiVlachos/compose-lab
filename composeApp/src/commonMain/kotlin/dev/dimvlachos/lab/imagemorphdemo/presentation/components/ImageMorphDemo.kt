package dev.dimvlachos.lab.imagemorphdemo.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import dev.dimvlachos.lab.core.demo.DemoState
import dev.dimvlachos.lab.core.presentation.components.imagemorph.ImageMorph
import dev.dimvlachos.lab.core.presentation.components.imagemorph.MorphLayers
import dev.dimvlachos.lab.core.presentation.components.imagemorph.MorphPhoto
import dev.dimvlachos.lab.core.presentation.ui.LabTheme
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

// The script's select(n) is the tap on card n; select(0) is the close. Index 0 is the grid.
@Composable
internal fun ImageMorphDemo(state: DemoState, layers: MorphLayers) {
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
    val photos =
        remember(titles, captions) {
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
    ImageMorph(
        photos = photos,
        expandedIndex = state.selectedIndex,
        onExpand = state::select,
        onCollapse = { state.select(0) },
        modifier = Modifier.fillMaxSize().background(LabTheme.colors.background),
        layers = layers,
    )
}
