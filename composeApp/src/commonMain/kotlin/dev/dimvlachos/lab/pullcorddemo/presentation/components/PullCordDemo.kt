package dev.dimvlachos.lab.pullcorddemo.presentation.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import dev.dimvlachos.lab.core.demo.DemoState
import dev.dimvlachos.lab.core.presentation.components.pullcord.PullCordLamp
import dev.dimvlachos.lab.core.presentation.components.pullcord.PullCordState
import dev.dimvlachos.lab.core.presentation.components.pullcord.rememberPullCordState
import dev.dimvlachos.lab.core.presentation.components.touch.ScriptedTouch
import dev.dimvlachos.lab.core.presentation.components.touch.drawTouch
import dev.dimvlachos.lab.core.presentation.ui.LabTheme
import dev.dimvlachos.lab.core.presentation.ui.ScreenPalette
import dev.dimvlachos.lab.resources.Res
import dev.dimvlachos.lab.resources.profile_name
import dev.dimvlachos.lab.resources.pullcord_dark_theme
import dev.dimvlachos.lab.resources.pullcord_handle
import dev.dimvlachos.lab.resources.pullcord_hint
import dev.dimvlachos.lab.resources.pullcord_notifications
import dev.dimvlachos.lab.resources.pullcord_previews
import dev.dimvlachos.lab.resources.pullcord_settings
import dev.dimvlachos.lab.resources.pullcord_sounds
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

// The lamp hangs right of the middle, clear of the title, so its cord falls between the rows'
// names and their switches: on the other side of the middle when they are laid out right to left.
private const val LampAcross = 0.62f

// The scripted finger holds the bead this long at the end of a pull before it lets go.
private const val HoldMs = 120L

// Room above the cards for the lamp's shade to hang in, and enough that the bead's 48 dp target,
// at the end of its 214 dp from the ceiling, hangs clear of the rows below it.
private val ShadeRoom = 64.dp

private val KnobInset = 3.dp
private val DividerHeight = 1.dp
private val RowHeight = 52.dp
private val AvatarSize = 52.dp
private val SwitchWidth = 44.dp
private val SwitchHeight = 26.dp
private val KnobSize = 20.dp

/**
 * A settings screen under a pull-cord lamp: by day a light screen, the lamp off; pull the cord and
 * the lamp comes on, and the screen turns dark in its light. The dark theme switch on the screen
 * works the lamp too. The script's pulls are played by a fingertip drawn on the bead, through the
 * lamp's own grab, drag and release.
 */
@Composable
internal fun PullCordDemo(state: DemoState, lamp: PullCordState = rememberPullCordState()) {
    val finger = remember { CordFinger() }
    val density = LocalDensity.current
    DisposableEffect(state, lamp, density) {
        state.setCordHandler { down, duration, across ->
            val by = with(density) { Offset(across.toPx(), down.toPx()) }
            finger.pull(lamp, by, duration.inWholeMilliseconds.toInt())
        }
        onDispose { state.setCordHandler(null) }
    }
    // Opened, the room's air comes in with the screen, and the cord is already swaying, as a lamp
    // does when a door opens. Not for the script, whose pulls need the bead where it hangs.
    LaunchedEffect(lamp) { if (!state.replay && !state.recording) lamp.stir() }
    // The screen's settings, and where it is scrolled to, live out here, not in the screen: it is
    // drawn in both looks at once while the light spreads, and both must agree.
    var notifications by rememberSaveable { mutableStateOf(true) }
    var sounds by rememberSaveable { mutableStateOf(false) }
    var previews by rememberSaveable { mutableStateOf(true) }
    val scroll = rememberScrollState()
    val touch = LabTheme.colors.touch
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    PullCordLamp(
        lamp,
        // Clear of the navigation bar, so its buttons stay light on the app's dark background
        // rather than lost on the day look's paper.
        Modifier.fillMaxSize().navigationBarsPadding().drawWithContent {
            drawContent()
            with(finger) { drawFinger(lamp, touch, ScreenPalette.Day.textPrimary) }
        },
        across = if (rtl) 1f - LampAcross else LampAcross,
    ) { lit ->
        val palette = if (lit) ScreenPalette.Night else ScreenPalette.Day
        Settings(
            palette,
            scroll,
            darkTheme = lit,
            onDarkTheme = { lamp.toggle() },
            notifications = notifications,
            onNotifications = { notifications = it },
            sounds = sounds,
            onSounds = { sounds = it },
            previews = previews,
            onPreviews = { previews = it },
        )
    }
}

