package dev.dimvlachos.lab.crtpipdemo.presentation.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import dev.dimvlachos.lab.core.demo.DemoState
import dev.dimvlachos.lab.core.platform.rememberReducedMotion
import dev.dimvlachos.lab.core.platform.rememberSoundPlayer
import dev.dimvlachos.lab.core.presentation.components.crtpip.CrtPictureInPicture
import dev.dimvlachos.lab.core.presentation.components.crtpip.CrtPipAnnouncer
import dev.dimvlachos.lab.core.presentation.components.crtpip.CrtPipLabels
import dev.dimvlachos.lab.core.presentation.components.crtpip.CrtPipSounds
import dev.dimvlachos.lab.core.presentation.components.crtpip.CrtPipState
import dev.dimvlachos.lab.core.presentation.components.touch.ScriptedTouch
import dev.dimvlachos.lab.core.presentation.components.touch.drawTouch
import dev.dimvlachos.lab.core.presentation.ui.CrtPipScreenColors
import dev.dimvlachos.lab.core.presentation.ui.LabTheme
import dev.dimvlachos.lab.crtpipdemo.CrtPipDemos
import dev.dimvlachos.lab.resources.Res
import dev.dimvlachos.lab.resources.crtpip_app
import dev.dimvlachos.lab.resources.crtpip_channel
import dev.dimvlachos.lab.resources.crtpip_comment_1
import dev.dimvlachos.lab.resources.crtpip_comment_2
import dev.dimvlachos.lab.resources.crtpip_comment_3
import dev.dimvlachos.lab.resources.crtpip_comments
import dev.dimvlachos.lab.resources.crtpip_expand
import dev.dimvlachos.lab.resources.crtpip_explore
import dev.dimvlachos.lab.resources.crtpip_hint
import dev.dimvlachos.lab.resources.crtpip_home
import dev.dimvlachos.lab.resources.crtpip_library
import dev.dimvlachos.lab.resources.crtpip_meta
import dev.dimvlachos.lab.resources.crtpip_minimize
import dev.dimvlachos.lab.resources.crtpip_next_1
import dev.dimvlachos.lab.resources.crtpip_next_1_meta
import dev.dimvlachos.lab.resources.crtpip_next_2
import dev.dimvlachos.lab.resources.crtpip_next_2_meta
import dev.dimvlachos.lab.resources.crtpip_next_3
import dev.dimvlachos.lab.resources.crtpip_next_3_meta
import dev.dimvlachos.lab.resources.crtpip_next_4
import dev.dimvlachos.lab.resources.crtpip_next_4_meta
import dev.dimvlachos.lab.resources.crtpip_next_5
import dev.dimvlachos.lab.resources.crtpip_next_5_meta
import dev.dimvlachos.lab.resources.crtpip_next_6
import dev.dimvlachos.lab.resources.crtpip_next_6_meta
import dev.dimvlachos.lab.resources.crtpip_now_full
import dev.dimvlachos.lab.resources.crtpip_now_mini
import dev.dimvlachos.lab.resources.crtpip_player
import dev.dimvlachos.lab.resources.crtpip_stop
import dev.dimvlachos.lab.resources.crtpip_stopped
import dev.dimvlachos.lab.resources.crtpip_subscribe
import dev.dimvlachos.lab.resources.crtpip_subscribers
import dev.dimvlachos.lab.resources.crtpip_title
import dev.dimvlachos.lab.resources.crtpip_undo
import dev.dimvlachos.lab.resources.crtpip_up_next
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/**
 * The prototype's cooking-video app, Skillet: a watch page for Marlow Kitchen's honey cornbread,
 * with Up next and comments under the player. Swipe the video down and it switches off into a
 * little rabbit-ear set in the corner, playing on while the page scrolls under it.
 */
