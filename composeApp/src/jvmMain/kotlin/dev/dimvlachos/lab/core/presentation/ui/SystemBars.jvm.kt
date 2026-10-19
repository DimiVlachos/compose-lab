package dev.dimvlachos.lab.core.presentation.ui

import androidx.compose.runtime.Composable

// No system bars where the tests run.
@Composable
internal actual fun SystemAppearance(mode: ThemeMode, dark: Boolean, requests: SystemBarRequests) =
    Unit