@Composable
private fun Settings(
    palette: ScreenPalette,
    scroll: ScrollState,
    darkTheme: Boolean,
    onDarkTheme: (Boolean) -> Unit,
    notifications: Boolean,
    onNotifications: (Boolean) -> Unit,
    sounds: Boolean,
    onSounds: (Boolean) -> Unit,
    previews: Boolean,
    onPreviews: (Boolean) -> Unit,
) {
    val spacing = LabTheme.spacing
    val type = LabTheme.typography
    // It scrolls when it is taller than the screen, as in landscape or at a large font size; with
    // room to spare, the hint sits at the bottom.
    BoxWithConstraints(Modifier.fillMaxSize().background(palette.background)) {
        Column(
            Modifier.fillMaxWidth()
                .verticalScroll(scroll)
                .heightIn(min = maxHeight)
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
                .padding(horizontal = spacing.mediumLarge, vertical = spacing.mediumLarge),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(
                Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(spacing.medium),
            ) {
                Text(
                    stringResource(Res.string.pullcord_settings),
                    style = type.title,
                    color = palette.textPrimary,
                )
                Spacer(Modifier.height(ShadeRoom))
                Profile(palette)
                Column(
                    Modifier.fillMaxWidth()
                        .clip(LabTheme.shapes.extraLarge)
                        .background(palette.surface)
                        .padding(horizontal = spacing.medium)
                ) {
                    SettingRow(Res.string.pullcord_dark_theme, darkTheme, onDarkTheme, palette)
                    Divider(palette)
                    SettingRow(
                        Res.string.pullcord_notifications,
                        notifications,
                        onNotifications,
                        palette,
                    )
                    Divider(palette)
                    SettingRow(Res.string.pullcord_sounds, sounds, onSounds, palette)
                    Divider(palette)
                    SettingRow(Res.string.pullcord_previews, previews, onPreviews, palette)
                }
            }
            Text(
                stringResource(Res.string.pullcord_hint),
                style = type.label,
                color = palette.textMuted,
                modifier =
                    Modifier.align(Alignment.CenterHorizontally).padding(top = spacing.large),
            )
        }
    }
}

@Composable
private fun Profile(palette: ScreenPalette) {
    val spacing = LabTheme.spacing
    val name = stringResource(Res.string.profile_name)
    // One card to a screen reader: the name and the handle, without the initials, which only
    // say the name again.
    Row(
        Modifier.fillMaxWidth()
            .semantics(mergeDescendants = true) {}
            .clip(LabTheme.shapes.extraLarge)
            .background(palette.surface)
            .padding(spacing.medium),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(AvatarSize)
                .clip(CircleShape)
                .background(palette.accent)
                .clearAndSetSemantics {},
            contentAlignment = Alignment.Center,
        ) {
            Text(
                name.split(' ').mapNotNull { it.firstOrNull() }.joinToString(""),
                style = LabTheme.typography.subtitle,
                color = palette.onAccent,
            )
        }
        Spacer(Modifier.width(spacing.medium))
        Column {
            Text(name, style = LabTheme.typography.subtitle, color = palette.textPrimary)
            Text(
                stringResource(Res.string.pullcord_handle),
                style = LabTheme.typography.label,
                color = palette.textMuted,
            )
        }
    }
}

@Composable
private fun SettingRow(
    label: StringResource,
    on: Boolean,
    onChange: (Boolean) -> Unit,
    palette: ScreenPalette,
) {
    Row(
        Modifier.fillMaxWidth()
            .heightIn(min = RowHeight)
            .toggleable(value = on, role = Role.Switch, onValueChange = onChange),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            stringResource(label),
            style = LabTheme.typography.body,
            color = palette.textPrimary,
            modifier = Modifier.weight(1f),
        )
        PaletteSwitch(on, palette)
    }
}

// A plain pill switch in the screen's own colours: the knob right and the accent on when on.
@Composable
private fun PaletteSwitch(on: Boolean, palette: ScreenPalette) {
    Box(
        Modifier.size(SwitchWidth, SwitchHeight)
            .clip(CircleShape)
            .background(if (on) palette.accent else palette.track)
            .padding(horizontal = KnobInset),
        contentAlignment = if (on) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        Box(
            Modifier.size(KnobSize)
                .clip(CircleShape)
                .background(if (on) palette.onAccent else palette.surface)
        )
    }
}

@Composable
private fun Divider(palette: ScreenPalette) {
    Box(Modifier.fillMaxWidth().height(DividerHeight).background(palette.divider))
}

/**
 * The scripted fingertip on the lamp's bead, so a clip shows the hand: it holds the bead as it is
 * pulled, and lifts where it let go. Nothing but the script moves it.
 */
@Stable
private class CordFinger {
    private val alpha = Animatable(0f)

    // Where the finger let go, or null while it holds the bead and is drawn on it.
    private var letGo by mutableStateOf<Offset?>(null)

    /**
     * Takes [lamp]'s bead, then shows the fingertip on it, pulls it [by] px over [durationMs] as a
     * hand does, quick to start and easing in, holds it a moment and lets go. A bead it can't take,
     * held by a real finger, say, it leaves alone, and shows no fingertip. A script stopped
     * mid-pull still lets go.
     */
    suspend fun pull(lamp: PullCordState, by: Offset, durationMs: Int) {
        letGo = null
        var holding = false
        try {
            // Where the bead is as it is taken, not before the fingertip faded in: it may have
            // swung on meanwhile.
            val from = lamp.bead
            holding = lamp.grab(from)
            if (!holding) return
            alpha.animateTo(1f, tween(ScriptedTouch.DownMs))
            animate(0f, 1f, animationSpec = tween(durationMs, easing = FastOutSlowInEasing)) {
                fraction,
                _ ->
                lamp.dragTo(from + by * fraction)
            }
            delay(HoldMs)
            letGo = lamp.bead
            lamp.release()
            holding = false
            alpha.animateTo(0f, tween(ScriptedTouch.UpMs))
        } finally {
            if (holding) lamp.release()
            withContext(NonCancellable) { alpha.snapTo(0f) }
        }
    }

    // White on the lamplit screen, ink on the daylit one, so it shows on both.
    fun DrawScope.drawFinger(lamp: PullCordState, lit: Color, day: Color) {
        val alpha = alpha.value
        if (alpha <= 0f) return
        drawTouch(if (lamp.lit) lit else day, letGo ?: lamp.bead, alpha)
    }
}