@Composable
internal fun CrtPipDemo(
    state: DemoState,
    pip: CrtPipState = remember { CrtPipState() },
    reducedMotion: Boolean = rememberReducedMotion(),
) {
    val dark = LabTheme.isDark
    val colors = if (dark) CrtPipScreenColors.Dark else CrtPipScreenColors.Light
    val sounds = rememberSoundPlayer(CrtPipSounds)
    val scroll = rememberScrollState()
    val finger = remember { PipFinger() }
    val density = LocalDensity.current.density
    SideEffect { finger.density = density }
    val labels =
        CrtPipLabels(
            player = stringResource(Res.string.crtpip_player),
            minimize = stringResource(Res.string.crtpip_minimize),
            hint = stringResource(Res.string.crtpip_hint),
            expand = stringResource(Res.string.crtpip_expand),
            stop = stringResource(Res.string.crtpip_stop),
            stopped = stringResource(Res.string.crtpip_stopped),
            undo = stringResource(Res.string.crtpip_undo),
            nowMini = stringResource(Res.string.crtpip_now_mini),
            nowFull = stringResource(Res.string.crtpip_now_full),
            length = "18:40",
        )

    DisposableEffect(state, pip) {
        state.setControlTapHandler { control ->
            try {
                script(control, pip, finger, scroll)
            } finally {
                // A move cut short lets go of the picture or the set.
                if (finger.pressed) withContext(NonCancellable) { pip.releaseScripted() }
                finger.pressed = false
            }
        }
        state.setFeedResetHandler {
            // Ends as it began: the full player at the top, and the next mini in the usual corner.
            finger.fadeOut()
            pip.reset()
            scroll.scrollTo(0)
        }
        onDispose {
            state.setControlTapHandler(null)
            state.setFeedResetHandler(null)
        }
    }
    // A script the user takes over leaves no fingertip behind, and no drag held.
    LaunchedEffect(state.replay) {
        if (!state.replay) {
            pip.releaseScripted()
            finger.hide()
        }
    }

    val touch = LabTheme.colors.touch
    Box(
        Modifier.fillMaxSize()
            .background(colors.background)
            .windowInsetsPadding(
                if (state.recording) WindowInsets(0)
                else
                    WindowInsets.systemBars
                        .union(WindowInsets.displayCutout)
                        .only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom),
            ),
    ) {
        CrtPictureInPicture(
            state = pip,
            labels = labels,
            colors = colors,
            dark = dark,
            sounds = sounds,
            modifier = Modifier.fillMaxSize(),
            reducedMotion = reducedMotion,
            appBar = { AppBar(colors) },
            page = { WatchPage(colors, scroll) },
            navBar = { NavBar(colors) },
        )
        CrtPipAnnouncer(pip, labels)
        Canvas(Modifier.fillMaxSize()) { with(finger) { drawFinger(touch) } }
    }
}

/** One of the tour's moves, as the prototype's `runDemo()` makes it, in the screen's dp. */
private suspend fun script(
    control: String,
    pip: CrtPipState,
    finger: PipFinger,
    scroll: ScrollState,
) {
    val w = pip.width
    val h = pip.height
    val k = pip.k
    when (control) {
        CrtPipDemos.SwipeDown -> {
            // Drag down slowly enough to see the picture start to give, then let go.
            val y0 = pip.playerY - 20 * k
            finger.appear(Offset(w * .62f, pip.playerY + 150 * k))
            finger.glide(Offset(w * .5f, y0), 520)
            finger.press { pip.playerDown(it.x, it.y, pip.now, scripted = true) }
            delay(140)
            finger.glide(Offset(w * .5f, y0 + 130 * k), 520) { at, _ ->
                pip.playerMove(at.x, at.y, pip.now)
            }
            finger.lift { pip.playerUp(it.x, it.y, pip.now + 1e6) }
        }
        CrtPipDemos.BrowseDown -> {
            // Browse the page while the show plays in the corner.
            finger.glide(Offset(w * .4f, h * .55f), 600)
            delay(900)
            finger.press {}
            val s0 = scroll.value
            val room = min(220 * k * finger.density, (scroll.maxValue - s0).toFloat())
            finger.glide(Offset(w * .4f, h * .55f - 140 * k), 700) { _, e ->
                scroll.dispatchRawDelta(s0 + e * room - scroll.value)
            }
            finger.lift {}
        }
        CrtPipDemos.BrowseUp -> {
            // And back to the top.
            finger.glide(Offset(w * .4f, h * .32f), 300)
            finger.press {}
            val s0 = scroll.value
            finger.glide(Offset(w * .4f, h * .32f + 140 * k), 650) { _, e ->
                scroll.dispatchRawDelta(s0 * (1 - e) - scroll.value)
            }
            finger.lift {}
        }
        CrtPipDemos.SlideSet -> {
            // Slide the set over to the other corner, letting go a little short so the spring
            // shows.
            val m = Offset(pip.setX, pip.setY)
            val tx = pip.cornerX("bl") + (m.x - pip.cabX) + 30 * k
            val ty = pip.cornerY("bl") + (m.y - pip.cabY)
            finger.glide(m, 600)
            finger.press { pip.setDown(it.x, it.y, pip.now, scripted = true) }
            delay(120)
            finger.glide(Offset(tx, ty), 700) { at, _ -> pip.setMove(at.x, at.y, pip.now) }
            finger.lift { pip.setUp(it.x, it.y, pip.now + 1e6) }
        }
        CrtPipDemos.TapSet -> {
            finger.glide(Offset(pip.setX, pip.setY), 450)
            finger.press {}
            delay(ScriptedTouch.TapHoldMs)
            finger.lift { pip.expand() }
        }
    }
}

