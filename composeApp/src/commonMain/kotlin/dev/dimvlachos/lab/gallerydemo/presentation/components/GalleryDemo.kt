package dev.dimvlachos.lab.gallerydemo.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
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
import dev.dimvlachos.lab.resources.profile_title
import org.jetbrains.compose.resources.stringResource

// Matches Paxos and Naxos: two results, side by side in the first row.
private const val DemoQuery = "xos"

@Composable
internal fun GalleryDemo(state: DemoState) {
    ProfileGallery(
        title = stringResource(Res.string.profile_title),
        name = stringResource(Res.string.profile_name),
        portrait = Res.drawable.portrait,
        photos = rememberIslandPhotos(),
        searchQuery = DemoQuery,
        scene = sceneForIndex(state.selectedIndex),
        onSceneChange = { state.select(indexForScene(it)) },
        modifier = Modifier.fillMaxSize().background(LabTheme.colors.background),
    )
}
