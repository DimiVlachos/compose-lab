package dev.dimvlachos.lab.core.presentation.components.profile

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.dimvlachos.lab.core.presentation.ui.LabTheme
import dev.dimvlachos.lab.resources.Res
import dev.dimvlachos.lab.resources.portrait
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.imageResource

internal const val ProfileNameTag = "profileName"
private val AvatarTop = 96.dp
private val SearchButtonSlot = 40.dp
private val FabSlot = 56.dp

/**
 * A minimal profile screen with three shared-element morphs: the avatar grows into a large circle
 * (1), the search button into a search bar that types its own query (2), and the FAB into an "Add
 * image" dialog (3). One index drives them; 0 or anything else shows the screen alone. Each source
 * keeps its slot's size while hidden, so the screen never reflows under a morph.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun ProfileMorphs(
    title: String,
    name: String,
    portrait: DrawableResource,
    searchQuery: String,
    searchCandidates: List<String>,
    openIndex: Int,
    onOpen: (Int) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val open = if (openIndex in 1..3) openIndex else 0
    // One painter for both avatar ends, decoded once (see ImageMorph).
    val bitmap = imageResource(portrait)
    val painter = remember(bitmap) { BitmapPainter(bitmap) }
    val current = rememberUpdatedState(open)
    val searchOpening = remember { { current.value == 2 } }
    SharedTransitionLayout(modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize()) {
            Row(
                Modifier.fillMaxWidth().padding(LabTheme.spacing.medium),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    title,
                    color = LabTheme.colors.textPrimary,
                    style = LabTheme.typography.subtitle,
                )
                SourceSlot(open != 2, Modifier.size(SearchButtonSlot)) {
                    SearchSource(this@SharedTransitionLayout, this, searchOpening, { onOpen(2) })
                }
            }
            Column(
                Modifier.align(Alignment.TopCenter).padding(top = AvatarTop),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(LabTheme.spacing.smallMedium),
            ) {
                SourceSlot(open != 1, Modifier.size(AvatarSize)) {
                    AvatarSource(painter, this@SharedTransitionLayout, this, { onOpen(1) })
                }
                Text(
                    name,
                    color = LabTheme.colors.textPrimary,
                    style = LabTheme.typography.subtitle,
                    modifier = Modifier.testTag(ProfileNameTag),
                )
            }
            SourceSlot(
                open != 3,
                Modifier.align(Alignment.BottomEnd)
                    .padding(LabTheme.spacing.mediumLarge)
                    .size(FabSlot),
            ) {
                FabSource(this@SharedTransitionLayout, this, { onOpen(3) })
            }
            AnimatedVisibility(
                open == 1,
                enter = EnterTransition.None,
                exit = ExitTransition.None,
            ) {
                AvatarTarget(painter, this@SharedTransitionLayout, this, onClose)
            }
            AnimatedVisibility(
                open == 2,
                enter = EnterTransition.None,
                exit = ExitTransition.None,
            ) {
                SearchTarget(
                    searchQuery,
                    searchCandidates,
                    this@SharedTransitionLayout,
                    this,
                    searchOpening,
                    onClose,
                )
            }
            AnimatedVisibility(
                open == 3,
                enter = EnterTransition.None,
                exit = ExitTransition.None,
            ) {
                FabDialogTarget(this@SharedTransitionLayout, this, onClose)
            }
        }
    }
}

// A fixed-size slot, so the screen keeps its layout while the source is away in its target; and a
// plain Box scope, so the top-level AnimatedVisibility is the one called, not the Row's or
// Column's.
@Composable
private fun SourceSlot(
    visible: Boolean,
    modifier: Modifier,
    content: @Composable AnimatedVisibilityScope.() -> Unit,
) {
    Box(modifier) {
        AnimatedVisibility(
            visible,
            enter = EnterTransition.None,
            exit = ExitTransition.None,
            content = content,
        )
    }
}

@Preview
@Composable
private fun ProfileMorphsPreview() {
    LabTheme {
        var open by remember { mutableIntStateOf(0) }
        ProfileMorphs(
            title = "Profile",
            name = "Alex Morgan",
            portrait = Res.drawable.portrait,
            searchQuery = "Cor",
            searchCandidates = listOf("Corfu", "Corfu Old Town", "Corinth", "Naxos", "Paxos"),
            openIndex = open,
            onOpen = { open = it },
            onClose = { open = 0 },
        )
    }
}
