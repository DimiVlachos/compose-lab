package dev.dimvlachos.lab.birdcagedemo.presentation.components

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import dev.dimvlachos.lab.birdcagedemo.BirdcageDemos
import dev.dimvlachos.lab.core.demo.DemoState
import dev.dimvlachos.lab.core.demo.demoInsets
import dev.dimvlachos.lab.core.platform.rememberReducedMotion
import dev.dimvlachos.lab.core.platform.rememberSoundPlayer
import dev.dimvlachos.lab.core.presentation.components.birdcage.Birdcage
import dev.dimvlachos.lab.core.presentation.components.birdcage.BirdcageBox
import dev.dimvlachos.lab.core.presentation.components.birdcage.BirdcageSounds
import dev.dimvlachos.lab.core.presentation.components.birdcage.BirdcageState
import dev.dimvlachos.lab.core.presentation.components.birdcage.CX
import dev.dimvlachos.lab.core.presentation.components.birdcage.HemDown
import dev.dimvlachos.lab.core.presentation.components.birdcage.HemUp
import dev.dimvlachos.lab.core.presentation.components.touch.drawTouch
import dev.dimvlachos.lab.core.presentation.ui.BirdcageScreenColors
import dev.dimvlachos.lab.core.presentation.ui.LabTheme
import dev.dimvlachos.lab.resources.Res
import dev.dimvlachos.lab.resources.birdcage_edit
import dev.dimvlachos.lab.resources.birdcage_group
import dev.dimvlachos.lab.resources.birdcage_group_meta
import dev.dimvlachos.lab.resources.birdcage_initials
import dev.dimvlachos.lab.resources.birdcage_latest
import dev.dimvlachos.lab.resources.birdcage_media
import dev.dimvlachos.lab.resources.birdcage_media_count
import dev.dimvlachos.lab.resources.birdcage_mute
import dev.dimvlachos.lab.resources.birdcage_muted
import dev.dimvlachos.lab.resources.birdcage_muted_tip
import dev.dimvlachos.lab.resources.birdcage_on
import dev.dimvlachos.lab.resources.birdcage_on_tip
import dev.dimvlachos.lab.resources.birdcage_pinned
import dev.dimvlachos.lab.resources.birdcage_pinned_count
import dev.dimvlachos.lab.resources.birdcage_silenced
import dev.dimvlachos.lab.resources.birdcage_title
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.stringResource

/**
 * The prototype's group info screen: the chat's mute switch is a brass birdcage. Messages make the
 * canary sing; pull the cover down to mute. The script's drags go through the same hem a finger
 * pulls.
 */
