package dev.dimvlachos.lab.core.presentation.components.searchmorph

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dev.dimvlachos.lab.core.presentation.components.imagemorph.MorphEnd
import dev.dimvlachos.lab.core.presentation.components.imagemorph.ReportMorphComposition
import dev.dimvlachos.lab.core.presentation.components.imagemorph.ShapedMorphNode
import dev.dimvlachos.lab.core.presentation.components.imagemorph.morphBoundsTransform
import dev.dimvlachos.lab.core.presentation.components.imagemorph.morphChrome
import dev.dimvlachos.lab.core.presentation.components.imagemorph.morphWidthFraction
import dev.dimvlachos.lab.core.presentation.ui.LabTheme
import dev.dimvlachos.lab.resources.Res
import dev.dimvlachos.lab.resources.action_back
import dev.dimvlachos.lab.resources.ic_arrow_back
import dev.dimvlachos.lab.resources.ic_search
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

internal val SearchButtonSize = 40.dp
internal val SearchPillHeight = 48.dp
private val SearchIconSize = 20.dp
private val PillShape = RoundedCornerShape(50)

/**
 * The shared-element keys of one search morph: the [container] that grows from the round button
 * into the pill, and the magnifier [icon] that rides from the button's centre to the pill's leading
 * slot. Each screen with a search morph needs keys of its own.
 */
internal class SearchMorphKeys(val container: String, val icon: String)

// The magnifier is its own shared element, above the container, travelling from the button's
// centre to the pill's leading slot while the container grows around it.
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun Magnifier(
    keys: SearchMorphKeys,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    opening: () -> Boolean,
) {
    with(sharedTransitionScope) {
        Icon(
            painter = painterResource(Res.drawable.ic_search),
            contentDescription = null,
            tint = LabTheme.colors.textPrimary,
            modifier =
                Modifier.sharedElement(
                        sharedContentState = rememberSharedContentState(keys.icon),
                        animatedVisibilityScope = animatedVisibilityScope,
                        boundsTransform = remember(opening) { morphBoundsTransform(opening) },
                        zIndexInOverlay = 2f,
                    )
                    .size(SearchIconSize),
        )
    }
}

/**
 * The closed end of a search morph: a round 40 dp button in the bar colour with the magnifier in
 * the middle. [opening] says which way the morph is going, read on each frame; [contentDescription]
 * names the button for accessibility, if the screen has no other name for it.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
internal fun MorphSearchButton(
    keys: SearchMorphKeys,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    opening: () -> Boolean,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
) {
    ReportMorphComposition(MorphEnd.Card)
    ShapedMorphNode(
        sharedTransitionScope,
        animatedVisibilityScope,
        keys.container,
        PillShape,
        modifier
            .size(SearchButtonSize)
            .then(
                if (contentDescription != null) {
                    Modifier.semantics { this.contentDescription = contentDescription }
                } else {
                    Modifier
                }
            )
            .clickable(role = Role.Button, onClick = onOpen),
        color = LabTheme.colors.bar,
    ) {
        Box(Modifier.align(Alignment.Center)) {
            Magnifier(keys, sharedTransitionScope, animatedVisibilityScope, opening)
        }
    }
}

/**
 * The open end of a search morph: a back arrow ([onClose]) and the button grown into a 48 dp pill,
 * the magnifier at its start, then the [query] and anything [trailing] it. The arrow, the query and
 * the trailing pieces fade in with the pill as it lands, through the chrome modifier their slots
 * are handed; [fraction] is the pill's width as a share of its final width, for a screen that waits
 * for the pill to land. [modifier] goes on the row holding the arrow and the pill.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
internal fun MorphSearchBar(
    keys: SearchMorphKeys,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    opening: () -> Boolean,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    fraction: MutableFloatState = remember { mutableFloatStateOf(1f) },
    backTag: String = SearchBarBackTag,
    pillTag: String = SearchBarPillTag,
    trailing: @Composable RowScope.(chrome: Modifier) -> Unit = {},
    query: @Composable RowScope.(chrome: Modifier) -> Unit,
) {
    ReportMorphComposition(MorphEnd.Detail)
    val chrome = Modifier.morphChrome(animatedVisibilityScope, fraction)
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(
            chrome
                .size(SearchButtonSize)
                .clip(CircleShape)
                .testTag(backTag)
                .clickable(role = Role.Button, onClick = onClose),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painterResource(Res.drawable.ic_arrow_back),
                stringResource(Res.string.action_back),
                tint = LabTheme.colors.textPrimary,
            )
        }
        ShapedMorphNode(
            sharedTransitionScope,
            animatedVisibilityScope,
            keys.container,
            PillShape,
            Modifier.weight(1f).height(SearchPillHeight).testTag(pillTag),
            overlayZIndex = 1f,
            color = LabTheme.colors.bar,
        ) {
            Row(
                // The whole pill, so the fraction is the pill's own (a Row wrapping its
                // content would reach its final width long before the pill does) and the
                // row centres in its height.
                Modifier.fillMaxSize()
                    .morphWidthFraction(fraction)
                    .padding(horizontal = LabTheme.spacing.medium),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(LabTheme.spacing.smallMedium),
            ) {
                Magnifier(keys, sharedTransitionScope, animatedVisibilityScope, opening)
                query(chrome)
                trailing(chrome)
            }
        }
    }
}

internal const val SearchBarBackTag = "searchBack"
internal const val SearchBarPillTag = "searchPill"
