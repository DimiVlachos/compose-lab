package dev.dimvlachos.lab.locketdemo.presentation.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import dev.dimvlachos.lab.core.demo.DemoState
import dev.dimvlachos.lab.core.demo.demoInsets
import dev.dimvlachos.lab.core.platform.rememberReducedMotion
import dev.dimvlachos.lab.core.platform.rememberSoundPlayer
import dev.dimvlachos.lab.core.presentation.components.locket.Locket
import dev.dimvlachos.lab.core.presentation.components.locket.LocketHand
import dev.dimvlachos.lab.core.presentation.components.locket.LocketLabels
import dev.dimvlachos.lab.core.presentation.components.locket.LocketSounds
import dev.dimvlachos.lab.core.presentation.components.locket.LocketState
import dev.dimvlachos.lab.core.presentation.components.locket.locketPhotos
import dev.dimvlachos.lab.core.presentation.ui.LabTheme
import dev.dimvlachos.lab.core.presentation.ui.LocketScreenColors
import dev.dimvlachos.lab.locketdemo.play
import dev.dimvlachos.lab.resources.Res
import dev.dimvlachos.lab.resources.locket_bio
import dev.dimvlachos.lab.resources.locket_bio_label
import dev.dimvlachos.lab.resources.locket_business
import dev.dimvlachos.lab.resources.locket_close
import dev.dimvlachos.lab.resources.locket_hint
import dev.dimvlachos.lab.resources.locket_name
import dev.dimvlachos.lab.resources.locket_name_label
import dev.dimvlachos.lab.resources.locket_open
import dev.dimvlachos.lab.resources.locket_photo_beach
import dev.dimvlachos.lab.resources.locket_photo_dog
import dev.dimvlachos.lab.resources.locket_photo_ridge
import dev.dimvlachos.lab.resources.locket_photo_sunflowers
import dev.dimvlachos.lab.resources.locket_recent
import dev.dimvlachos.lab.resources.locket_save
import dev.dimvlachos.lab.resources.locket_status
import dev.dimvlachos.lab.resources.locket_tip_closed
import dev.dimvlachos.lab.resources.locket_tip_open
import dev.dimvlachos.lab.resources.locket_tip_placed
import dev.dimvlachos.lab.resources.locket_title
import dev.dimvlachos.lab.resources.locket_use_photo
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.jetbrains.compose.resources.stringResource

/**
 * The prototype's edit-profile screen: the profile photo lives in a gold locket on a chain over a
 * strip of Recent photos, and the round chip under it shows what others see. The script's taps and
 * drag go through the same locket a finger works.
 */
