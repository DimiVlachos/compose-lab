package dev.dimvlachos.lab.core.presentation.components.frostcard

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.CompositingStrategy
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import dev.dimvlachos.lab.core.platform.Sound
import dev.dimvlachos.lab.core.platform.SoundLoop
import dev.dimvlachos.lab.core.platform.SoundPlayer
import dev.dimvlachos.lab.core.platform.playVaried
import dev.dimvlachos.lab.core.presentation.canvas2d.Canvas2D
import dev.dimvlachos.lab.core.presentation.ui.FrostCardScreenColors
import dev.dimvlachos.lab.resources.Res
import dev.dimvlachos.lab.resources.frostcard_account
import dev.dimvlachos.lab.resources.frostcard_activity
import dev.dimvlachos.lab.resources.frostcard_back
import dev.dimvlachos.lab.resources.frostcard_balance
import dev.dimvlachos.lab.resources.frostcard_card
import dev.dimvlachos.lab.resources.frostcard_card_frozen
import dev.dimvlachos.lab.resources.frostcard_freeze
import dev.dimvlachos.lab.resources.frostcard_frozen_said
import dev.dimvlachos.lab.resources.frostcard_hint
import dev.dimvlachos.lab.resources.frostcard_hint_frozen
import dev.dimvlachos.lab.resources.frostcard_more
import dev.dimvlachos.lab.resources.frostcard_sub
import dev.dimvlachos.lab.resources.frostcard_sub_frozen
import dev.dimvlachos.lab.resources.frostcard_tab_cards
import dev.dimvlachos.lab.resources.frostcard_tab_home
import dev.dimvlachos.lab.resources.frostcard_tab_more
import dev.dimvlachos.lab.resources.frostcard_tab_pay
import dev.dimvlachos.lab.resources.frostcard_title
import dev.dimvlachos.lab.resources.frostcard_tx_1
import dev.dimvlachos.lab.resources.frostcard_tx_1_amount
import dev.dimvlachos.lab.resources.frostcard_tx_1_when
import dev.dimvlachos.lab.resources.frostcard_tx_2
import dev.dimvlachos.lab.resources.frostcard_tx_2_amount
import dev.dimvlachos.lab.resources.frostcard_tx_2_when
import dev.dimvlachos.lab.resources.frostcard_tx_3
import dev.dimvlachos.lab.resources.frostcard_tx_3_amount
import dev.dimvlachos.lab.resources.frostcard_tx_3_when
import dev.dimvlachos.lab.resources.frostcard_tx_4
import dev.dimvlachos.lab.resources.frostcard_tx_4_amount
import dev.dimvlachos.lab.resources.frostcard_tx_4_when
import dev.dimvlachos.lab.resources.frostcard_tx_5
import dev.dimvlachos.lab.resources.frostcard_tx_5_amount
import dev.dimvlachos.lab.resources.frostcard_tx_5_when
import dev.dimvlachos.lab.resources.frostcard_unfrozen_said
import dev.dimvlachos.lab.resources.jetbrains_mono_semibold
import dev.dimvlachos.lab.resources.pt_serif_bold_italic
import kotlin.math.ceil
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import org.jetbrains.compose.resources.Font
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/** The clips the card plays. */
internal val FrostCardSounds =
    setOf(
        Sound.FrostFreeze,
        Sound.FrostTick,
        Sound.FrostHiss,
        Sound.FrostDrip,
        Sound.FrostSet,
        Sound.FrostThawed,
    )

/**
 * Where things are on the screen, in dp from its top left, for the script's finger: the switch's
 * centre, the card's box, and the screen's size.
 */
@Stable
internal class FrostCardLayout {
    var switch by mutableStateOf(Offset.Zero)
    var card by mutableStateOf(Rect.Zero)
    var width by mutableFloatStateOf(0f)
    var height by mutableFloatStateOf(0f)
}

/**
 * The prototype's bank card screen: the Juniper debit card, the account, the Freeze card switch, a
 * hint, the recent activity and the tabs. Flip the switch (or hold the warm card for half a second)
 * and frost grows over the card; hold a thumb on the frozen card to thaw it. [onToggle] is what the
 * switch does, so the script's tap goes through the same path.
 */
