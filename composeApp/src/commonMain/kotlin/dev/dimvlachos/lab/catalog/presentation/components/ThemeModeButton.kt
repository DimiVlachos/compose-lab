package dev.dimvlachos.lab.catalog.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.dimvlachos.lab.core.presentation.components.topbar.LabIconButton
import dev.dimvlachos.lab.core.presentation.ui.ThemeMode
import dev.dimvlachos.lab.resources.Res
import dev.dimvlachos.lab.resources.ic_brightness_auto
import dev.dimvlachos.lab.resources.ic_dark_mode
import dev.dimvlachos.lab.resources.ic_light_mode
import dev.dimvlachos.lab.resources.theme_dark
import dev.dimvlachos.lab.resources.theme_light
import dev.dimvlachos.lab.resources.theme_system
import org.jetbrains.compose.resources.stringResource

/**
 * A button in the bar that shows the theme [mode] the app is in and, tapped, moves on to the next
 * one ([onCycle]): following the system, then light, then dark.
 */
@Composable
internal fun ThemeModeButton(mode: ThemeMode, onCycle: () -> Unit, modifier: Modifier = Modifier) {
    val (icon, description) =
        when (mode) {
            ThemeMode.System -> Res.drawable.ic_brightness_auto to Res.string.theme_system
            ThemeMode.Light -> Res.drawable.ic_light_mode to Res.string.theme_light
            ThemeMode.Dark -> Res.drawable.ic_dark_mode to Res.string.theme_dark
        }
    LabIconButton(
        icon,
        contentDescription = stringResource(description),
        onClick = onCycle,
        modifier = modifier,
    )
}
