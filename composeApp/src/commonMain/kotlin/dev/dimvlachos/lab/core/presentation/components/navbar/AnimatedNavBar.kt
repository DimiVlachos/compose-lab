package dev.dimvlachos.lab.core.presentation.components.navbar

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.util.lerp
import dev.dimvlachos.lab.core.presentation.ui.AppColors
import dev.dimvlachos.lab.core.presentation.ui.LabTheme
import dev.dimvlachos.lab.resources.Res
import dev.dimvlachos.lab.resources.ic_home
import dev.dimvlachos.lab.resources.ic_home_filled
import dev.dimvlachos.lab.resources.ic_profile
import dev.dimvlachos.lab.resources.ic_profile_filled
import dev.dimvlachos.lab.resources.ic_saved
import dev.dimvlachos.lab.resources.ic_saved_filled
import dev.dimvlachos.lab.resources.ic_search
import dev.dimvlachos.lab.resources.nav_home
import dev.dimvlachos.lab.resources.nav_profile
import dev.dimvlachos.lab.resources.nav_saved
import dev.dimvlachos.lab.resources.nav_search
import kotlin.math.max
import kotlin.math.roundToInt
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun AnimatedNavBar(
    items: List<NavItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    layers: NavBarLayers = NavBarLayers.All,
    scrollState: NavBarScrollState? = null,
) {
    ReportComposition()
    if (items.isEmpty()) return
    val selected = selectedIndex.coerceIn(0, items.lastIndex)
    val itemCount = items.size
    val indicator = remember { IndicatorState(selected) }
    val colors = LabTheme.colors
    val collapse: () -> Float =
        remember(layers.scrollAware, scrollState) {
            if (layers.scrollAware && scrollState != null) {
                { scrollState.collapse }
            } else {
                { 0f }
            }
        }

    LaunchedEffect(selected, layers) {
        if (layers.indicator || layers.cutout) {
            indicator.animateTo(selected, stretch = layers.indicator)
        } else {
            indicator.snapTo(selected)
        }
    }

    Box(
        modifier
            .fillMaxWidth()
            .height(NavBarDimens.BubbleOverhang + NavBarDimens.BarHeight)
            .graphicsLayer { translationY = -collapse() * NavBarDimens.FloatLift.toPx() }
    ) {
        Row(
            Modifier.align(Alignment.BottomCenter)
                .collapsingWidth(itemCount, collapse)
                .height(NavBarDimens.BarHeight)
                .drawBehind { drawBar(itemCount, indicator, layers, colors, collapse) }
                .selectableGroup()
        ) {
            items.forEachIndexed { index, item ->
                NavBarItem(
                    item = item,
                    selected = index == selected,
                    animateIcon = layers.icons,
                    hideIcon = layers.cutout && index == selected,
                    collapse = collapse,
                    onClick = { onSelect(index) },
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                )
            }
        }
        if (layers.cutout) {
            Bubble(
                item = items[selected],
                indicator = indicator,
                itemCount = itemCount,
                stretch = layers.indicator,
                collapse = collapse,
                colors = colors,
            )
        }
    }
}

private fun Modifier.collapsingWidth(itemCount: Int, collapse: () -> Float): Modifier =
    layout { measurable, constraints ->
        val width =
            barWidth(
                constraints.maxWidth,
                (NavBarDimens.CollapsedSlot * itemCount).roundToPx(),
                collapse(),
            )
        val placeable = measurable.measure(constraints.copy(minWidth = width, maxWidth = width))
        layout(width, placeable.height) { placeable.place(0, 0) }
    }

