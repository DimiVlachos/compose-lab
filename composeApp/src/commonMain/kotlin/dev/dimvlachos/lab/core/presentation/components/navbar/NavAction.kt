package dev.dimvlachos.lab.core.presentation.components.navbar

import androidx.compose.runtime.Immutable
import org.jetbrains.compose.resources.DrawableResource

@Immutable data class NavAction(val icon: DrawableResource, val label: String)