/** The bar: the Skillet mark and name, search, and the user's avatar. */
@Composable
private fun AppBar(colors: CrtPipScreenColors) {
    Row(
        Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            Modifier.weight(1f).semantics(mergeDescendants = true) { heading() },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            Canvas(Modifier.size(24.dp)) { drawSkilletMark() }
            BasicText(
                stringResource(Res.string.crtpip_app),
                style = text(colors.ink, 19.sp, 800, letterSpacing = (-.02).em),
            )
        }
        Canvas(Modifier.size(22.dp).clearAndSetSemantics {}) {
            val s = size.width / 24f
            drawCircle(colors.muted, 6.5f * s, Offset(11 * s, 11 * s), style = Stroke(2 * s))
            drawLine(
                colors.muted,
                Offset(16 * s, 16 * s),
                Offset(20 * s, 20 * s),
                2 * s,
                StrokeCap.Round,
            )
        }
        Avatar("DL", CrtPipScreenColors.Avatar, 30.dp, 12.sp)
    }
}

/** The logo: a little cast-iron pan with tomato sauce in it. */
private fun DrawScope.drawSkilletMark() {
    val s = size.width / 24f
    drawOval(LogoIron, Offset(1.5f * s, 6.5f * s), Size(17 * s, 13 * s))
    drawOval(LogoSauce, Offset(3.4f * s, 7.6f * s), Size(13.2f * s, 9.6f * s))
    drawLine(
        LogoIron,
        Offset(17.5f * s, 11.2f * s),
        Offset(23 * s, 8.6f * s),
        2.6f * s,
        StrokeCap.Round,
    )
}

private val LogoIron = Color(0xFF2B2724)
private val LogoSauce = Color(0xFFE8402A)

@Composable
private fun Avatar(initials: String, color: Color, size: Dp, fontSize: TextUnit) {
    Box(
        Modifier.size(size).clip(CircleShape).background(color).clearAndSetSemantics {},
        contentAlignment = Alignment.Center,
    ) {
        BasicText(initials, style = text(Color.White, fontSize, 700))
    }
}

private class UpNext(
    val title: StringResource,
    val meta: StringResource,
    val length: String,
    val thumb: Thumb,
)

private class Comment(
    val initials: String,
    val color: Color,
    val handle: String,
    val text: StringResource,
)

private val Next =
    listOf(
        UpNext(
            Res.string.crtpip_next_1,
            Res.string.crtpip_next_1_meta,
            "12:04",
            Thumb(
                Linear(135f, "#2d2a27", "#5b4a3a"),
                Ring(
                    .58f,
                    .56f,
                    0f to "#7a3f22",
                    .15f to "#7a3f22",
                    .16f to "#e9b44c",
                    .2f to "#e9b44c",
                    .21f to "#f2e6cf",
                    .31f to "#f2e6cf",
                    .32f to null,
                ),
            ),
        ),
        UpNext(
            Res.string.crtpip_next_2,
            Res.string.crtpip_next_2_meta,
            "9:31",
            Thumb(
                Linear(135f, "#a7744a", "#6e4a2e"),
                Ring(.64f, .48f, 0f to "#e3b46a", .13f to "#e3b46a", .14f to null),
                Ring(.40f, .52f, 0f to "#e9c27f", .14f to "#e9c27f", .15f to null),
            ),
        ),
        UpNext(
            Res.string.crtpip_next_3,
            Res.string.crtpip_next_3_meta,
            "21:17",
            Thumb(
                Linear(135f, "#3c4d5c", "#22303b"),
                Ring(
                    .52f,
                    .56f,
                    0f to "#8e2a1b",
                    .22f to "#8e2a1b",
                    .23f to "#f0ebe2",
                    .3f to "#f0ebe2",
                    .31f to null,
                ),
            ),
        ),
        UpNext(
            Res.string.crtpip_next_4,
            Res.string.crtpip_next_4_meta,
            "14:50",
            Thumb(
                Linear(135f, "#5e7f74", "#34504a"),
                Ring(
                    .5f,
                    .54f,
                    0f to "#f1d68a",
                    .18f to "#f1d68a",
                    .19f to "#f6a168",
                    .22f to "#f6a168",
                    .23f to "#f6f0e4",
                    .31f to "#f6f0e4",
                    .32f to null,
                ),
            ),
        ),
        UpNext(
            Res.string.crtpip_next_5,
            Res.string.crtpip_next_5_meta,
            "17:02",
            Thumb(
                Linear(135f, "#e8dccb", "#c9b79d"),
                Ring(
                    .5f,
                    .55f,
                    0f to "#6b1f2c",
                    .16f to "#6b1f2c",
                    .17f to "#d79a52",
                    .27f to "#d79a52",
                    .28f to null,
                ),
            ),
        ),
        UpNext(
            Res.string.crtpip_next_6,
            Res.string.crtpip_next_6_meta,
            "8:45",
            Thumb(
                Linear(160f, "#8cb2c9", "#4d6e58", end = .7f),
                Ring(
                    .46f,
                    .58f,
                    0f to "#c88a3e",
                    .17f to "#c88a3e",
                    .18f to "#3a3633",
                    .26f to "#3a3633",
                    .27f to null,
                ),
            ),
        ),
    )

