package dev.dimvlachos.lab.gallerydemo.presentation.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import dev.dimvlachos.lab.core.demo.DemoState
import dev.dimvlachos.lab.core.presentation.components.gallery.ProfileGallery
import dev.dimvlachos.lab.core.presentation.ui.LabTheme
import dev.dimvlachos.lab.gallerydemo.indexForScene
import dev.dimvlachos.lab.gallerydemo.sceneForIndex
import dev.dimvlachos.lab.imagemorphdemo.presentation.components.rememberIslandPhotos
import dev.dimvlachos.lab.resources.Res
import dev.dimvlachos.lab.resources.portrait
import dev.dimvlachos.lab.resources.profile_name
import org.jetbrains.compose.resources.stringResource

// Matches Paxos and Naxos: two results, side by side in the first row.
private const val DemoQuery = "xos"

@Composable
internal fun GalleryDemo(state: DemoState) {
    val scroll = rememberScrollState()
    // The script's scrollBy drags the grid, so the header collapses exactly as under a finger.
    DisposableEffect(state, scroll) {
        state.setScrollHandler { px ->
            scroll.animateScrollBy(px, tween(durationMillis = 1_000, easing = FastOutSlowInEasing))
        }
        onDispose { state.setScrollHandler(null) }
    }
    ProfileGallery(
        name = stringResource(Res.string.profile_name),
        portrait = Res.drawable.portrait,
        photos = rememberIslandPhotos(),
        searchQuery = DemoQuery,
        scene = sceneForIndex(state.selectedIndex),
        onSceneChange = { state.select(indexForScene(it)) },
        modifier = Modifier.fillMaxSize().background(LabTheme.colors.background),
        scrollState = scroll,
    )
}