@Composable
internal fun FrostCardScreen(
    state: FrostCardState,
    layout: FrostCardLayout,
    colors: FrostCardScreenColors,
    sounds: SoundPlayer,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = LocalHapticFeedback.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val frozenSaid = stringResource(Res.string.frostcard_frozen_said)
    val unfrozenSaid = stringResource(Res.string.frostcard_unfrozen_said)
    var said by remember { mutableStateOf("") }
    var pending by remember { mutableStateOf("") }
    var sayings by remember { mutableIntStateOf(0) }
    LaunchedEffect(sayings) {
        if (sayings == 0) return@LaunchedEffect
        said = ""
        delay(30)
        said = pending
    }
    LaunchedEffect(state, sounds, haptics, frozenSaid, unfrozenSaid) {
        val listener: (FrostEvent, Float) -> Unit = { event, v ->
            when (event) {
                FrostEvent.Freeze -> sounds.play(Sound.FrostFreeze)
                FrostEvent.Crackle ->
                    sounds.playVaried(Sound.FrostTick, (v / .042f).coerceIn(.2f, 1f))
                FrostEvent.Drip -> sounds.playVaried(Sound.FrostDrip, v.coerceIn(.3f, 1f))
                FrostEvent.Frozen -> {
                    sounds.play(Sound.FrostSet)
                    haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                    pending = frozenSaid
                    sayings++
                }
                FrostEvent.Unfrozen -> {
                    sounds.play(Sound.FrostThawed)
                    haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                    pending = unfrozenSaid
                    sayings++
                }
            }
        }
        state.onEvent = listener
        try {
            awaitCancellation()
        } finally {
            if (state.onEvent === listener) state.onEvent = null
        }
    }
    LaunchedEffect(state, sounds, lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            // The melt's hiss: a loop while the thaw runs, its volume following it, and stopped
            // with the loop when the screen is left.
            var hiss: SoundLoop? = null
            try {
                var last = 0L
                while (true) {
                    // At rest the loop sleeps, until a touch, the switch or the script moves it.
                    if (!state.busy) {
                        val seen = state.frame
                        snapshotFlow { state.frame }.first { it != seen }
                        last = 0L
                    }
                    // Frozen over and still, only the glints move: a calmer 30 frames a second.
                    if (state.resting) delay(33)
                    withFrameNanos { now ->
                        val dt = if (last == 0L) 0f else ((now - last) / 1e9f).coerceAtMost(.15f)
                        last = now
                        state.step(dt)
                    }
                    if (state.hiss > .01f) {
                        val loop = hiss ?: sounds.loop(Sound.FrostHiss, 0f).also { hiss = it }
                        loop.setVolume(state.hiss)
                    } else if (hiss != null) {
                        hiss?.stop()
                        hiss = null
                    }
                }
            } finally {
                hiss?.stop()
            }
        }
    }

    val density = LocalDensity.current
    val serif = FontFamily(Font(Res.font.pt_serif_bold_italic, FontWeight.Bold, FontStyle.Italic))
    val mono = FontFamily(Font(Res.font.jetbrains_mono_semibold, FontWeight.SemiBold))
    val fonts =
        remember(serif, mono) {
            mapOf(FrostSerif to serif, FrostMono to mono, "sans" to FontFamily.Default)
        }
    val measurer = rememberTextMeasurer(cacheSize = 32)
    val painter =
        remember(density.density, fonts, measurer) {
            FrostCardPainter(density.density, measurer, fonts)
        }
    val frozen = state.frozen

    BoxWithConstraints(modifier.background(colors.background)) {
        val short = maxHeight < 640.dp
        var origin by remember { mutableStateOf(Offset.Zero) }
        Column(
            Modifier.fillMaxSize().onGloballyPositioned {
                origin = it.positionInRoot()
                layout.width = it.size.width / density.density
                layout.height = it.size.height / density.density
            },
        ) {
            Bar(colors)
            // The card's place: the canvas over everything draws it, and touches land here.
            Box(Modifier.padding(start = 16.dp, end = 16.dp, top = 6.dp)) {
                val cardLabel =
                    stringResource(
                        if (frozen) Res.string.frostcard_card_frozen else Res.string.frostcard_card,
                    )
                Box(
                    Modifier.fillMaxWidth()
                        .aspectRatio(1.586f)
                        .onGloballyPositioned {
                            // Its own size, not its clipped bounds: a clip or a scale in a
                            // transition must not grow the frost again.
                            val at = it.positionInRoot()
                            val d = density.density
                            val w = it.size.width / d
                            val h = it.size.height / d
                            val x = (at.x - origin.x) / d
                            val y = (at.y - origin.y) / d
                            layout.card = Rect(x, y, x + w, y + h)
                            state.layout(w, h, layout.height - y)
                        }
                        .semantics {
                            contentDescription = cardLabel
                            role = Role.Image
                        }
                        .pointerInput(state) {
                            awaitEachGesture {
                                val down = awaitFirstDown(requireUnconsumed = false)
                                state.pressStart(
                                    down.position.x / density.density,
                                    down.position.y / density.density,
                                )
                                // A cancelled gesture lets go too, as a pointercancel does.
                                try {
                                    while (true) {
                                        val e = awaitPointerEvent()
                                        val ch = e.changes.firstOrNull { it.id == down.id } ?: break
                                        if (!ch.pressed) break
                                        state.pressMove(
                                            ch.position.x / density.density,
                                            ch.position.y / density.density,
                                        )
                                        ch.consume()
                                    }
                                } finally {
                                    state.pressEnd()
                                }
                            }
                        },
                )
            }
            Account(colors, short)
            FreezeRow(state, layout, colors, origin, state.reducedMotion, onToggle)
            if (!short) Hint(frozen, colors)
            Activity(frozen, colors, Modifier.weight(1f))
            Tabs(colors)
        }
        // In a layer of its own, so the card redrawing every frame leaves the screen's other
        // drawing recorded as it was.
        val iceLayer = rememberGraphicsLayer()
        Canvas(Modifier.fillMaxSize().graphicsLayer()) {
            state.frame
            if (state.growth == null) return@Canvas
            val d = density.density
            val x = layout.card.left
            val y = layout.card.top
            state.floor(size.height / d - y)
            drawIntoCanvas { canvas ->
                val c = Canvas2D(canvas, size.width.toInt(), size.height.toInt())
                c.scale(d, d)
                painter.drawBase(c, state, x, y)
                if (painter.live(state)) {
                    // The ice that changes this frame is composed in a layer of its own, the
                    // size of what changes, at the painter's resolution for it, and laid over
                    // the card; the finished ice round it is drawn as it was baked.
                    val box = painter.liveBox(state)
                    val k = painter.liveDensity(state)
                    val w = ceil(box.width * k).toInt()
                    val h = ceil(box.height * k).toInt()
                    if (w > 0 && h > 0) {
                        iceLayer.compositingStrategy = CompositingStrategy.Offscreen
                        iceLayer.record(Density(k), LayoutDirection.Ltr, IntSize(w, h)) {
                            drawIntoCanvas { lc ->
                                val l = Canvas2D(lc, w, h)
                                l.scale(k, k)
                                painter.composeLive(l, state, box)
                            }
                        }
                        painter.drawStill(c, state, x, y, box)
                        translate((x + box.left) * d, (y + box.top) * d) {
                            scale(d / k, d / k, pivot = Offset.Zero) { drawLayer(iceLayer) }
                        }
                    }
                } else {
                    painter.drawStill(c, state, x, y, null)
                }
                painter.drawAbove(c, state, x, y)
            }
        }
        Box(
            Modifier.size(1.dp).semantics {
                liveRegion = LiveRegionMode.Polite
                if (said.isNotEmpty()) contentDescription = said
            },
        )
    }
}

