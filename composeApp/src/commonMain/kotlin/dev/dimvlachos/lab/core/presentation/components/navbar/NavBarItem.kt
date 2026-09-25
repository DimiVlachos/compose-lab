package dev.dimvlachos.lab.core.presentation.components.navbar

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import dev.dimvlachos.lab.core.presentation.ui.LabTheme
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource

@Composable
internal fun NavBarItem(
    item: NavItem,
    selected: Boolean,
    animateIcon: Boolean,
    hideIcon: Boolean,
    collapse: () -> Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ReportComposition()
    val reveal = remember { Animatable(if (selected) 1f else 0f) }
    val bounce = remember { Animatable(1f) }
    val clip = remember { Path() }
    val colors = LabTheme.colors
    val spacing = LabTheme.spacing
    val typography = LabTheme.typography
    val tint = if (selected) colors.accent else colors.textMuted

    LaunchedEffect(selected, animateIcon) {
        val target = if (selected) 1f else 0f
        if (!animateIcon) {
            reveal.snapTo(target)
            bounce.snapTo(1f)
            return@LaunchedEffect
        }
        if (reveal.value == target) return@LaunchedEffect
        if (selected) {
            launch {
                reveal.animateTo(1f, tween(durationMillis = 420, easing = FastOutSlowInEasing))
            }
            bounce.snapTo(0.7f)
            bounce.animateTo(1f, spring(dampingRatio = 0.35f, stiffness = 450f))
        } else {
            reveal.animateTo(0f, tween(durationMillis = 180))
        }
    }

    Column(
        modifier
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.Tab,
                interactionSource = null,
                indication = null,
            )
            .graphicsLayer { translationY = collapse() * NavBarDimens.LabelShift.toPx() },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            Modifier.size(NavBarDimens.IconSize).graphicsLayer {
                alpha = if (hideIcon) 0f else 1f
                scaleX = bounce.value
                scaleY = 2f - bounce.value
            }
        ) {
            Icon(
                painter = painterResource(item.icon),
                contentDescription = null,
                tint = tint,
                modifier = Modifier.matchParentSize(),
            )
            Icon(
                painter = painterResource(item.selectedIcon),
                contentDescription = null,
                tint = colors.accent,
                modifier =
                    Modifier.matchParentSize().drawWithContent {
                        val progress = reveal.value
                        when {
                            progress <= 0f -> Unit
                            progress >= 1f -> drawContent()
                            else -> {
                                clip.reset()
                                clip.addOval(Rect(center, size.maxDimension * 0.75f * progress))
                                clipPath(clip) { this@drawWithContent.drawContent() }
                            }
                        }
                    },
            )
        }
        Spacer(Modifier.height(spacing.extraSmall))
        Text(
            item.label,
            color = tint,
            style = typography.label,
            maxLines = 1,
            modifier = Modifier.graphicsLayer { alpha = 1f - collapse() },
        )
    }
}
