package dev.dimvlachos.lab.core.presentation.components.navbar

import androidx.compose.runtime.Immutable

/**
 * The layers of an [AnimatedNavBar], each switched on or off on its own. With all of them off it is
 * a plain bar.
 *
 * @property indicator A morphing indicator, its leading and trailing edges on different springs.
 * @property icons A circular reveal of the filled icon, with a squash and bounce.
 * @property scrollAware The bar shrinks into a floating pill as its [NavBarScrollState] collapses.
 * @property cutout The selected tab rides a bubble in a notch cut out of the bar.
 * @property action A tab's [NavItem.action] comes out of the bar as a button.
 */
@Immutable
public data class NavBarLayers(
    val indicator: Boolean = false,
    val icons: Boolean = false,
    val scrollAware: Boolean = false,
    val cutout: Boolean = false,
    val action: Boolean = false,
) {
    public companion object {
        /** Every layer on. */
        public val All: NavBarLayers =
            NavBarLayers(
                indicator = true,
                icons = true,
                scrollAware = true,
                cutout = true,
                action = true,
            )
    }
}
