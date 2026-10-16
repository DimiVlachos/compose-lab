package dev.dimvlachos.lab.core.presentation.components.cardfile

import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf

internal val LocalCardFileCompositionProbe = staticCompositionLocalOf<(() -> Unit)?> { null }

// Non-restartable, so it can't be skipped: it runs exactly when its caller recomposes.
@Composable
@NonRestartableComposable
internal fun ReportCardFileComposition() {
    val probe = LocalCardFileCompositionProbe.current
    SideEffect { probe?.invoke() }
}