private val Comments =
    listOf(
        Comment("BK", Color(0xFF7A5BB0), "@bethanyk", Res.string.crtpip_comment_1),
        Comment("JR", Color(0xFF3D7F8F), "@jroddallas", Res.string.crtpip_comment_2),
        Comment("MO", Color(0xFFB0663B), "@mike.in.ohio", Res.string.crtpip_comment_3),
    )

/**
 * Under the player: the video's title and channel, Up next and the comments, in the prototype's 12
 * dp grid with 170 dp at the foot so the last rows scroll out from under the set.
 */
@Composable
private fun WatchPage(colors: CrtPipScreenColors, scroll: ScrollState) {
    Column(
        Modifier.fillMaxSize()
            .verticalScroll(scroll)
            .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 170.dp)
            .testTag("crt-page"),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column {
            BasicText(
                stringResource(Res.string.crtpip_title),
                Modifier.semantics { heading() },
                style = text(colors.ink, 17.5.sp, 700, 1.22.em, (-.01).em),
            )
            BasicText(
                stringResource(Res.string.crtpip_meta),
                Modifier.padding(top = 4.dp),
                style = text(colors.muted, 12.5.sp),
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Avatar("MK", CrtPipScreenColors.ChannelAvatar, 34.dp, 12.5.sp)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                BasicText(
                    stringResource(Res.string.crtpip_channel),
                    style = text(colors.ink, 14.sp, 700),
                )
                // An inline line in the body's 15 px line box, 23.25 px tall.
                BasicText(
                    stringResource(Res.string.crtpip_subscribers),
                    style = text(colors.muted, 12.sp, lineHeight = 1.9375.em),
                )
            }
            Box(
                Modifier.clip(RoundedCornerShape(999.dp))
                    .background(colors.ink)
                    .padding(horizontal = 14.dp, vertical = 8.dp),
            ) {
                BasicText(
                    stringResource(Res.string.crtpip_subscribe),
                    style = text(colors.background, 13.sp, 650, lineHeight = 1.2.em),
                )
            }
        }
        // The prototype's chips row is squeezed to nothing by its grid, so the approved page shows
        // none; its two gaps stay.
        Spacer(Modifier.height(0.dp))
        BasicText(
            stringResource(Res.string.crtpip_up_next),
            Modifier.padding(top = 6.dp).semantics { heading() },
            style = text(colors.ink, 15.sp, 700),
        )
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            for (n in Next) NextRow(n, colors)
        }
        BasicText(
            stringResource(Res.string.crtpip_comments),
            Modifier.padding(top = 6.dp).semantics { heading() },
            style = text(colors.ink, 15.sp, 700),
        )
        for (c in Comments) CommentRow(c, colors)
    }
}

