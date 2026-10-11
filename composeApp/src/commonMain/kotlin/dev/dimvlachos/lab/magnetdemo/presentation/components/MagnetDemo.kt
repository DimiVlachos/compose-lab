package dev.dimvlachos.lab.magnetdemo.presentation.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import dev.dimvlachos.lab.core.demo.DemoState
import dev.dimvlachos.lab.core.presentation.components.magnet.MagnetState
import dev.dimvlachos.lab.core.presentation.components.magnet.MagnetTable
import dev.dimvlachos.lab.core.presentation.components.touch.ScriptedTouch
import dev.dimvlachos.lab.core.presentation.components.touch.drawTouch
import dev.dimvlachos.lab.core.presentation.ui.LabTheme
import dev.dimvlachos.lab.core.presentation.ui.isWideStage
import dev.dimvlachos.lab.magnetdemo.rememberIslandMagnetState
import dev.dimvlachos.lab.resources.Res
import dev.dimvlachos.lab.resources.magnet_hint
import kotlin.math.min
import kotlin.time.Duration
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.stringResource

// The scripted finger holds a magnet this long where it puts it down before it lifts.
private const val HoldMs = 120L

/**
 * The twelve island photos on a magnet table, with a hint above it. The script's drags, flicks and
 * taps are played by a fingertip drawn on the magnet, through the table's own place, drag and
 * release.
 */
@Composable
internal fun MagnetDemo(state: DemoState, table: MagnetState = rememberIslandMagnetState()) {
    val finger = remember { MagnetFinger() }
    // The stage's size, read only by the script, so setting it recomposes nothing.
    val stage = remember { Stage() }
    DisposableEffect(state, table) {
        state.setMagnetDragHandler { tag, path, duration ->
            finger.drag(table, tag, path.map { stage.toPx(it) }, duration)
        }
        state.setMagnetReleaseHandler { tag, duration -> finger.flickHome(table, tag, duration) }
        state.setMagnetTapHandler { tag -> finger.tap(table, tag) }
        state.setPhotoTapHandler { id -> finger.tapPhoto(table, id) }
        onDispose {
            state.setMagnetDragHandler(null)
            state.setMagnetReleaseHandler(null)
            state.setMagnetTapHandler(null)
            state.setPhotoTapHandler(null)
        }
    }
    // Opened, the room's air comes in with the screen and the magnets on their nails are already
    // swaying, as the pull cord's lamp is. Not for the script, which starts from them hanging
    // still.
    LaunchedEffect(table) { if (!state.replay && !state.recording) table.stir() }
    val colors = LabTheme.colors
    // Whether every magnet is in the strip: read as that, so a photo sticking recomposes nothing.
    val idle by remember(table) { derivedStateOf { table.results.isEmpty() } }
    val hintAlpha by animateFloatAsState(if (idle) 1f else 0f)
    val hint = stringResource(Res.string.magnet_hint)
    val tableModifier =
        Modifier.onSizeChanged { stage.size = it }
            .drawWithContent {
                drawContent()
                with(finger) { drawFinger(colors.touch) }
            }
    // Clear of the navigation bar and the sides' cutouts; the screen it is shown in keeps it clear
    // of the status bar already.
    BoxWithConstraints(
        Modifier.fillMaxSize()
            .background(colors.stripRail)
            .windowInsetsPadding(
                WindowInsets.safeDrawing.only(
                    WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom
                )
            )
    ) {
        if (isWideStage(maxWidth, maxHeight)) {
            // On a phone on its side every bit of height goes to the table: the hint lies over its
            // top on a chip of the rail, as a magnet's label does over the cards.
            Box(Modifier.fillMaxSize()) {
                MagnetTable(table, Modifier.fillMaxSize().then(tableModifier))
                Text(
                    hint,
                    style = LabTheme.typography.label,
                    color = colors.textMuted,
                    modifier =
                        Modifier.align(Alignment.TopCenter)
                            .padding(top = LabTheme.spacing.small)
                            .graphicsLayer { alpha = hintAlpha }
                            .background(colors.stripRail.copy(alpha = 0.8f), CircleShape)
                            .padding(
                                horizontal = LabTheme.spacing.smallMedium,
                                vertical = LabTheme.spacing.extraSmall,
                            )
                            .then(if (idle) Modifier else Modifier.clearAndSetSemantics {}),
                )
            }
            return@BoxWithConstraints
        }
        Column(Modifier.fillMaxSize()) {
            // The hint has a line of its own over the table, so no card ever lies under it. It
            // gives way while a magnet is out, as by then it has been taken, but keeps its line,
            // so the table doesn't jump.
            Text(
                hint,
                style = LabTheme.typography.label,
                color = colors.textMuted,
                modifier =
                    Modifier.align(Alignment.CenterHorizontally)
                        .padding(vertical = LabTheme.spacing.smallMedium)
                        .graphicsLayer { alpha = hintAlpha }
                        .then(if (idle) Modifier else Modifier.clearAndSetSemantics {}),
            )
            MagnetTable(table, Modifier.fillMaxWidth().weight(1f).then(tableModifier))
        }
    }
}

private class Stage {
    var size = IntSize.Zero

    fun toPx(fraction: Offset): Offset {
        val width = size.width.toFloat()
        val height = size.height.toFloat()
        // Only the stage's shape matters here, so its px stand in for dp.
        if (!isWideStage(width.dp, height.dp)) {
            return Offset(fraction.x * size.width, fraction.y * size.height)
        }
        // On a stage wider than it is tall the script plays down a band in its middle about as
        // wide as a clip's stage, so the magnets travel as far as they do in the clip, and the two
        // put down apart still lie apart and the two brought together still snap.
        val band = min(width, height * ScriptBand)
        return Offset((width - band) / 2f + fraction.x * band, fraction.y * height)
    }
}

