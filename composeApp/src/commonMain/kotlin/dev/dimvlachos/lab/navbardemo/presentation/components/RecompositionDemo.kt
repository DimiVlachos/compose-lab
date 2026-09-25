package dev.dimvlachos.lab.navbardemo.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import dev.dimvlachos.lab.core.demo.DemoState
import dev.dimvlachos.lab.core.presentation.components.navbar.LocalNavBarCompositionProbe
import dev.dimvlachos.lab.core.presentation.components.navbar.NavBarLayers
import dev.dimvlachos.lab.core.presentation.ui.LabTheme
import dev.dimvlachos.lab.resources.Res
import dev.dimvlachos.lab.resources.recompositions_count
import dev.dimvlachos.lab.resources.recompositions_frames
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun RecompositionDemo(state: DemoState) {
    var frames by remember { mutableIntStateOf(0) }
    var recompositions by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) withFrameNanos { frames++ }
    }
    val probe = remember { { recompositions += 1 } }
    Box(Modifier.fillMaxSize()) {
        CompositionLocalProvider(LocalNavBarCompositionProbe provides probe) {
            NavBarDemo(state, NavBarLayers(indicator = true, icons = true, cutout = true))
        }
        Column(
            Modifier.align(Alignment.TopCenter)
                .padding(top = LabTheme.spacing.huge + LabTheme.spacing.extraLarge),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(LabTheme.spacing.small),
        ) {
            Text(
                stringResource(Res.string.recompositions_frames, frames),
                color = LabTheme.colors.textPrimary,
                style = LabTheme.typography.stat,
            )
            Text(
                stringResource(Res.string.recompositions_count, recompositions),
                color = LabTheme.colors.accent,
                style = LabTheme.typography.stat,
            )
        }
    }
}