@Composable
private fun NextRow(n: UpNext, colors: CrtPipScreenColors) {
    Row(
        Modifier.semantics(mergeDescendants = true) {},
        horizontalArrangement = Arrangement.spacedBy(11.dp),
    ) {
        Box(
            Modifier.size(128.dp, 72.dp).clip(RoundedCornerShape(10.dp)).drawWithCache {
                val layers = n.thumb.brushes(size)
                onDrawBehind { for (b in layers) drawRect(b) }
            },
        ) {
            Box(
                Modifier.align(Alignment.BottomEnd)
                    .padding(end = 5.dp, bottom = 5.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0f, 0f, 0f, .78f))
                    .padding(horizontal = 5.dp, vertical = 1.dp),
            ) {
                BasicText(
                    n.length,
                    style =
                        text(Color.White, 10.5.sp, 600, lineHeight = 1.2.em)
                            .copy(fontFamily = FontFamily.Monospace),
                )
            }
        }
        Column(Modifier.weight(1f)) {
            BasicText(
                stringResource(n.title),
                style = text(colors.ink, 13.5.sp, 650, 1.25.em),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            BasicText(
                stringResource(n.meta),
                Modifier.padding(top = 3.dp),
                style = text(colors.muted, 12.sp),
            )
        }
    }
}

@Composable
private fun CommentRow(c: Comment, colors: CrtPipScreenColors) {
    Row(
        Modifier.semantics(mergeDescendants = true) {},
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Avatar(c.initials, c.color, 28.dp, 11.sp)
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            BasicText(c.handle, style = text(colors.muted, 12.sp, 600))
            BasicText(stringResource(c.text), style = text(colors.ink, 13.sp))
        }
    }
}

/** The tab bar: Home, Explore and Library, over a hairline; decoration, hidden from readers. */
@Composable
private fun NavBar(colors: CrtPipScreenColors) {
    Row(
        Modifier.fillMaxWidth()
            .background(colors.background)
            .drawBehind { drawRect(colors.line, size = Size(size.width, 1.dp.toPx())) }
            .padding(start = 8.dp, end = 8.dp, top = 9.dp, bottom = 12.dp)
            .clearAndSetSemantics {},
        horizontalArrangement = Arrangement.SpaceAround,
    ) {
        NavItem(stringResource(Res.string.crtpip_home), colors.ink) { c -> drawHome(c) }
        NavItem(stringResource(Res.string.crtpip_explore), colors.muted) { c -> drawExplore(c) }
        NavItem(stringResource(Res.string.crtpip_library), colors.muted) { c -> drawLibrary(c) }
    }
}

@Composable
private fun NavItem(label: String, color: Color, icon: DrawScope.(Color) -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Canvas(Modifier.size(22.dp)) { icon(color) }
        BasicText(label, style = text(color, 11.sp, 600))
    }
}

private fun DrawScope.drawHome(c: Color) {
    val s = size.width / 24f
    val p =
        Path().apply {
            moveTo(4 * s, 11 * s)
            lineTo(12 * s, 4 * s)
            lineTo(20 * s, 11 * s)
            lineTo(20 * s, 20 * s)
            lineTo(15 * s, 20 * s)
            lineTo(15 * s, 14 * s)
            lineTo(9 * s, 14 * s)
            lineTo(9 * s, 20 * s)
            lineTo(4 * s, 20 * s)
            close()
        }
    drawPath(p, c)
}

private fun DrawScope.drawExplore(c: Color) {
    val s = size.width / 24f
    drawCircle(c, 8 * s, Offset(12 * s, 12 * s), style = Stroke(2 * s))
    val p =
        Path().apply {
            moveTo(15.5f * s, 8.5f * s)
            lineTo(13.5f * s, 13.5f * s)
            lineTo(8.5f * s, 15.5f * s)
            lineTo(10.5f * s, 10.5f * s)
            close()
        }
    drawPath(p, c)
}

private fun DrawScope.drawLibrary(c: Color) {
    val s = size.width / 24f
    drawRoundRect(
        c,
        Offset(4 * s, 5 * s),
        Size(16 * s, 14 * s),
        androidx.compose.ui.geometry.CornerRadius(3 * s),
        style = Stroke(2 * s),
    )
    val p =
        Path().apply {
            moveTo(10 * s, 9 * s)
            lineTo(15 * s, 12 * s)
            lineTo(10 * s, 15 * s)
            close()
        }
    drawPath(p, c)
}

/** A thumbnail's CSS background: a linear gradient, with radial rings with hard stops over it. */
private class Thumb(val base: Linear, vararg val rings: Ring) {
    fun brushes(size: Size): List<Brush> = listOf(base.brush(size)) + rings.map { it.brush(size) }
}

