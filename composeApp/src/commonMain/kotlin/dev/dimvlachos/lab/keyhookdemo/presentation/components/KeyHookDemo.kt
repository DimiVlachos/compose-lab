package dev.dimvlachos.lab.keyhookdemo.presentation.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import dev.dimvlachos.lab.core.demo.DemoState
import dev.dimvlachos.lab.core.demo.demoInsets
import dev.dimvlachos.lab.core.platform.rememberReducedMotion
import dev.dimvlachos.lab.core.platform.rememberSoundPlayer
import dev.dimvlachos.lab.core.presentation.components.keyhook.KeyHolder
import dev.dimvlachos.lab.core.presentation.components.keyhook.KeyRack
import dev.dimvlachos.lab.core.presentation.components.keyhook.KeyRackSounds
import dev.dimvlachos.lab.core.presentation.components.keyhook.KeyRackState
import dev.dimvlachos.lab.core.presentation.components.keyhook.KeyRing
import dev.dimvlachos.lab.core.presentation.components.keyhook.RackGeometry
import dev.dimvlachos.lab.core.presentation.components.touch.ScriptedTouch
import dev.dimvlachos.lab.core.presentation.components.touch.drawTouch
import dev.dimvlachos.lab.core.presentation.ui.KeyHookScreenColors
import dev.dimvlachos.lab.core.presentation.ui.LabTheme
import dev.dimvlachos.lab.keyhookdemo.Household
import dev.dimvlachos.lab.keyhookdemo.KeyHookDemos
import dev.dimvlachos.lab.keyhookdemo.Show
import dev.dimvlachos.lab.resources.Res
import dev.dimvlachos.lab.resources.keyhook_brand
import dev.dimvlachos.lab.resources.keyhook_continue_watching
import dev.dimvlachos.lab.resources.keyhook_empty
import dev.dimvlachos.lab.resources.keyhook_hint_signed_in
import dev.dimvlachos.lab.resources.keyhook_hint_signed_out
import dev.dimvlachos.lab.resources.keyhook_household
import dev.dimvlachos.lab.resources.keyhook_nobody
import dev.dimvlachos.lab.resources.keyhook_search
import dev.dimvlachos.lab.resources.keyhook_sign_in_as
import dev.dimvlachos.lab.resources.keyhook_sign_out
import dev.dimvlachos.lab.resources.keyhook_signed_in_as
import dev.dimvlachos.lab.resources.keyhook_status_in
import dev.dimvlachos.lab.resources.keyhook_status_out
import dev.dimvlachos.lab.resources.keyhook_tab_account
import dev.dimvlachos.lab.resources.keyhook_tab_home
import dev.dimvlachos.lab.resources.keyhook_tab_list
import dev.dimvlachos.lab.resources.keyhook_tab_live
import dev.dimvlachos.lab.resources.keyhook_take_keys
import dev.dimvlachos.lab.resources.keyhook_welcome
import dev.dimvlachos.lab.resources.keyhook_whos_home
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.stringResource

/**
 * The prototype's family streaming app, whose account switcher is the key rack by the front door:
 * Jordan's keys are clipped to the account card; hang them on the rack to sign out, or carry
 * someone else's set up to the card to sign in as them, and their shows load below. The script's
 * drags go through the same rack a finger does.
 */
