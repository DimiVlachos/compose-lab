package dev.dimvlachos.lab.core.presentation.components.navbar

import androidx.compose.runtime.Immutable
import org.jetbrains.compose.resources.DrawableResource

/**
 * The action button a tab brings out of the bar while it is selected.
 *
 * @property icon Drawn on the button.
 * @property label Read out by screen readers.
 */
@Immutable public data class NavAction(val icon: DrawableResource, val label: String)
