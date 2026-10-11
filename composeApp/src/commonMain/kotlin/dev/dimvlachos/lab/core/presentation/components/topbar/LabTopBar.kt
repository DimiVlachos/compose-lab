package dev.dimvlachos.lab.core.presentation.components.topbar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import dev.dimvlachos.lab.core.presentation.ui.LabTheme
import dev.dimvlachos.lab.resources.Res
import dev.dimvlachos.lab.resources.ic_arrow_back
import dev.dimvlachos.lab.resources.ic_play_arrow
import dev.dimvlachos.lab.resources.ic_search
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

private val BarHeight = 56.dp
private val BarElevation = 8.dp

/**
 * The bar across the top of every screen: a way back ([navigationIcon]), the [title], buttons at
 * the end ([actions]), and anything that belongs to the bar under them ([bottom]), like a row of
 * filters. It reaches up under the status bar, so its colour runs to the top of the screen, and
 * casts its shadow over the content below it.
 */
@Composable
internal fun LabTopBar(
    modifier: Modifier = Modifier,
    navigationIcon: (@Composable () -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    bottom: (@Composable () -> Unit)? = null,
    title: @Composable BoxScope.() -> Unit,
) {
    val colors = LabTheme.colors
    Column(
        modifier
            .zIndex(1f)
            .fillMaxWidth()
            .shadow(
                BarElevation,
                RectangleShape,
                clip = false,
                ambientColor = colors.topBarShadow,
                spotColor = colors.topBarShadow,
            )
            .background(colors.topBar)
            .windowInsetsPadding(
                WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
            )
    ) {
        Row(
            Modifier.fillMaxWidth()
                .heightIn(min = BarHeight)
                .padding(horizontal = LabTheme.spacing.small),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            navigationIcon?.invoke()
            Box(
                Modifier.weight(1f).padding(horizontal = LabTheme.spacing.small),
                contentAlignment = Alignment.CenterStart,
            ) {
                title()
            }
            actions()
        }
        bottom?.invoke()
    }
}

/** A screen's title in the [LabTopBar]: one line, cut short with an ellipsis if it must be. */
@Composable
internal fun LabTopBarTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        color = LabTheme.colors.textPrimary,
        style = LabTheme.typography.subtitle,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier,
    )
}

/**
 * An icon button for the [LabTopBar], [tint]ed in the theme's text colour unless told otherwise.
 */
@Composable
internal fun LabIconButton(
    icon: DrawableResource,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = LabTheme.colors.textPrimary,
) {
    IconButton(
        onClick = onClick,
        modifier = modifier,
        colors =
            IconButtonDefaults.iconButtonColors(
                containerColor = Color.Transparent,
                contentColor = tint,
            ),
    ) {
        Icon(painterResource(icon), contentDescription = contentDescription)
    }
}

@Composable
private fun TopBarSample() {
    LabTopBar(
        navigationIcon = {
            LabIconButton(Res.drawable.ic_arrow_back, contentDescription = "Back", onClick = {})
        },
        actions = {
            LabIconButton(Res.drawable.ic_search, contentDescription = "Search", onClick = {})
            LabIconButton(
                Res.drawable.ic_play_arrow,
                contentDescription = "Replay",
                onClick = {},
                tint = LabTheme.colors.accent,
            )
        },
    ) {
        LabTopBarTitle("Fogged mirror")
    }
}

@Preview
@Composable
private fun LabTopBarLightPreview() {
    LabTheme(darkTheme = false) {
        Box(Modifier.background(LabTheme.colors.background).padding(bottom = 16.dp)) {
            TopBarSample()
        }
    }
}

@Preview
@Composable
private fun LabTopBarDarkPreview() {
    LabTheme(darkTheme = true) {
        Box(Modifier.background(LabTheme.colors.background).padding(bottom = 16.dp)) {
            TopBarSample()
        }
    }
}