@Composable
internal fun KeyHookDemo(
    state: DemoState,
    rack: KeyRackState = remember { KeyRackState(Household.people) },
    reducedMotion: Boolean = rememberReducedMotion(),
) {
    val sounds = rememberSoundPlayer(KeyRackSounds)
    SideEffect { rack.reducedMotion = reducedMotion }
    val finger = remember { ScriptFinger() }

    DisposableEffect(state, rack) {
        state.setControlTapHandler { control ->
            try {
                when (control) {
                    KeyHookDemos.HangJordan -> finger.hangJordan(rack)
                    KeyHookDemos.TakeAva -> finger.takeAva(rack)
                }
            } finally {
                // Cut short, the script lets go of what it holds.
                if (rack.grabIsScripted) rack.letGo()
            }
        }
        state.setFeedResetHandler {
            finger.hide()
            rack.reset()
        }
        onDispose {
            state.setControlTapHandler(null)
            state.setFeedResetHandler(null)
        }
    }
    // A script the user takes over leaves no fingertip and no keys in its hand.
    LaunchedEffect(state.replay) {
        if (!state.replay) {
            if (rack.grabIsScripted) rack.letGo()
            finger.hide()
        }
    }

    val colors = if (LabTheme.isDark) KeyHookScreenColors.Dark else KeyHookScreenColors.Light
    val who = rack.signedIn
    val touch = LabTheme.colors.touch
    Column(
        Modifier.fillMaxSize().background(colors.background).windowInsetsPadding(demoInsets(state)),
    ) {
        AppBar(colors)
        Column(Modifier.padding(start = 18.dp, end = 18.dp, bottom = 8.dp)) {
            Text(
                if (who != null) stringResource(Res.string.keyhook_welcome, who.first)
                else stringResource(Res.string.keyhook_whos_home),
                Modifier.semantics { heading() },
                color = colors.ink,
                style = GreetingStyle,
            )
            Text(
                stringResource(Res.string.keyhook_household),
                Modifier.padding(top = 3.dp),
                color = colors.muted,
                style = HouseholdStyle,
            )
        }
        // The hero takes what the screen leaves, the same whoever is signed in: the feed below it
        // is one row of shows tall either way.
        Box(Modifier.weight(1f).fillMaxWidth()) {
            AccountCard(who, colors)
            Hint(
                stringResource(
                    if (who != null) Res.string.keyhook_hint_signed_in
                    else Res.string.keyhook_hint_signed_out,
                ),
                colors,
                rack.holders.size,
            )
            val labels =
                rack.rings.map {
                    if (rack.carriedIndex == it.index)
                        stringResource(Res.string.keyhook_sign_out, it.holder.name)
                    else stringResource(Res.string.keyhook_sign_in_as, it.holder.name)
                }
            KeyRack(
                rack,
                labels,
                sounds,
                Modifier.fillMaxSize(),
                onTouch = {
                    if (state.replay) state.endReplay()
                    finger.hide()
                },
            )
            Canvas(Modifier.fillMaxSize()) { with(finger) { drawFinger(touch) } }
        }
        Feed(who, colors, reducedMotion)
        TabBar(colors)
        val status =
            if (who != null) stringResource(Res.string.keyhook_status_in, who.name)
            else stringResource(Res.string.keyhook_status_out)
        Box(
            Modifier.size(1.dp).semantics {
                liveRegion = LiveRegionMode.Polite
                contentDescription = status
            },
        )
    }
}

/** The app's bar: its name and a search button. */
@Composable
private fun AppBar(colors: KeyHookScreenColors) {
    Row(
        Modifier.fillMaxWidth().padding(start = 18.dp, end = 12.dp, top = 16.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            Icon(BrandIcon, colors.accent, 20f)
            Text(stringResource(Res.string.keyhook_brand), color = colors.ink, style = BrandStyle)
        }
        val search = stringResource(Res.string.keyhook_search)
        Box(
            Modifier.size(44.dp).semantics { contentDescription = search },
            contentAlignment = Alignment.Center,
        ) {
            Icon(SearchIcon, colors.ink, 21f)
        }
    }
}