@Composable
private fun Bar(colors: FrostCardScreenColors) {
    Box(
        Modifier.fillMaxWidth().heightIn(min = 46.dp).padding(horizontal = 12.dp, vertical = 4.dp),
    ) {
        BasicText(
            stringResource(Res.string.frostcard_back),
            Modifier.align(Alignment.CenterStart).padding(horizontal = 4.dp),
            style = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Medium),
            color = { colors.accent },
        )
        BasicText(
            stringResource(Res.string.frostcard_title),
            Modifier.align(Alignment.Center).semantics { heading() },
            style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold),
            color = { colors.ink },
        )
        val more = stringResource(Res.string.frostcard_more)
        BasicText(
            "•••",
            Modifier.align(Alignment.CenterEnd).padding(horizontal = 4.dp).clearAndSetSemantics {
                contentDescription = more
            },
            style = TextStyle(fontSize = 15.sp, letterSpacing = 1.sp),
            color = { colors.muted },
        )
    }
}

@Composable
private fun Account(colors: FrostCardScreenColors, short: Boolean) {
    Row(
        Modifier.fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = if (short) 8.dp else 12.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom,
    ) {
        BasicText(
            stringResource(Res.string.frostcard_account),
            style = TextStyle(fontSize = 13.sp),
            color = { colors.muted },
            maxLines = 1,
        )
        BasicText(
            stringResource(Res.string.frostcard_balance),
            style =
                TextStyle(
                    fontSize = 15.sp,
                    fontWeight = FontWeight(650),
                    fontFeatureSettings = "tnum",
                ),
            color = { colors.ink },
        )
    }
}

