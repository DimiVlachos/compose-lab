package dev.dimvlachos.lab.core.presentation.components.navbar

import androidx.compose.runtime.Immutable
import org.jetbrains.compose.resources.DrawableResource

/**
 * One tab of an [AnimatedNavBar].
 *
 * @property label Shown under the icon.
 * @property icon The outlined icon, shown while the tab is not selected.
 * @property selectedIcon The filled icon, revealed when the tab is selected.
 * @property action The button the tab brings out while selected, or null for none.
 */
@Immutable
public data class NavItem(
    val label: String,
    val icon: DrawableResource,
    val selectedIcon: DrawableResource,
    val action: NavAction? = null,
)