@Composable
internal fun BirdcageDemo(
    state: DemoState,
    cage: BirdcageState = remember { BirdcageState() },
    reducedMotion: Boolean = rememberReducedMotion(),
) {
    val sounds = rememberSoundPlayer(BirdcageSounds)
    SideEffect { cage.reducedMotion = reducedMotion }
    val chat = remember { GroupChat() }
    val flash = remember { Animatable(0f) }
    val finger = remember { HemFinger() }
    val density = LocalDensity.current.density
    val slop = 6f * density
    var root by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var hero by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var tourEnded by remember { mutableStateOf(false) }

    suspend fun deliver() {
        chat.next()
        if (cage.deliver()) {
            // The Latest row lights up and fades back, as the prototype's flash.
            try {
                flash.snapTo(1f)
                delay(FlashMs)
                flash.animateTo(0f, tween(FadeMs, easing = CssEase))
            } finally {
                // Cut short, the row still clears, as the prototype's timeout always does.
                withContext(NonCancellable) { flash.snapTo(0f) }
            }
        }
    }

    DisposableEffect(state, cage) {
        state.setControlTapHandler { control ->
            when (control) {
                BirdcageDemos.Message -> deliver()
                BirdcageDemos.CoverDown -> {
                    val r = root
                    if (r != null) finger.place(Offset(r.size.width * .78f, r.size.height * .78f))
                    finger.dragHem(root, hero, cage, HemDown - HemUp - 40f, DownMs, density, slop)
                    delay(500)
                    finger.hide()
                }
                BirdcageDemos.CoverUp -> {
                    finger.dragHem(root, hero, cage, -(HemDown - HemUp) * .8f, UpMs, density, slop)
                    delay(400)
                    finger.hide()
                }
            }
        }
        // Back as it began: the cover up, the chat on and the first message showing, so it loops.
        state.setFeedResetHandler {
            cage.reset()
            chat.reset()
            finger.hide()
            tourEnded = true
        }
        onDispose {
            state.setControlTapHandler(null)
            state.setFeedResetHandler(null)
        }
    }
    // A script the user takes over leaves no fingertip behind.
    LaunchedEffect(state.replay) { if (!state.replay) finger.hide() }
    // Once the chat is the user's, a message drops in now and then, so they hear the difference.
    // Only while the screen is in front: a chirp from a phone in a pocket is no use to anyone.
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(state.replay, state.recording, lifecycle) {
        if (state.replay || state.recording) return@LaunchedEffect
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            delay(
                when {
                    reducedMotion -> ReducedFirstMessageMs
                    tourEnded -> AfterTourMs
                    else -> FirstMessageMs
                },
            )
            while (true) {
                while (cage.dragging) delay(50)
                deliver()
                delay(EveryMessageMs)
            }
        }
    }

    val colors = if (LabTheme.isDark) BirdcageScreenColors.Dark else BirdcageScreenColors.Light
    val touch = LabTheme.colors.touch
    Box(Modifier.fillMaxSize().background(colors.background).onGloballyPositioned { root = it }) {
        Column(Modifier.fillMaxSize().windowInsetsPadding(demoInsets(state))) {
            AppBar(colors)
            Group(colors)
            Birdcage(
                state = cage,
                label = stringResource(Res.string.birdcage_mute),
                sounds = sounds,
                focusRing = colors.accent,
                modifier =
                    Modifier.weight(1f).fillMaxWidth().heightIn(min = 220.dp).onGloballyPositioned {
                        hero = it
                    },
            )
            StateText(cage.muted, reducedMotion, colors)
            Details(chat.latest, cage.silenced, { flash.value }, colors)
            Spacer(Modifier.height(22.dp))
        }
        Canvas(Modifier.fillMaxSize()) { with(finger) { drawFinger(touch) } }
    }
}

