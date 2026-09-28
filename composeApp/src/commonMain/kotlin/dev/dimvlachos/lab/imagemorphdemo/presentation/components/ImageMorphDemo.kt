package dev.dimvlachos.lab.imagemorphdemo.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.dimvlachos.lab.core.demo.DemoState
import dev.dimvlachos.lab.core.presentation.components.imagemorph.ImageMorph
import dev.dimvlachos.lab.core.presentation.components.imagemorph.MorphLayers
import dev.dimvlachos.lab.core.presentation.ui.LabTheme

// The script's select(n) is the tap on card n; select(0) is the close. Index 0 is the grid.
@Composable
internal fun ImageMorphDemo(state: DemoState, layers: MorphLayers) {
    val photos = rememberIslandPhotos()
    ImageMorph(
        photos = photos,
        expandedIndex = state.selectedIndex,
        onExpand = state::select,
        onCollapse = { state.select(0) },
        modifier = Modifier.fillMaxSize().background(LabTheme.colors.background),
        layers = layers,
    )
}
