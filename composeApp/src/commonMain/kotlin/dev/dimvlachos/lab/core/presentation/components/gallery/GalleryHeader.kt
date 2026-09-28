package dev.dimvlachos.lab.core.presentation.components.gallery

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import dev.dimvlachos.lab.core.presentation.components.profile.AvatarSize
import dev.dimvlachos.lab.core.presentation.ui.LabTheme
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.collectLatest

// Expanded: 16 top + the 88 dp avatar + 4 bottom. Collapsed: 16 top + a 40 dp row + 8 bottom,
// Edda's bar padding. The difference is how far the grid scrolls to collapse it. The grid keeps
// GridTopGap below the header, so it rests 16 dp under the bar's shadow line once collapsed and
// 20 dp under the avatar when expanded.
internal val HeaderExpandedHeight = 108.dp
internal val GridTopGap = 16.dp
internal val HeaderCollapsedHeight = 64.dp
internal const val GalleryHeaderTag = "galleryHeader"
private val HeaderTop = 16.dp
private val HeaderExpandedBottom = 4.dp
private val HeaderCollapsedBottom = 8.dp
private val HeaderSide = 20.dp
private val HeaderGap = 16.dp
private val CollapsedAvatarSize = 40.dp // the search button's size
private val SearchButtonSize = 40.dp
private val HeaderShadowElevation = 8.dp // Edda's bar shadow
private const val HeaderShadowMs = 200

/** How far the header has collapsed, 0 to 1, over the first [rangePx] of the grid's scroll. */
internal fun headerCollapse(scrollPx: Int, rangePx: Float): Float =
    if (rangePx <= 0f) 1f else (scrollPx / rangePx).coerceIn(0f, 1f)

// Edda's rule: the bar only needs a shadow once content is passing under it, which here is once
// the header has fully become the bar.
internal fun headerShadowShows(collapse: Float): Boolean = collapse >= 0.999f

/**
 * The profile header, which becomes the screen's top bar as the grid scrolls: the avatar shrinks
 * from 88 dp to the search button's 40 dp, the photo count fades, and the padding tightens to a
 * bar's. Everything follows [collapse], read only in layout and in layer blocks, so scrolling
 * relayouts and redraws the header without recomposing it. Once fully collapsed it takes Edda's bar
 * shadow, faded in over 200 ms.
 */
@Composable
internal fun CollapsingHeader(
    collapse: () -> Float,
    name: String,
    count: String,
    avatar: @Composable () -> Unit,
    search: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shadow = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        snapshotFlow { headerShadowShows(collapse()) }
            .collectLatest { shows ->
                shadow.animateTo(if (shows) 1f else 0f, tween(HeaderShadowMs))
            }
    }
    Layout(
        modifier =
            modifier
                .testTag(GalleryHeaderTag)
                .fillMaxWidth()
                .graphicsLayer {
                    shadowElevation = shadow.value * HeaderShadowElevation.toPx()
                    shape = RectangleShape
                    clip = false
                }
                .background(LabTheme.colors.background),
        content = {
            Box { avatar() }
            Text(
                name,
                color = LabTheme.colors.textPrimary,
                style = LabTheme.typography.subtitle,
                maxLines = 1,
                modifier = Modifier.testTag(ProfileNameTag),
            )
            Text(
                count,
                color = LabTheme.colors.textMuted,
                style = LabTheme.typography.body,
                maxLines = 1,
                modifier = Modifier.graphicsLayer { alpha = 1f - collapse() },
            )
            Box { search() }
        },
    ) { measurables, constraints ->
        val f = collapse()
        val side = HeaderSide.roundToPx()
        val gap = HeaderGap.roundToPx()
        val top = HeaderTop.roundToPx()
        val bottom = lerp(HeaderExpandedBottom, HeaderCollapsedBottom, f).roundToPx()
        val avatarPx = lerp(AvatarSize, CollapsedAvatarSize, f).roundToPx()
        val searchPx = SearchButtonSize.roundToPx()
        val avatar = measurables[0].measure(Constraints.fixed(avatarPx, avatarPx))
        val search = measurables[3].measure(Constraints.fixed(searchPx, searchPx))
        val textWidth =
            (constraints.maxWidth - 2 * side - avatarPx - searchPx - 2 * gap).coerceAtLeast(0)
        val name = measurables[1].measure(Constraints(maxWidth = textWidth))
        val count = measurables[2].measure(Constraints(maxWidth = textWidth))
        // The count gives its line up as it fades, so the name settles in the middle of the bar.
        val textHeight = name.height + ((1f - f) * count.height).roundToInt()
        val content = maxOf(avatarPx, searchPx, textHeight)
        layout(constraints.maxWidth, top + content + bottom) {
            avatar.place(side, top + (content - avatarPx) / 2)
            val textTop = top + (content - textHeight) / 2
            name.place(side + avatarPx + gap, textTop)
            count.place(side + avatarPx + gap, textTop + name.height)
            search.place(constraints.maxWidth - side - searchPx, top + (content - searchPx) / 2)
        }
    }
}