/** The step's bar: back, the screen's title, and Edit. */
@Composable
private fun AppBar(colors: BirdcageScreenColors) {
    Row(
        Modifier.fillMaxWidth().padding(start = 6.dp, end = 14.dp, top = 16.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        // The chat's own back, drawn as the prototype's: the demo screen's bar goes back.
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
        BasicText(
            stringResource(Res.string.birdcage_title),
            Modifier.weight(1f).semantics { heading() },
            style =
                css(15.5f, FontWeight(650)).copy(color = colors.ink, textAlign = TextAlign.Center),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        BasicText(
            stringResource(Res.string.birdcage_edit),
            Modifier.padding(horizontal = 6.dp),
            style = css(14f, FontWeight.SemiBold).copy(color = colors.accent),
            maxLines = 1,
        )
    }
}

/** The group: its avatar, its name, and who and where it is. */
@Composable
private fun Group(colors: BirdcageScreenColors) {
    Row(
        Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier.size(46.dp)
                .clip(CircleShape)
                .drawBehind { drawAvatar() }
                .clearAndSetSemantics {},
            contentAlignment = Alignment.Center,
        ) {
            BasicText(
                stringResource(Res.string.birdcage_initials),
                style = css(16f, FontWeight.Bold).copy(color = Color.White),
            )
        }
        Column(Modifier.weight(1f)) {
            BasicText(
                stringResource(Res.string.birdcage_group),
                style =
                    css(19f, FontWeight.Bold, lines = 1.2f)
                        .copy(color = colors.ink, letterSpacing = (-.01).em),
            )
            BasicText(
                stringResource(Res.string.birdcage_group_meta),
                Modifier.padding(top = 1.dp),
                style = css(12.5f).copy(color = colors.muted),
            )
        }
    }
}

/** CSS's `linear-gradient(140deg, …)` across the avatar's square. */
private fun DrawScope.drawAvatar() {
    val a = 140f * PI.toFloat() / 180f
    val dx = sin(a)
    val dy = -cos(a)
    val len = abs(size.width * dx) + abs(size.height * dy)
    val c = Offset(size.width / 2f, size.height / 2f)
    val half = Offset(dx * len / 2f, dy * len / 2f)
    drawRect(
        Brush.linearGradient(
            listOf(BirdcageScreenColors.AvatarFrom, BirdcageScreenColors.AvatarTo),
            start = c - half,
            end = c + half,
        ),
    )
}

/**
 * The state under the cage, read out as it changes: the drawing is never the only cue. Each line
 * crossfades in place, the old one fading out under the new, as the prototype's `xfade()` does.
 */
@Composable
private fun StateText(muted: Boolean, reducedMotion: Boolean, colors: BirdcageScreenColors) {
    val fade = tween<Float>(if (reducedMotion) 0 else StateFadeMs, easing = EaseInOutQuad)
    Column(
        Modifier.fillMaxWidth()
            .padding(start = 22.dp, end = 22.dp, bottom = 10.dp)
            .heightIn(min = 46.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Crossfade(muted, animationSpec = fade, label = "state") { m ->
            BasicText(
                stringResource(if (m) Res.string.birdcage_muted else Res.string.birdcage_on),
                Modifier.fillMaxWidth().testTag("state").semantics {
                    liveRegion = LiveRegionMode.Polite
                },
                style =
                    css(15f, FontWeight.Bold)
                        .copy(color = colors.ink, textAlign = TextAlign.Center),
            )
        }
        Crossfade(muted, animationSpec = fade, label = "tip") { m ->
            BasicText(
                stringResource(
                    if (m) Res.string.birdcage_muted_tip else Res.string.birdcage_on_tip,
                ),
                Modifier.fillMaxWidth().testTag("tip"),
                style = css(12.5f).copy(color = colors.muted, textAlign = TextAlign.Center),
            )
        }
    }
}

/** The prototype's state-line crossfade, in ms. */
internal const val StateFadeMs = 250

/** The prototype's `ease()`, an in-out quad, as a cubic. */
private val EaseInOutQuad = CubicBezierEasing(.455f, .03f, .515f, .955f)

/** The chat's card: the latest message with its silenced count, then media and pins. */
@Composable
private fun Details(
    latest: String,
    silenced: Int,
    flash: () -> Float,
    colors: BirdcageScreenColors,
) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        Modifier.padding(horizontal = 16.dp)
            .fillMaxWidth()
            .clip(shape)
            .background(colors.card)
            .border(1.dp, colors.line, shape),
    ) {
        Row(
            Modifier.fillMaxWidth()
                .drawBehind { drawRect(colors.accent.copy(alpha = .12f * flash())) }
                .heightIn(min = 48.dp)
                .padding(horizontal = 14.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Column(Modifier.weight(1f)) {
                BasicText(
                    stringResource(Res.string.birdcage_latest),
                    style =
                        css(11.5f, FontWeight.SemiBold)
                            .copy(color = colors.muted, letterSpacing = .02.em),
                )
                BasicText(
                    latest,
                    Modifier.testTag("latest"),
                    style = css(14.5f).copy(color = colors.ink),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (silenced > 0) {
                BasicText(
                    stringResource(Res.string.birdcage_silenced, silenced),
                    Modifier.testTag("silenced")
                        .clip(RoundedCornerShape(999.dp))
                        .background(colors.chip)
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                    style = css(11.5f, FontWeight.Bold).copy(color = colors.quietBadge),
                    maxLines = 1,
                )
            }
        }
        Line(colors)
        CountRow(
            stringResource(Res.string.birdcage_media),
            stringResource(Res.string.birdcage_media_count),
            colors,
        )
        Line(colors)
        CountRow(
            stringResource(Res.string.birdcage_pinned),
            stringResource(Res.string.birdcage_pinned_count),
            colors,
        )
    }
}

@Composable
private fun Line(colors: BirdcageScreenColors) {
    Box(Modifier.fillMaxWidth().height(1.dp).background(colors.line))
}

@Composable
private fun CountRow(text: String, count: String, colors: BirdcageScreenColors) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        BasicText(
            text,
            Modifier.weight(1f),
            style = css(14.5f).copy(color = colors.ink),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        BasicText(count, style = css(13f).copy(color = colors.muted))
    }
}

/** The group's messages, as the prototype writes them: an invented running club in Austin. */
@Stable
private class GroupChat {
    private var index by mutableIntStateOf(-1)

    val latest: String
        get() = if (index < 0) First else Messages[index % Messages.size]

    fun next() {
        index++
    }

    fun reset() {
        index = -1
    }

    companion object {
        const val First = "Maya: Saturday long run starts at 7:00 AM."
        val Messages =
            listOf(
                "Jordan: Coffee after at Bluebonnet Bakery?",
                "Coach Lee: Track tonight is 6 × 800 at 6:30 PM.",
                "Priya: Lost a blue bottle at the trailhead. Anyone?",
                "Sam: Hill repeats are off, it's pouring.",
                "Maya: New route map is pinned.",
                "Dev: Who's in for the Turkey Trot?",
            )
    }
}

/**
 * The scripted fingertip: it fades in, glides to the hem, pulls it through the same drag a finger
 * makes, and fades out, as the prototype's does.
 */
@Stable
private class HemFinger {
    private val alpha = Animatable(0f)
    private var at by mutableStateOf(Offset.Zero)

    /** Puts the fingertip at [from], to fade in as it glides from there to the hem. */
    fun place(from: Offset) {
        at = from
    }

    suspend fun hide() {
        alpha.animateTo(0f, tween(FadeInMs, easing = CssEase))
    }

    private suspend fun glide(to: Offset, ms: Int) {
        val from = at
        animate(ms) { e -> at = Offset(from.x + (to.x - from.x) * e, from.y + (to.y - from.y) * e) }
    }

    /**
     * Glides to the hem, takes it, and drags it [dyBox] of the cage's units over [ms], then lets
     * go. Cut short, it still lets go, so the cover is never left held.
     */
    suspend fun dragHem(
        root: LayoutCoordinates?,
        hero: LayoutCoordinates?,
        cage: BirdcageState,
        dyBox: Float,
        ms: Int,
        density: Float,
        slop: Float,
    ) {
        val r = root?.takeIf { it.isAttached } ?: return
        val h = hero?.takeIf { it.isAttached } ?: return
        val origin = r.localPositionOf(h, Offset.Zero)
        val box = BirdcageBox(h.size.width / density, h.size.height / density)
        val px = box.scale * density
        val hem =
            Offset(
                origin.x + (box.left + CX * box.scale) * density,
                origin.y + (box.top + (cage.hemY + 2f) * box.scale) * density,
            )
        coroutineScope {
            launch { alpha.animateTo(1f, tween(FadeInMs, easing = CssEase)) }
            glide(hem, GlideMs)
        }
        delay(PressMs)
        val y0 = hem.y - origin.y
        val id = cage.down(y0, cage.time)
        var held = true
        try {
            animate(ms) { e ->
                val y = y0 + dyBox * px * e
                // A finger that took the hem over has it from here.
                if (cage.holds(id)) cage.move(y, cage.time, px, slop)
                at = Offset(hem.x, y + origin.y)
            }
            delay(60)
            if (cage.holds(id)) cage.up(cage.time)
            held = false
        } finally {
            if (held && cage.holds(id)) withContext(NonCancellable) { cage.up(cage.time) }
        }
    }

    fun DrawScope.drawFinger(color: Color) {
        val a = alpha.value
        if (a <= 0f) return
        drawTouch(color, at, a)
    }
}

/** Runs [frame] each frame for [ms] with the prototype's eased progress, ending at exactly 1. */
private suspend fun animate(ms: Int, frame: (Float) -> Unit) {
    var start = -1L
    while (true) {
        val done = withFrameMillis { now ->
            if (start < 0) start = now
            val p = ((now - start).toFloat() / ms).coerceIn(0f, 1f)
            frame(ease(p))
            p >= 1f
        }
        if (done) return
    }
}

/** The prototype's ease: quadratic in and out. */
private fun ease(t: Float) = if (t < .5f) 2f * t * t else 1f - (-2f * t + 2f).pow(2) / 2f

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

private const val FlashMs = 60L
private const val FadeMs = 500
private const val FadeInMs = 200
private const val GlideMs = 500
private const val PressMs = 120L
private const val DownMs = 700
private const val UpMs = 650
private const val FirstMessageMs = 7000L
// The prototype's next message comes 8 s after the tour's last, which is 2 s before its reset.
private const val AfterTourMs = 6000L
private const val ReducedFirstMessageMs = 4000L

/** CSS's `ease`, which the prototype's fades use. */
private val CssEase = CubicBezierEasing(.25f, .1f, .25f, 1f)
private const val EveryMessageMs = 9000L