private fun DrawScope.drawBar(
    itemCount: Int,
    indicator: IndicatorState,
    layers: NavBarLayers,
    colors: AppColors,
    collapse: () -> Float,
) {
    val corner = size.height / 2f
    val slot = size.width / itemCount
    val notch =
        if (layers.cutout) {
            val morphed =
                morphedNotchParams(
                    m = collapse(),
                    restCenterY = NavBarDimens.NotchCenterY.toPx(),
                    notchRadius = NavBarDimens.NotchRadius.toPx(),
                    filletRadius = NavBarDimens.NotchFillet.toPx(),
                )
            morphed?.let {
                filletedNotch(
                    centerX = clampNotchCenter(indicator.centerSlot * slot, size.width),
                    centerY = it.centerY,
                    notchRadius = it.notchRadius,
                    filletRadius = it.filletRadius,
                    barWidth = size.width,
                    cornerRadius = corner,
                )
            }
        } else {
            null
        }
    drawPath(barPath(size, corner, notch), colors.bar)

    if (layers.indicator && !layers.cutout) {
        val pillHeight = NavBarDimens.PillHeight.toPx()
        val inset = NavBarDimens.PillInset.toPx()
        val left = indicator.leftSlot * slot + inset
        val right = max(indicator.rightSlot * slot - inset, left + pillHeight)
        drawRoundRect(
            color = colors.accentSoft,
            topLeft = Offset(left, (size.height - pillHeight) / 2f),
            size = Size(right - left, pillHeight),
            cornerRadius = CornerRadius(pillHeight / 2f),
        )
    }
}

@Composable
private fun BoxScope.Bubble(
    item: NavItem,
    indicator: IndicatorState,
    itemCount: Int,
    stretch: Boolean,
    collapse: () -> Float,
    colors: AppColors,
) {
    Box(
        Modifier.align(Alignment.TopStart)
            .layout { measurable, constraints ->
                val m = collapse()
                val barWidthPx =
                    barWidth(
                        constraints.maxWidth,
                        (NavBarDimens.CollapsedSlot * itemCount).roundToPx(),
                        m,
                    )
                val slotWidth = barWidthPx / itemCount.toFloat()
                val stretchFactor =
                    if (stretch) {
                        (indicator.rightSlot - indicator.leftSlot).coerceIn(0.8f, 1.6f)
                    } else {
                        1f
                    }
                val height = lerp(NavBarDimens.BubbleSize.toPx(), NavBarDimens.PillHeight.toPx(), m)
                val bubbleWidth = NavBarDimens.BubbleSize.toPx() * stretchFactor
                val inset = NavBarDimens.PillInset.toPx()
                val pillWidth =
                    max((indicator.rightSlot - indicator.leftSlot) * slotWidth - 2 * inset, height)
                val width = lerp(bubbleWidth, pillWidth, m)
                val placeable =
                    measurable.measure(Constraints.fixed(width.roundToInt(), height.roundToInt()))
                layout(constraints.maxWidth, constraints.maxHeight) {
                    val barLeft = (constraints.maxWidth - barWidthPx) / 2f
                    val center =
                        clampNotchCenter(indicator.centerSlot * slotWidth, barWidthPx.toFloat())
                    val centerY =
                        lerp(
                            NavBarDimens.BubbleSize.toPx() / 2f,
                            NavBarDimens.BubbleOverhang.toPx() + NavBarDimens.BarHeight.toPx() / 2f,
                            m,
                        )
                    placeable.place(
                        (barLeft + center - width / 2f).roundToInt(),
                        (centerY - height / 2f).roundToInt(),
                    )
                }
            }
            .drawBehind {
                drawRoundRect(color = colors.accent, cornerRadius = CornerRadius(size.height / 2f))
            },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(item.selectedIcon),
            contentDescription = null,
            tint = colors.onAccent,
            modifier = Modifier.size(NavBarDimens.IconSize),
        )
    }
}

@Preview
@Composable
private fun AnimatedNavBarPreview() {
    LabTheme {
        val items =
            listOf(
                NavItem(
                    stringResource(Res.string.nav_home),
                    Res.drawable.ic_home,
                    Res.drawable.ic_home_filled,
                ),
                NavItem(
                    stringResource(Res.string.nav_search),
                    Res.drawable.ic_search,
                    Res.drawable.ic_search,
                ),
                NavItem(
                    stringResource(Res.string.nav_saved),
                    Res.drawable.ic_saved,
                    Res.drawable.ic_saved_filled,
                ),
                NavItem(
                    stringResource(Res.string.nav_profile),
                    Res.drawable.ic_profile,
                    Res.drawable.ic_profile_filled,
                ),
            )
        AnimatedNavBar(items = items, selectedIndex = 0, onSelect = {})
    }
}
