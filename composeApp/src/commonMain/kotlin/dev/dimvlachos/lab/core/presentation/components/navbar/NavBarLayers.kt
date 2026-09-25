package dev.dimvlachos.lab.core.presentation.components.navbar

import androidx.compose.runtime.Immutable

@Immutable
data class NavBarLayers(
    val indicator: Boolean = false,
    val icons: Boolean = false,
    val scrollAware: Boolean = false,
    val cutout: Boolean = false,
) {
    companion object {
        val All = NavBarLayers(indicator = true, icons = true, scrollAware = true, cutout = true)
    }
}
