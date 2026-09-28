package dev.dimvlachos.lab.core.presentation.components.imagemorph

import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf

internal val LocalMorphCompositionProbe = staticCompositionLocalOf<((MorphEnd) -> Unit)?> { null }

// Non-restartable, so it can't be skipped: it runs exactly when its caller recomposes.
@Composable
@NonRestartableComposable
internal fun ReportMorphComposition(end: MorphEnd) {
    val probe = LocalMorphCompositionProbe.current
    SideEffect { probe?.invoke(end) }
}