/** Who is signed in, or that nobody is: an avatar and two lines, the D-ring hanging under it. */
@Composable
private fun AccountCard(who: KeyHolder?, colors: KeyHookScreenColors) {
    val shape = RoundedCornerShape(16.dp)
    Row(
        Modifier.padding(horizontal = 14.dp)
            .fillMaxWidth()
            .height(RackGeometry.CardBottom.dp)
            .drawBehind {
                // The prototype's faint 0 1px 2px shadow.
                drawRoundRect(
                    CardShadow,
                    Offset(0f, 1.dp.toPx()),
                    size,
                    CornerRadius(16.dp.toPx()),
                )
            }
            .clip(shape)
            .background(colors.card)
            .drawBehind {
                val w = 1.dp.toPx()
                drawRoundRect(
                    colors.line,
                    Offset(w / 2f, w / 2f),
                    Size(size.width - w, size.height - w),
                    CornerRadius(16.dp.toPx() - w / 2f),
                    style = Stroke(w),
                )
            }
            .padding(horizontal = 14.dp)
            .clearAndSetSemantics {},
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(11.dp),
    ) {
        // The avatar's colour changes with its initials, at once.
        if (who != null) {
            Box(
                Modifier.size(38.dp).clip(CircleShape).background(cssHex(who.color)),
                contentAlignment = Alignment.Center,
            ) {
                Text(who.initials, color = Color.White, style = AvatarStyle)
            }
        } else {
            Box(
                Modifier.size(38.dp).drawBehind {
                    val w = 1.5.dp.toPx()
                    drawCircle(
                        colors.muted,
                        size.minDimension / 2f - w / 2f,
                        style =
                            Stroke(
                                w,
                                pathEffect =
                                    PathEffect.dashPathEffect(floatArrayOf(w * 3f, w * 2f)),
                            ),
                    )
                },
                contentAlignment = Alignment.Center,
            ) {
                Icon(KeyIcon, colors.muted, 18f)
            }
        }
        Column {
            Text(
                stringResource(
                    if (who != null) Res.string.keyhook_signed_in_as else Res.string.keyhook_nobody,
                ),
                color = colors.muted,
                style = WhoLineStyle,
            )
            Text(
                who?.name ?: stringResource(Res.string.keyhook_take_keys),
                color = colors.ink,
                style = WhoNameStyle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/**
 * The hint on the wall, at most half the hero wide: under the card, easing down to midway between
 * the card and the board when the screen leaves a lot of wall, as the prototype places it.
 */
@Composable
private fun Hint(text: String, colors: KeyHookScreenColors, count: Int) {
    val density = LocalDensity.current.density
    Layout(
        content = {
            Text(
                text,
                Modifier.clearAndSetSemantics {},
                color = colors.muted,
                style = HintStyle,
            )
        },
        modifier = Modifier.fillMaxSize(),
    ) { measurables, constraints ->
        val w = constraints.maxWidth
        val h = constraints.maxHeight
        val p =
            measurables
                .single()
                .measure(Constraints(maxWidth = (w * .52f).toInt().coerceAtLeast(0)))
        val geo = RackGeometry(w / density, h / density, count)
        val top = geo.hintTop(p.height / density)
        layout(w, h) { p.place((18f * density).roundToInt(), (top * density).roundToInt()) }
    }
}

/** What the person signed in is watching, or a note that nobody is; one row tall either way. */
@Composable
private fun Feed(who: KeyHolder?, colors: KeyHookScreenColors, reducedMotion: Boolean) {
    // The row fades up whenever it changes, as the prototype's does.
    val shown = remember { Animatable(1f) }
    LaunchedEffect(who) {
        if (reducedMotion) return@LaunchedEffect
        shown.snapTo(0f)
        shown.animateTo(1f, tween(450, easing = CssEase))
    }
    Column(Modifier.padding(start = 18.dp, end = 18.dp, top = 2.dp, bottom = 8.dp)) {
        Text(
            stringResource(Res.string.keyhook_continue_watching),
            Modifier.padding(bottom = 7.dp).semantics { heading() },
            color = colors.ink,
            style = FeedTitleStyle,
        )
        Box(
            Modifier.graphicsLayer {
                alpha = shown.value
                translationY = (1f - shown.value) * 6.dp.toPx()
            },
        ) {
            val shows = who?.let { Household.showsOf(it) }
            // Signed out, two invisible shows hold the row open under the note.
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                for (i in 0..1) {
                    ShowCard(
                        shows?.get(i),
                        colors,
                        Modifier.weight(1f)
                            .then(
                                if (shows == null)
                                    Modifier.graphicsLayer { alpha = 0f }.clearAndSetSemantics {}
                                else Modifier,
                            ),
                    )
                }
            }
            if (shows == null) {
                Box(
                    Modifier.matchParentSize()
                        .drawBehind {
                            val w = 1.5.dp.toPx()
                            drawRoundRect(
                                colors.line,
                                Offset(w / 2f, w / 2f),
                                Size(size.width - w, size.height - w),
                                CornerRadius(12.dp.toPx() - w / 2f),
                                style =
                                    Stroke(
                                        w,
                                        pathEffect =
                                            PathEffect.dashPathEffect(
                                                floatArrayOf(w * 3f, w * 3f),
                                            ),
                                    ),
                            )
                        }
                        .padding(horizontal = 18.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        stringResource(Res.string.keyhook_empty),
                        color = colors.muted,
                        style = EmptyStyle,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

/** One show: its thumbnail, its title on it and how far in, and the episode under it. */
@Composable
private fun ShowCard(show: Show?, colors: KeyHookScreenColors, modifier: Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(
            Modifier.fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(10.dp))
                .drawBehind {
                    if (show == null) return@drawBehind
                    // CSS's 135deg: corner to corner along the line through the middle at 135°.
                    val half = (size.width + size.height) / sqrt(2f) / 2f
                    val d = Offset(half / sqrt(2f), half / sqrt(2f))
                    val c = Offset(size.width / 2f, size.height / 2f)
                    drawRect(
                        Brush.linearGradient(
                            listOf(Color(show.from), Color(show.to)),
                            start = c - d,
                            end = c + d,
                        ),
                    )
                    drawRect(
                        ProgressColor,
                        Offset(0f, size.height - 3.dp.toPx()),
                        Size(size.width * show.progress, 3.dp.toPx()),
                    )
                }
                .padding(horizontal = 8.dp, vertical = 7.dp),
            contentAlignment = Alignment.BottomStart,
        ) {
            if (show != null) {
                val d = LocalDensity.current.density
                val style =
                    remember(d) {
                        ThumbStyle.copy(shadow = Shadow(TitleShadow, Offset(0f, d), 3f * d))
                    }
                Text(show.title, color = Color.White, style = style)
            }
        }
        Text(
            show?.episode ?: " ",
            color = colors.muted,
            style = EpisodeStyle,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** The app's sections, Home showing. */
@Composable
private fun TabBar(colors: KeyHookScreenColors) {
    Row(
        Modifier.fillMaxWidth()
            .drawBehind {
                drawRect(colors.line, size = Size(size.width, 1.dp.toPx()))
            }
            .padding(start = 4.dp, end = 4.dp, top = 6.dp + 1.dp, bottom = 12.dp),
    ) {
        val tabs =
            listOf(
                HomeIcon to Res.string.keyhook_tab_home,
                LiveIcon to Res.string.keyhook_tab_live,
                ListIcon to Res.string.keyhook_tab_list,
                AccountIcon to Res.string.keyhook_tab_account,
            )
        for ((i, tab) in tabs.withIndex()) {
            val on = i == 0
            Column(
                Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Icon(tab.first, if (on) colors.ink else colors.muted, 21f)
                Text(
                    stringResource(tab.second),
                    color = if (on) colors.ink else colors.muted,
                    style = if (on) TabOnStyle else TabStyle,
                )
            }
        }
    }
}

/** One of the prototype's 24-unit SVG icons: each part a path, filled or stroked. */
private class SvgIcon(vararg parts: Pair<String, Float>) {
    // Each part's path, and its stroke width (0 to fill it).
    val parts = parts.map { (d, w) -> PathParser().parsePathString(d).toPath() to w }
}

@Composable
private fun Icon(icon: SvgIcon, color: Color, sizeDp: Float) {
    Canvas(Modifier.size(sizeDp.dp).clearAndSetSemantics {}) {
        scale(size.width / 24f, size.height / 24f, pivot = Offset.Zero) {
            for ((path, w) in icon.parts) {
                drawPath(
                    path,
                    color,
                    style =
                        if (w == 0f) Fill
                        else Stroke(w, cap = StrokeCap.Round, join = StrokeJoin.Round),
                )
            }
        }
    }
}

private val BrandIcon =
    SvgIcon(
        "M8 3h8l-1 5H9z M12 8v3 M6.5 11h11l-1.6 9.2a1 1 0 0 1-1 .8H9.1a1 1 0 0 1-1-.8z" to 1.8f,
    )
private val SearchIcon =
    SvgIcon("M4.5 10.5a6 6 0 1 0 12 0a6 6 0 1 0 -12 0z" to 2f, "m15 15 5 5" to 2f)
private val KeyIcon =
    SvgIcon("M4 12a4 4 0 1 0 8 0a4 4 0 1 0 -8 0z" to 1.8f, "M12 12h9m-3 0v3m-3-3v2" to 1.8f)
private val HomeIcon = SvgIcon("M4 11 12 4l8 7v8a1 1 0 0 1-1 1h-4v-6H9v6H5a1 1 0 0 1-1-1z" to 0f)
private val LiveIcon =
    SvgIcon(
        "M5.5 5h13a2 2 0 0 1 2 2v8a2 2 0 0 1-2 2h-13a2 2 0 0 1-2-2v-8a2 2 0 0 1 2-2z" to 1.8f,
        "M8 20h8" to 1.8f,
    )
private val ListIcon = SvgIcon("M6 4h12v16l-6-4-6 4z" to 1.8f)
private val AccountIcon =
    SvgIcon(
        "M8.4 8.5a3.6 3.6 0 1 0 7.2 0a3.6 3.6 0 1 0 -7.2 0z" to 1.8f,
        "M5 20c1-3.6 3.8-5.4 7-5.4s6 1.8 7 5.4" to 1.8f,
    )

/**
 * The scripted fingertip, which carries keys as the prototype's demo finger does: it glides in the
 * prototype's ease, takes hold of a set where its keys hang and carries it on a gentle curve.
 */
@Stable
private class ScriptFinger {
    private var x by mutableStateOf(0f)
    private var y by mutableStateOf(0f)
    private var alpha by mutableStateOf(0f)

    suspend fun hangJordan(rack: KeyRackState) {
        val g = rack.geometry ?: return
        val jordan = rack.rings[0]
        x = g.width * .55f
        y = g.height * .45f
        fade(.6f)
        to(rack.ringPointX(jordan), rack.ringPointY(jordan), 600f)
        press(rack, jordan)
        delay(180)
        drag(rack, g.hookX(0) + 6f * g.u, g.boardTop + 34f * g.u, 1100f, -28f * g.u)
        release(rack)
    }

    suspend fun takeAva(rack: KeyRackState) {
        val g = rack.geometry ?: return
        val ava = rack.rings[1]
        if (alpha <= 0f) fade(.6f)
        to(rack.ringPointX(ava), rack.ringPointY(ava), 650f)
        press(rack, ava)
        delay(200)
        drag(rack, g.width * .66f, g.cardBottom + 46f * g.u, 1050f, 22f * g.u)
        release(rack)
        delay(700)
        to(g.width * .5f, g.height * .62f, 500f)
        fade(0f)
    }

    private suspend fun press(rack: KeyRackState, ring: KeyRing) {
        rack.startGrab(ring, x, y, scripted = true)
        fade(1f, ScriptedTouch.DownMs.toFloat())
    }

    private suspend fun release(rack: KeyRackState) {
        rack.endGrab(x, y)
        fade(.6f, ScriptedTouch.DownMs.toFloat())
    }

    fun hide() {
        alpha = 0f
    }

    private suspend fun to(tx: Float, ty: Float, ms: Float) {
        val x0 = x
        val y0 = y
        animate(ms) { p ->
            val e = KeyRackState.ease(p)
            x = x0 + (tx - x0) * e
            y = y0 + (ty - y0) * e
        }
    }

    // Carries the keys along a gentle curve, with a little sideways wobble a real hand would add.
    private suspend fun drag(rack: KeyRackState, tx: Float, ty: Float, ms: Float, bend: Float) {
        val x0 = x
        val y0 = y
        animate(ms) { p ->
            val e = KeyRackState.ease(p)
            x = x0 + (tx - x0) * e + sin(p * PI.toFloat()) * bend
            y = y0 + (ty - y0) * e
            rack.moveGrab(x, y)
        }
    }

    private suspend fun fade(to: Float, ms: Float = ScriptedTouch.UpMs.toFloat()) {
        val a0 = alpha
        try {
            animate(ms) { alpha = a0 + (to - a0) * it }
        } finally {
            withContext(NonCancellable) { if (to == 0f) alpha = 0f }
        }
    }

    private suspend fun animate(ms: Float, frame: (Float) -> Unit) {
        val start = withFrameNanos { it }
        var p = 0f
        while (p < 1f) {
            withFrameNanos { now -> p = ((now - start) / 1e6f / ms).coerceIn(0f, 1f) }
            frame(p)
        }
    }

    fun DrawScope.drawFinger(color: Color) {
        val a = alpha
        if (a <= 0f) return
        drawTouch(color, Offset(x * density, y * density), a)
    }
}

private fun cssHex(hex: String): Color = Color(0xFF000000 or hex.removePrefix("#").toLong(16))

private val CardShadow = Color(40, 25, 10).copy(alpha = .06f)
private val ProgressColor = Color(0xFFFFCF6A)
private val CssEase = CubicBezierEasing(.25f, .1f, .25f, 1f)

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

private val BrandStyle = css(16f, FontWeight(750)).copy(letterSpacing = (-.01).em)
private val GreetingStyle = css(23f, FontWeight.Bold, 1.15f).copy(letterSpacing = (-.01).em)
private val HouseholdStyle = css(13f)
private val AvatarStyle = css(14f, FontWeight.Bold)
private val WhoLineStyle = css(11.5f, FontWeight.SemiBold).copy(letterSpacing = .02.em)
private val WhoNameStyle = css(15f, FontWeight(650))
private val HintStyle = css(12.5f, lines = 1.35f).copy(lineBreak = LineBreak.Heading)
private val FeedTitleStyle = css(13f, FontWeight.Bold).copy(letterSpacing = .01.em)
private val ThumbStyle = css(12f, FontWeight.Bold, 1.15f)
private val TitleShadow = Color.Black.copy(alpha = .4f)
private val EpisodeStyle = css(11.5f)
private val EmptyStyle = css(12.5f)
private val TabStyle = css(10.5f)
private val TabOnStyle = css(10.5f, FontWeight(650))