@Composable
internal fun LocketDemo(
    state: DemoState,
    locket: LocketState = remember { LocketState() },
    reducedMotion: Boolean = rememberReducedMotion(),
) {
    val sounds = rememberSoundPlayer(LocketSounds)
    SideEffect { locket.reducedMotion = reducedMotion }
    val photos = remember { locketPhotos() }
    val hand = remember(locket) { LocketHand(locket) }
    // The script's steps run one after another, as the prototype awaits each: on a slow device a
    // step that runs late holds the next back rather than overlapping it.
    val turn = remember { Mutex() }

    DisposableEffect(state, locket, hand) {
        state.setControlTapHandler { control ->
            try {
                turn.withLock { hand.play(control, locket) }
            } catch (e: CancellationException) {
                hand.stop()
                throw e
            }
        }
        // Shut, still, with the Blue Ridge photo back in, so the clip loops.
        state.setFeedResetHandler {
            turn.withLock {
                hand.stop()
                locket.reset()
            }
        }
        onDispose {
            state.setControlTapHandler(null)
            state.setFeedResetHandler(null)
        }
    }
    // A script the user takes over leaves no fingertip behind.
    LaunchedEffect(state.replay) { if (!state.replay) hand.stop() }

    val names =
        listOf(
            stringResource(Res.string.locket_photo_beach),
            stringResource(Res.string.locket_photo_dog),
            stringResource(Res.string.locket_photo_ridge),
            stringResource(Res.string.locket_photo_sunflowers),
        )
    val useLabels = names.map { stringResource(Res.string.locket_use_photo, it) }
    val labels =
        LocketLabels(
            open = stringResource(Res.string.locket_open),
            close = stringResource(Res.string.locket_close),
            recent = stringResource(Res.string.locket_recent),
            hint = stringResource(Res.string.locket_hint),
            usePhoto = useLabels,
        )
    val colors = if (LabTheme.isDark) LocketScreenColors.Dark else LocketScreenColors.Light
    Column(
        Modifier.fillMaxSize().background(colors.background).windowInsetsPadding(demoInsets(state)),
    ) {
        AppBar(colors)
        Locket(
            state = locket,
            photos = photos,
            labels = labels,
            muted = colors.muted,
            accent = colors.accent,
            sounds = sounds,
            modifier = Modifier.weight(1f).fillMaxWidth().heightIn(min = 250.dp),
            hand = hand,
            onTouch = { hand.stop() },
        )
        Who(locket, photos, names, colors)
        Column(
            Modifier.padding(horizontal = 18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Field(
                stringResource(Res.string.locket_name_label),
                stringResource(Res.string.locket_name),
                colors,
            )
            Field(
                stringResource(Res.string.locket_bio_label),
                stringResource(Res.string.locket_bio),
                colors,
            )
        }
        Box(
            Modifier.padding(start = 18.dp, end = 18.dp, top = 14.dp, bottom = 22.dp)
                .fillMaxWidth()
                .heightIn(min = 50.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(colors.ink)
                .clickable(role = Role.Button) {},
            contentAlignment = Alignment.Center,
        ) {
            Text(
                stringResource(Res.string.locket_save),
                color = colors.background,
                style = css(16f, FontWeight(650), lines = 1.2f),
            )
        }
    }
}

/** The screen's bar: back, its title, and whose profile it is. */
@Composable
private fun AppBar(colors: LocketScreenColors) {
    Row(
        Modifier.fillMaxWidth().padding(start = 6.dp, end = 14.dp, top = 16.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        // The screen's own back, drawn as the prototype's: the demo screen's bar goes back.
        Canvas(Modifier.size(44.dp).clearAndSetSemantics {}) {
            val s = 22.dp.toPx() / 24f
            val o = Offset((size.width - 22.dp.toPx()) / 2f, (size.height - 22.dp.toPx()) / 2f)
            val w = 2.1f * s
            drawLine(
                colors.ink,
                o + Offset(14.5f * s, 5.5f * s),
                o + Offset(8f * s, 12f * s),
                w,
                StrokeCap.Round,
            )
            drawLine(
                colors.ink,
                o + Offset(8f * s, 12f * s),
                o + Offset(14.5f * s, 18.5f * s),
                w,
                StrokeCap.Round,
            )
        }
        Text(
            stringResource(Res.string.locket_title),
            Modifier.weight(1f),
            color = colors.ink,
            style = css(15.5f, FontWeight(650)),
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            stringResource(Res.string.locket_business),
            Modifier.clip(CircleShape)
                .background(colors.chip)
                .padding(horizontal = 10.dp, vertical = 5.dp),
            color = colors.ink,
            style = css(12f, FontWeight.SemiBold),
            maxLines = 1,
        )
    }
}

/** The round chip others see, the photo's name, read out as it changes, and what to do next. */
@Composable
private fun Who(
    locket: LocketState,
    photos: List<ImageBitmap>,
    names: List<String>,
    colors: LocketScreenColors,
) {
    val status = stringResource(Res.string.locket_status, names[locket.photo])
    val tip =
        when {
            locket.isOpen && locket.placed > 0 -> stringResource(Res.string.locket_tip_placed)
            locket.isOpen -> stringResource(Res.string.locket_tip_open)
            else -> stringResource(Res.string.locket_tip_closed)
        }
    // While a photo trims into the oval the text holds still, as the prototype's; it changes once,
    // when the photo is in. A photo dropped on the closed locket opens it as it starts, so this
    // also
    // keeps the opening from changing the tip a moment before the photo lands.
    val held = remember { arrayOf(status, tip) }
    if (locket.placingPhoto < 0) {
        held[0] = status
        held[1] = tip
    }
    Row(
        Modifier.fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, bottom = 10.dp)
            .heightIn(min = 48.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(11.dp),
    ) {
        // The avatar crossfades from the photo it showed to the new one, as the prototype's: the
        // old photo whole, the new over it.
        val avatar = rememberCrossfade(locket.photo, locket.resets, locket.reducedMotion)
        val ring = remember { Path() }
        Canvas(Modifier.size(36.dp).testTag("avatar").clearAndSetSemantics {}) {
            val r = size.minDimension / 2f
            // The CSS rings: 2 dp of the card, then 1.5 dp of the hairline, outside the photo.
            drawCircle(colors.line, r + 3.5.dp.toPx())
            drawCircle(colors.card, r + 2.dp.toPx())
            ring.rewind()
            ring.addOval(Rect(Offset.Zero, size))
            clipPath(ring) {
                val e = avatar.progress
                val old = avatar.previous
                if (old != null && e < 1f) drawPhoto(photos[old], 1f)
                drawPhoto(photos[avatar.current], e)
            }
        }
        Column(Modifier.weight(1f)) {
            CrossfadeText(
                held[0],
                locket,
                Modifier.testTag("status").semantics { liveRegion = LiveRegionMode.Polite },
                colors.ink,
                css(14f, FontWeight(650)),
            )
            CrossfadeText(held[1], locket, Modifier.testTag("tip"), colors.muted, css(12f))
        }
    }
}

private fun DrawScope.drawPhoto(photo: ImageBitmap, alpha: Float) =
    drawImage(
        photo,
        IntOffset.Zero,
        IntSize(photo.width, photo.height),
        IntOffset.Zero,
        IntSize(size.width.toInt(), size.height.toInt()),
        alpha = alpha,
    )

/**
 * A crossfade between values of [target], as the prototype's: the new one fades in while the old
 * one fades out, over [Locket.FADE_MS] with its smoothstep. Cut short, the half-shown value becomes
 * one more fading [ghosts] entry, from the opacity it had reached, and older ones keep fading as
 * they were, so nothing jumps. A change of [snapKey] (a reset) or [reduced] motion jumps instead.
 * [progress] and each ghost's [Ghost.alpha] are read in the draw phase.
 */
@Stable
private class Crossfade<T>(initial: T) {
    var current by mutableStateOf(initial)
    val ghosts = mutableStateListOf<Ghost<T>>()
    val fade = Animatable(1f)
    val progress: Float
        get() = Locket.fadeEase(fade.value)

    /** The value shown before [current], if it is still fading out. */
    val previous: T?
        get() = ghosts.lastOrNull()?.value
}

/** A value fading out, from the opacity [from] it showed when it was replaced. */
@Stable
private class Ghost<T>(val value: T, val from: Float) {
    val fade = Animatable(0f)
    val alpha: Float
        get() = from * (1f - Locket.fadeEase(fade.value))
}

@Composable
private fun <T> rememberCrossfade(target: T, snapKey: Int, reduced: Boolean): Crossfade<T> {
    val x = remember { Crossfade(target) }
    val scope = rememberCoroutineScope()
    var key by remember { mutableIntStateOf(snapKey) }
    LaunchedEffect(target, snapKey) {
        if (reduced || snapKey != key) {
            key = snapKey
            x.current = target
            x.ghosts.clear()
            x.fade.snapTo(1f)
            return@LaunchedEffect
        }
        if (target == x.current) return@LaunchedEffect
        val ghost = Ghost(x.current, x.progress)
        x.ghosts += ghost
        // Each ghost fades on its own, so a later change can't cut it off.
        scope.launch {
            ghost.fade.animateTo(1f, tween(Locket.FADE_MS.toInt(), easing = LinearEasing))
            x.ghosts.remove(ghost)
        }
        x.current = target
        x.fade.snapTo(0f)
        x.fade.animateTo(1f, tween(Locket.FADE_MS.toInt(), easing = LinearEasing))
    }
    return x
}

/**
 * One line of the who row that crossfades in place when it changes. Its semantics carry the new
 * text at once, so a screen reader and tests never see the old one.
 */
@Composable
private fun CrossfadeText(
    text: String,
    locket: LocketState,
    modifier: Modifier,
    color: Color,
    style: TextStyle,
) {
    val x = rememberCrossfade(text, locket.resets, locket.reducedMotion)
    Box(modifier.fillMaxWidth().clearAndSetSemantics { this.text = AnnotatedString(text) }) {
        for (ghost in x.ghosts) {
            key(ghost) {
                Text(
                    ghost.value,
                    Modifier.graphicsLayer { alpha = ghost.alpha },
                    color = color,
                    style = style,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Text(
            x.current,
            Modifier.graphicsLayer { alpha = x.progress },
            color = color,
            style = style,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** A filled-in field of the profile: its label over its value, in a hairline card. */
@Composable
private fun Field(label: String, value: String, colors: LocketScreenColors) {
    Column(
        Modifier.fillMaxWidth()
            .clip(FieldShape)
            .background(colors.card)
            .border(1.dp, colors.line, FieldShape)
            .padding(start = 14.dp, end = 14.dp, top = 8.dp, bottom = 9.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Text(
            label,
            color = colors.muted,
            style = css(11.5f, FontWeight.SemiBold).copy(letterSpacing = .02.em),
        )
        Text(
            value,
            color = colors.ink,
            style = css(15.5f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private val FieldShape = RoundedCornerShape(14.dp)

// The prototype's text sits on CSS lines, 1.55 times its size unless it says otherwise, with the
// spare height shared above and below.
private val CssLines = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.None)

private fun css(size: Float, weight: FontWeight = FontWeight.Normal, lines: Float = 1.55f) =
    TextStyle(
        fontSize = size.sp,
        fontWeight = weight,
        lineHeight = (size * lines).sp,
        lineHeightStyle = CssLines,
    )