// How wide the script's band on a wide stage is, for its height: about a 4:5 clip's stage.
private const val ScriptBand = 0.85f

/**
 * The scripted fingertip on a magnet, so a clip shows the hand: it takes the magnet, carries it,
 * puts it down and lifts. Nothing but the script moves it.
 */
@Stable
private class MagnetFinger {
    private val alpha = Animatable(0f)
    private var at by mutableStateOf(Offset.Zero)

    // Which gesture has the fingertip. A gesture that starts as the last one fades out takes it
    // over, and the last one, cancelled mid-fade, must not then hide the new one's fingertip.
    private var gesture = 0

    /**
     * Takes [tag]'s magnet where it is, carries it through [path], in px, over [duration], and puts
     * it down at the end. A magnet it can't take, a third out of the strip, say, it leaves alone. A
     * script stopped mid-drag still lets go.
     */
    suspend fun drag(table: MagnetState, tag: String, path: List<Offset>, duration: Duration) {
        val mine = ++gesture
        var holding = false
        try {
            val from = table.magnetPosition(tag) ?: return
            at = from
            alpha.animateTo(1f, tween(ScriptedTouch.DownMs))
            holding = table.place(tag, from)
            if (!holding) return
            val points = listOf(from) + path
            animate(
                0f,
                1f,
                animationSpec =
                    tween(duration.inWholeMilliseconds.toInt(), easing = FastOutSlowInEasing),
            ) { t, _ ->
                at = along(points, t)
                table.drag(tag, at)
            }
            delay(HoldMs)
            table.release(tag, Offset.Zero)
            holding = false
            alpha.animateTo(0f, tween(ScriptedTouch.UpMs))
        } finally {
            if (holding) table.release(tag, Offset.Zero)
            if (mine == gesture) withContext(NonCancellable) { alpha.snapTo(0f) }
        }
    }

    /** Takes [tag]'s magnet and flicks it down into its slot over [duration]. */
    suspend fun flickHome(table: MagnetState, tag: String, duration: Duration) {
        val mine = ++gesture
        var holding = false
        try {
            val from = table.magnetPosition(tag) ?: return
            val to = table.slotPosition(tag) ?: return
            at = from
            alpha.animateTo(1f, tween(ScriptedTouch.DownMs))
            holding = table.place(tag, from)
            if (!holding) return
            // A flick is a yank: one of a joined pair tears straight off it, leaving the other.
            if (table.joined) table.tearOff(tag)
            val ms = duration.inWholeMilliseconds.toInt().coerceAtLeast(1)
            animate(0f, 1f, animationSpec = tween(ms, easing = FastOutLinearInEasing)) { t, _ ->
                at = lerp(from, to, t)
                table.drag(tag, at)
            }
            // Let go still moving, as a flick is.
            table.release(tag, (to - from) * (1_000f / ms))
            holding = false
            alpha.animateTo(0f, tween(ScriptedTouch.UpMs))
        } finally {
            if (holding) table.release(tag, Offset.Zero)
            if (mine == gesture) withContext(NonCancellable) { alpha.snapTo(0f) }
        }
    }

    /** Taps [tag]'s magnet: its photos fan out, or fold back if they are out. */
    suspend fun tap(table: MagnetState, tag: String) {
        val mine = ++gesture
        try {
            at = table.magnetPosition(tag) ?: return
            alpha.animateTo(1f, tween(ScriptedTouch.DownMs))
            table.fanOut(if (table.fannedOut == tag) null else tag)
            delay(ScriptedTouch.TapHoldMs)
            alpha.animateTo(0f, tween(ScriptedTouch.UpMs))
        } finally {
            if (mine == gesture) withContext(NonCancellable) { alpha.snapTo(0f) }
        }
    }

    /**
     * Taps the photo [id] where the grid shows it, which opens it, or, open, taps it in the middle
     * of the table, where it lies large, which closes it.
     */
    suspend fun tapPhoto(table: MagnetState, id: String) {
        val mine = ++gesture
        try {
            val index = table.photos.indexOfFirst { it.id == id }
            if (index < 0) return
            val open = table.opened == id
            at =
                if (open) Offset(table.width / 2f, table.tableHeight / 2f) * table.density
                else table.photoCentre(index) * table.density
            alpha.animateTo(1f, tween(ScriptedTouch.DownMs))
            if (open) table.close() else table.open(id)
            delay(ScriptedTouch.TapHoldMs)
            alpha.animateTo(0f, tween(ScriptedTouch.UpMs))
        } finally {
            if (mine == gesture) withContext(NonCancellable) { alpha.snapTo(0f) }
        }
    }

    fun DrawScope.drawFinger(color: Color) {
        val alpha = alpha.value
        if (alpha <= 0f) return
        drawTouch(color, at, alpha)
    }
}

// The point [t] of the way along [points], taken at even time steps.
private fun along(points: List<Offset>, t: Float): Offset {
    if (points.size == 1) return points[0]
    val f = t * (points.size - 1)
    val i = f.toInt().coerceAtMost(points.size - 2)
    return lerp(points[i], points[i + 1], f - i)
}

@Preview
@Composable
private fun MagnetDemoDarkPreview() {
    LabTheme(darkTheme = true) { MagnetDemo(remember { DemoState() }) }
}

@Preview
@Composable
private fun MagnetDemoLightPreview() {
    LabTheme(darkTheme = false) { MagnetDemo(remember { DemoState() }) }
}
