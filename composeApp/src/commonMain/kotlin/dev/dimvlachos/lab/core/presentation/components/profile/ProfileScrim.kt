package dev.dimvlachos.lab.core.presentation.components.profile

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import dev.dimvlachos.lab.core.presentation.components.imagemorph.MorphProgress
import dev.dimvlachos.lab.core.presentation.components.imagemorph.morphBackdropAlpha
import dev.dimvlachos.lab.core.presentation.ui.LabTheme

internal const val ScrimTag = "profileScrim"
private const val ScrimMaxAlpha = 0.8f

// A flat colour over the screen, on the morph's own progress: in with it, gone by half way on a
// close. Tapping it closes, and it swallows the tap so nothing underneath opens.
@Composable
internal fun ProfileScrim(
    animatedVisibilityScope: AnimatedVisibilityScope,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    maxAlpha: Float = ScrimMaxAlpha,
    tag: String = ScrimTag,
) {
    MorphProgress(animatedVisibilityScope) { progress, closing ->
        Box(
            modifier
                .fillMaxSize()
                .testTag(tag)
                .graphicsLayer {
                    alpha = maxAlpha * morphBackdropAlpha(progress.value, closing())
                    compositingStrategy = CompositingStrategy.ModulateAlpha
                }
                .background(LabTheme.colors.background)
                .clickable(interactionSource = null, indication = null, onClick = onClose)
        )
    }
}