/** `linear-gradient(<angle>deg, from, to <end>)`, as the browser lays its line out. */
private class Linear(val degrees: Float, val from: String, val to: String, val end: Float = 1f) {
    fun brush(size: Size): Brush {
        val a = degrees * kotlin.math.PI.toFloat() / 180f
        val dx = kotlin.math.sin(a)
        val dy = -kotlin.math.cos(a)
        val len = kotlin.math.abs(size.width * dx) + kotlin.math.abs(size.height * dy)
        val c = Offset(size.width / 2, size.height / 2)
        val start = c - Offset(dx, dy) * (len / 2)
        val stop = c + Offset(dx, dy) * (len / 2)
        return Brush.linearGradient(
            0f to css(from),
            end to css(to),
            start = start,
            end = stop,
        )
    }
}

/**
 * `radial-gradient(circle at x y, …)`: stops as fractions of the distance to the farthest corner, a
 * null colour being the transparent one that ends it (taken as the last colour, clear, as the
 * browser's premultiplied fade is).
 */
private class Ring(val x: Float, val y: Float, vararg val stops: Pair<Float, String?>) {
    fun brush(size: Size): Brush {
        val cx = size.width * x
        val cy = size.height * y
        val r = max(hypot(max(cx, size.width - cx), max(cy, size.height - cy)), 1f)
        var last = Color.Transparent
        val colorStops =
            stops
                .map { (at, c) ->
                    val color = if (c == null) last.copy(alpha = 0f) else css(c).also { last = it }
                    at to color
                }
                .toTypedArray()
        return Brush.radialGradient(*colorStops, center = Offset(cx, cy), radius = r)
    }
}

private fun css(hex: String): Color = Color(("FF" + hex.removePrefix("#")).toLong(16))

/** CSS line boxes: the leading split above and below every line, the first and last included. */
private val CssLines = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.None)

/** The screen's text: the system UI font, at the prototype's body line height of 1.55. */
private fun text(
    color: Color,
    size: TextUnit,
    weight: Int = 400,
    lineHeight: TextUnit = 1.55.em,
    letterSpacing: TextUnit = TextUnit.Unspecified,
) =
    TextStyle(
        color = color,
        fontSize = size,
        fontWeight = FontWeight(weight),
        lineHeight = lineHeight,
        letterSpacing = letterSpacing,
        lineHeightStyle = CssLines,
    )

/**
 * The scripted fingertip, in the screen's dp: it glides between points on the prototype's ease,
 * comes down and lifts, and works the player and the set through the same calls a finger does.
 */
@Stable
private class PipFinger {
    private val alpha = Animatable(0f)
    private val down = Animatable(0f)
    private var at by mutableStateOf(Offset.Zero)
    private var placed = false
    var pressed = false

    /** The screen's density, for scrolling in pixels. */
    var density = 1f

    suspend fun appear(from: Offset) {
        if (!placed) {
            at = from
            placed = true
        }
        if (alpha.value < 1f) alpha.animateTo(1f, tween(200))
    }

    /** Glides to [to] over [ms]; [each] hears every step with how far along it is, eased. */
    suspend fun glide(to: Offset, ms: Int, each: (Offset, Float) -> Unit = { _, _ -> }) {
        val from = at
        val p = Animatable(0f)
        p.animateTo(1f, tween(ms, easing = PrototypeEase)) {
            at = Offset(from.x + (to.x - from.x) * value, from.y + (to.y - from.y) * value)
            each(at, value)
        }
    }

    suspend fun press(action: (Offset) -> Unit) {
        pressed = true
        action(at)
        down.animateTo(1f, tween(ScriptedTouch.DownMs))
    }

    suspend fun lift(action: (Offset) -> Unit) {
        pressed = false
        action(at)
        down.animateTo(0f, tween(ScriptedTouch.DownMs))
    }

    suspend fun fadeOut() {
        alpha.animateTo(0f, tween(ScriptedTouch.UpMs))
        placed = false
    }

    suspend fun hide() {
        placed = false
        pressed = false
        alpha.snapTo(0f)
        down.snapTo(0f)
    }

    fun DrawScope.drawFinger(color: Color) {
        val a = alpha.value
        if (a <= 0f) return
        drawTouch(color, at * this.density, a * (.6f + .4f * down.value))
    }
}

/** The prototype's ease: quadratic in, quadratic out. */
private val PrototypeEase = Easing { CrtPipState.ease(it) }
