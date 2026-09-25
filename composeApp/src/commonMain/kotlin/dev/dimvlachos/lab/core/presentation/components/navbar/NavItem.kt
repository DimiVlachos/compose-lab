package dev.dimvlachos.lab.core.presentation.components.navbar

import androidx.compose.runtime.Immutable
import org.jetbrains.compose.resources.DrawableResource

@Immutable
data class NavItem(
    val label: String,
    val icon: DrawableResource,
    val selectedIcon: DrawableResource,
)
