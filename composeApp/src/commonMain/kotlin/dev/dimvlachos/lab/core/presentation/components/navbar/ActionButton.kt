package dev.dimvlachos.lab.core.presentation.components.navbar

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import dev.dimvlachos.lab.core.presentation.ui.AppColors
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource

private val JellySettleSpring =
    spring<Float>(dampingRatio = 0.45f, stiffness = Spring.StiffnessMediumLow)

internal suspend fun jellyIn(
    scaleX: Animatable<Float, AnimationVector1D>,
    scaleY: Animatable<Float, AnimationVector1D>,
): Unit = coroutineScope {
    launch {
        scaleX.snapTo(0.2f)
        scaleX.animateTo(
            0.85f,
            keyframes {
                durationMillis = 160
                0.2f at 0
                1.25f at 90
                0.85f at 160
            },
        )
        scaleX.animateTo(1f, JellySettleSpring)
    }
    launch {
        scaleY.snapTo(0.2f)
        scaleY.animateTo(
            1.1f,
            keyframes {
                durationMillis = 160
                0.2f at 0
                0.8f at 90
                1.1f at 160
            },
        )
        scaleY.animateTo(1f, JellySettleSpring)
    }
}

internal suspend fun squash(
    scaleX: Animatable<Float, AnimationVector1D>,
    scaleY: Animatable<Float, AnimationVector1D>,
): Unit = coroutineScope {
    launch {
        scaleX.animateTo(0.85f, tween(90))
        scaleX.animateTo(1f, JellySettleSpring)
    }
    launch {
        scaleY.animateTo(1.1f, tween(90))
        scaleY.animateTo(1f, JellySettleSpring)
    }
}

@Composable
internal fun ActionButton(
    action: NavAction,
    selectedIndex: Int,
    isActive: Boolean,
    state: ActionRevealState,
    onActionClick: (Int) -> Unit,
    colors: AppColors,
    modifier: Modifier = Modifier,
) {
    ReportComposition()
    val scope = rememberCoroutineScope()
    Box(
        modifier
            .graphicsLayer {
                alpha = state.revealValue
                scaleX = state.scaleXValue
                scaleY = state.scaleYValue
                transformOrigin = TransformOrigin(0f, 0.5f)
            }
            .clip(CircleShape)
            .background(colors.accent)
            .then(
                if (isActive) {
                    Modifier.semantics { contentDescription = action.label }
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            role = Role.Button,
                        ) {
                            scope.launch {
                                state.squashTap()
                                onActionClick(selectedIndex)
                            }
                        }
                } else {
                    Modifier
                }
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(action.icon),
            contentDescription = null,
            tint = colors.onAccent,
            modifier = Modifier.size(NavBarDimens.IconSize),
        )
    }
}