private val Flake =
    PathParser()
        .parsePathString(
            "M12 3v18M4.2 7.5l15.6 9M4.2 16.5l15.6-9M12 6.5l-2-2M12 6.5l2-2M12 17.5l-2 2M12 17.5l2 2",
        )
        .toPath()

private val KnobFlake =
    PathParser()
        .parsePathString(
            "M13 4v18M5.2 8.5l15.6 9M5.2 17.5l15.6-9M13 7l-2-2M13 7l2-2M13 19l-2 2M13 19l2 2" +
                "M7.8 10l-2.7.4M7.8 10l-.9-2.6M18.2 16l2.7-.4M18.2 16l.9 2.6M7.8 16l-.9 2.6" +
                "M7.8 16l-2.7-.4M18.2 10l.9-2.6M18.2 10l2.7.4",
        )
        .toPath()

// The Freeze card row: the snowflake tile, the words, and the switch. Its colours ease over 0.8 s
// with the frozen flag, as the prototype's transitions do.
@Composable
private fun FreezeRow(
    state: FrostCardState,
    layout: FrostCardLayout,
    colors: FrostCardScreenColors,
    origin: Offset,
    reducedMotion: Boolean,
    onToggle: () -> Unit,
) {
    val frozen = state.frozen
    // The fades are read while drawing, so they redraw the row without composing it again.
    val slow: AnimationSpec<Color> = if (reducedMotion) snap() else tween(800)
    val border =
        animateColorAsState(if (frozen) colors.ice.copy(alpha = .7f) else colors.line, slow)
    val tile =
        animateColorAsState(if (frozen) colors.ice.copy(alpha = .3f) else colors.background, slow)
    val glyph =
        animateColorAsState(if (frozen) FrostCardScreenColors.IceInk else colors.muted, slow)
    val label = stringResource(Res.string.frostcard_freeze)
    Row(
        Modifier.padding(start = 16.dp, end = 16.dp, top = 6.dp)
            .fillMaxWidth()
            .drawBehind {
                val r = CornerRadius(14.dp.toPx())
                drawRoundRect(colors.card, cornerRadius = r)
                val w = 1.dp.toPx()
                drawRoundRect(
                    border.value,
                    topLeft = Offset(w / 2, w / 2),
                    size = Size(size.width - w, size.height - w),
                    cornerRadius = CornerRadius(r.x - w / 2),
                    style = Stroke(w),
                )
            }
            .toggleable(value = frozen, role = Role.Switch, onValueChange = { onToggle() })
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Canvas(Modifier.size(34.dp)) {
            drawRoundRect(tile.value, cornerRadius = CornerRadius(10.dp.toPx()))
            val k = 20.dp.toPx() / 24f
            val o = 7.dp.toPx()
            translate(o, o) {
                scale(k, k, pivot = Offset.Zero) {
                    drawPath(Flake, glyph.value, style = Stroke(1.7f, cap = StrokeCap.Round))
                }
            }
        }
        Column(Modifier.weight(1f)) {
            BasicText(
                label,
                style = TextStyle(fontSize = 15.sp, fontWeight = FontWeight(650)),
                color = { colors.ink },
            )
            BasicText(
                stringResource(
                    if (frozen) Res.string.frostcard_sub_frozen else Res.string.frostcard_sub,
                ),
                style = TextStyle(fontSize = 12.5.sp),
                color = { colors.muted },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        val density = LocalDensity.current.density
        Switch(
            frozen,
            colors,
            reducedMotion,
            Modifier.onGloballyPositioned {
                val b = it.boundsInRoot()
                val d = density
                layout.switch = Offset((b.center.x - origin.x) / d, (b.center.y - origin.y) / d)
            },
        )
    }
}

// The switch: a track that turns ice blue, and a knob that slides over with the prototype's
// cubic-bezier(.65,0,.35,1) (0.42 s, softened to 0.55) and frosts over.
@Composable
private fun Switch(
    on: Boolean,
    colors: FrostCardScreenColors,
    reducedMotion: Boolean,
    modifier: Modifier,
) {
    fun <T> spec(t: AnimationSpec<T>): AnimationSpec<T> = if (reducedMotion) snap() else t
    val track by animateColorAsState(if (on) colors.ice else colors.track, spec(tween(700)))
    val x by
        animateFloatAsState(
            if (on) 1f else 0f,
            spec(tween(550, easing = CubicBezierEasing(.65f, 0f, .35f, 1f))),
        )
    val frost by
        animateFloatAsState(if (on) 1f else 0f, spec(tween(900, easing = FastOutSlowInEasing)))
    val flake by animateFloatAsState(if (on) 1f else 0f, spec(tween(1200, delayMillis = 200)))
    Canvas(modifier.size(52.dp, 32.dp).clearAndSetSemantics {}) {
        val d = density
        drawRoundRect(track, cornerRadius = androidx.compose.ui.geometry.CornerRadius(16 * d))
        val kx = 3 * d + x * 20 * d
        val ky = 3 * d
        val r = 13 * d
        val c = Offset(kx + r, ky + r)
        drawCircle(Color.Black.copy(alpha = .25f), r, c + Offset(0f, 1 * d))
        drawCircle(Color.White, r, c)
        if (frost > 0f) {
            drawCircle(
                Brush.radialGradient(
                    0f to FrostCardScreenColors.KnobWhite,
                    .28f to FrostCardScreenColors.KnobWhite,
                    .6f to FrostCardScreenColors.KnobFrost,
                    1f to FrostCardScreenColors.KnobEdge,
                    center = Offset(kx + 26 * d * .34f, ky + 26 * d * .3f),
                    radius = 26 * d * .8f,
                ),
                r,
                c,
                alpha = frost,
            )
        }
        if (flake > 0f) {
            translate(kx, ky) {
                scale(d, d, pivot = Offset.Zero) {
                    drawPath(
                        KnobFlake,
                        FrostCardScreenColors.KnobFlake,
                        alpha = flake,
                        style = Stroke(.9f, cap = StrokeCap.Round),
                    )
                }
            }
        }
    }
}

@Composable
private fun Hint(frozen: Boolean, colors: FrostCardScreenColors) {
    BasicText(
        stringResource(if (frozen) Res.string.frostcard_hint_frozen else Res.string.frostcard_hint),
        Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 6.dp),
        style = TextStyle(fontSize = 12.sp, textAlign = TextAlign.Center),
        color = { colors.muted },
        maxLines = 1,
    )
}

private class Tx(
    val logo: Color,
    val name: StringResource,
    val whenText: StringResource,
    val amount: StringResource,
    val income: Boolean = false,
)

private val Purchases =
    listOf(
        Tx(
            FrostCardScreenColors.Larkspur,
            Res.string.frostcard_tx_1,
            Res.string.frostcard_tx_1_when,
            Res.string.frostcard_tx_1_amount,
        ),
        Tx(
            FrostCardScreenColors.GreenAcres,
            Res.string.frostcard_tx_2,
            Res.string.frostcard_tx_2_when,
            Res.string.frostcard_tx_2_amount,
        ),
        Tx(
            FrostCardScreenColors.Cascade,
            Res.string.frostcard_tx_3,
            Res.string.frostcard_tx_3_when,
            Res.string.frostcard_tx_3_amount,
        ),
        Tx(
            FrostCardScreenColors.Brightline,
            Res.string.frostcard_tx_4,
            Res.string.frostcard_tx_4_when,
            Res.string.frostcard_tx_4_amount,
            income = true,
        ),
        Tx(
            FrostCardScreenColors.Northside,
            Res.string.frostcard_tx_5,
            Res.string.frostcard_tx_5_when,
            Res.string.frostcard_tx_5_amount,
        ),
    )

// Recent activity, which greys out to half strength while the card is frozen (0.9 s), as the
// prototype's `opacity: .5; filter: grayscale(1)`. Done in each colour, read while drawing, rather
// than with two offscreen layers redrawn every frame.
@Composable
private fun Activity(frozen: Boolean, colors: FrostCardScreenColors, modifier: Modifier) {
    val grey = animateFloatAsState(if (frozen) 1f else 0f, tween(900))
    fun dim(c: Color): Color {
        val k = grey.value
        if (k <= 0f) return c
        val l = .2126f * c.red + .7152f * c.green + .0722f * c.blue
        val g = lerp(c, Color(l, l, l, c.alpha), k)
        return lerp(colors.background, g, 1f - .5f * k)
    }
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(0.dp))
            .padding(start = 16.dp, end = 16.dp, top = 10.dp),
    ) {
        BasicText(
            stringResource(Res.string.frostcard_activity),
            Modifier.padding(start = 4.dp, end = 4.dp, bottom = 4.dp).semantics { heading() },
            style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight(650)),
            color = { colors.muted },
        )
        Column {
            Purchases.forEachIndexed { i, tx ->
                Row(
                    Modifier.fillMaxWidth()
                        .drawWithContent {
                            drawContent()
                            if (i < Purchases.size - 1)
                                drawLine(
                                    dim(colors.line),
                                    Offset(0f, size.height - .5f * density),
                                    Offset(size.width, size.height - .5f * density),
                                    1 * density,
                                )
                        }
                        .padding(horizontal = 4.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(11.dp),
                ) {
                    val name = stringResource(tx.name)
                    Box(
                        Modifier.size(34.dp)
                            .drawBehind { drawCircle(dim(tx.logo)) }
                            .clearAndSetSemantics {},
                        Alignment.Center,
                    ) {
                        BasicText(
                            name.take(1),
                            style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Bold),
                            color = { dim(Color.White) },
                        )
                    }
                    Column(Modifier.weight(1f)) {
                        BasicText(
                            name,
                            style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
                            color = { dim(colors.ink) },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        BasicText(
                            stringResource(tx.whenText),
                            style = TextStyle(fontSize = 12.sp),
                            color = { dim(colors.muted) },
                        )
                    }
                    BasicText(
                        stringResource(tx.amount),
                        style =
                            TextStyle(
                                fontSize = 14.sp,
                                fontWeight = FontWeight(650),
                                fontFeatureSettings = "tnum",
                            ),
                        color = { dim(if (tx.income) colors.accent else colors.ink) },
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

private val TabIcons =
    listOf(
            "M4 11l8-6 8 6v8H4z",
            "M5.5 6h13a2.5 2.5 0 0 1 2.5 2.5v7a2.5 2.5 0 0 1-2.5 2.5h-13A2.5 2.5 0 0 1 3 15.5v-7A2.5 2.5 0 0 1 5.5 6zM3 10h18",
            "M5 8h12l-3-3M19 16H7l3 3",
            "",
        )
        .map { PathParser().parsePathString(it).toPath() }

// The tab bar, Cards lit, hidden from screen readers as the prototype's is.
@Composable
private fun Tabs(colors: FrostCardScreenColors) {
    val names =
        listOf(
            stringResource(Res.string.frostcard_tab_home),
            stringResource(Res.string.frostcard_tab_cards),
            stringResource(Res.string.frostcard_tab_pay),
            stringResource(Res.string.frostcard_tab_more),
        )
    Row(
        Modifier.fillMaxWidth()
            .clearAndSetSemantics {}
            .drawWithContent {
                drawContent()
                drawLine(
                    colors.line,
                    Offset(0f, .5f * density),
                    Offset(size.width, .5f * density),
                    1 * density,
                )
            }
            .padding(start = 8.dp, end = 8.dp, top = 2.dp, bottom = 8.dp),
    ) {
        names.forEachIndexed { i, name ->
            val ink = if (i == 1) colors.accent else colors.muted
            Column(
                Modifier.weight(1f).padding(top = 7.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Canvas(Modifier.size(21.dp)) {
                    val s = size.width / 24f
                    val stroke = Stroke(1.8f * s, cap = StrokeCap.Round, join = StrokeJoin.Round)
                    if (i == 3) {
                        for (x in listOf(6f, 12f, 18f)) drawCircle(
                            ink,
                            1.3f * s,
                            Offset(x * s, 12 * s),
                            style = stroke,
                        )
                    } else {
                        scale(s, s, pivot = Offset.Zero) {
                            drawPath(
                                TabIcons[i],
                                ink,
                                style =
                                    Stroke(1.8f, cap = StrokeCap.Round, join = StrokeJoin.Round),
                            )
                        }
                    }
                }
                BasicText(
                    name,
                    style = TextStyle(fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold),
                    color = { ink },
                    maxLines = 1,
                )
            }
        }
    }
}
