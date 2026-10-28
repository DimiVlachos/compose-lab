package dev.dimvlachos.lab.spiderdemo.presentation.components

import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import dev.dimvlachos.lab.core.demo.DemoState
import dev.dimvlachos.lab.core.demo.demoInsets
import dev.dimvlachos.lab.core.platform.rememberReducedMotion
import dev.dimvlachos.lab.core.platform.rememberSoundPlayer
import dev.dimvlachos.lab.core.presentation.canvas2d.setBlur
import dev.dimvlachos.lab.core.presentation.components.spider.SpiderPhase
import dev.dimvlachos.lab.core.presentation.components.spider.SpiderRefreshEffects
import dev.dimvlachos.lab.core.presentation.components.spider.SpiderRefreshFeed
import dev.dimvlachos.lab.core.presentation.components.spider.SpiderRefreshSounds
import dev.dimvlachos.lab.core.presentation.components.spider.SpiderRefreshState
import dev.dimvlachos.lab.core.presentation.components.touch.drawTouch
import dev.dimvlachos.lab.core.presentation.ui.LabTheme
import dev.dimvlachos.lab.core.presentation.ui.SpiderRefreshScreenColors
import dev.dimvlachos.lab.resources.Res
import dev.dimvlachos.lab.resources.spider_caught_up
import dev.dimvlachos.lab.resources.spider_chip_events
import dev.dimvlachos.lab.resources.spider_chip_lost
import dev.dimvlachos.lab.resources.spider_chip_nearby
import dev.dimvlachos.lab.resources.spider_chip_sale
import dev.dimvlachos.lab.resources.spider_feed
import dev.dimvlachos.lab.resources.spider_like
import dev.dimvlachos.lab.resources.spider_new
import dev.dimvlachos.lab.resources.spider_place
import dev.dimvlachos.lab.resources.spider_place_meta
import dev.dimvlachos.lab.resources.spider_refresh
import dev.dimvlachos.lab.resources.spider_reply
import dev.dimvlachos.lab.resources.spider_said_new
import dev.dimvlachos.lab.resources.spider_said_none
import dev.dimvlachos.lab.resources.spider_tab_events
import dev.dimvlachos.lab.resources.spider_tab_home
import dev.dimvlachos.lab.resources.spider_tab_me
import dev.dimvlachos.lab.resources.spider_tab_messages
import dev.dimvlachos.lab.resources.spider_tab_post
import dev.dimvlachos.lab.spiderdemo.SpiderHand
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/**
 * The prototype's Elm Hollow neighborhood feed. Pull it down and the spider lowers itself on its
 * silk; the app bar's refresh button runs the same sequence. The script's pulls go through the same
 * calls a finger makes.
 */
@Composable
internal fun SpiderRefreshDemo(
    state: DemoState,
    spider: SpiderRefreshState = remember { SpiderRefreshState() },
    reducedMotion: Boolean = rememberReducedMotion(),
) {
    val sounds = rememberSoundPlayer(SpiderRefreshSounds)
    SideEffect { spider.reducedMotion = reducedMotion }
    SpiderRefreshEffects(spider, sounds)
    val density = LocalDensity.current.density
    // Where the screen, the feed and the refresh button are, read when the hand needs them.
    val places = remember { arrayOfNulls<LayoutCoordinates>(3) }
    fun placeOf(i: Int, centre: Boolean): Offset? {
        val root = places[0]?.takeIf { it.isAttached } ?: return null
        val p = places[i]?.takeIf { it.isAttached } ?: return null
        val at = if (centre) Offset(p.size.width / 2f, p.size.height / 2f) else Offset.Zero
        return root.localPositionOf(p, at) / density
    }
    val hand =
        remember(spider) {
            SpiderHand(spider, feed = { placeOf(1, false) }, button = { placeOf(2, true) })
        }
    var fade by remember { mutableFloatStateOf(1f) }
    // The script's steps take turns, as the prototype awaits each: on a slow device a pull that
    // runs late holds the next step back rather than running alongside it.
    val turn = remember { Mutex() }
    DisposableEffect(state, hand) {
        suspend fun step(block: suspend () -> Unit) {
            try {
                turn.withLock { block() }
            } catch (e: CancellationException) {
                hand.stop()
                fade = 1f
                throw e
            }
        }
        state.setControlTapHandler { control -> step { hand.play(control) } }
        // Back to the first five posts, so the clip loops: the feed fades out, is reset, and fades
        // back in, once the spider is home, however late it ran.
        state.setFeedResetHandler {
            step {
                while (spider.phase != SpiderPhase.Idle) withFrameNanos {}
                if (!spider.reducedMotion) fadeTo(1f, 0f) { fade = it }
                spider.reset()
                if (!spider.reducedMotion) fadeTo(0f, 1f) { fade = it }
                fade = 1f
            }
        }
        onDispose {
            state.setControlTapHandler(null)
            state.setFeedResetHandler(null)
        }
    }
    // A script the user takes over leaves no fingertip and no pull behind.
    LaunchedEffect(state.replay) { if (!state.replay) hand.stop() }
    val takeOver = {
        hand.stop()
        fade = 1f
        if (state.replay) state.endReplay()
    }

    val colors =
        if (LabTheme.isDark) SpiderRefreshScreenColors.Dark else SpiderRefreshScreenColors.Light
    val touch = LabTheme.colors.touch
    Box(
        Modifier.fillMaxSize().background(colors.background).onGloballyPositioned {
            places[0] = it
        },
    ) {
        Column(Modifier.fillMaxSize().windowInsetsPadding(demoInsets(state))) {
            AppBar(
                colors,
                busy = spider.busy,
                onRefresh = {
                    takeOver()
                    spider.refreshButton()
                },
                onButton = { places[2] = it },
            )
            Chips(colors)
            SpiderRefreshFeed(
                spider,
                colors,
                colors.accent,
                stringResource(Res.string.spider_feed),
                Modifier.weight(1f)
                    .fillMaxWidth()
                    .graphicsLayer { alpha = fade }
                    .onGloballyPositioned { places[1] = it },
                onTouch = takeOver,
            ) { id ->
                Post(id, fresh = id in spider.fresh, colors)
            }
            Tabs(colors)
        }
        Toast(
            spider.toast,
            reducedMotion,
            Modifier.align(Alignment.BottomCenter)
                .windowInsetsPadding(demoInsets(state))
                .padding(bottom = 84.dp),
        )
        // A polite live region says how many posts arrived.
        val said =
            when (val n = spider.said) {
                -1 -> ""
                0 -> stringResource(Res.string.spider_said_none)
                else -> pluralStringResource(Res.plurals.spider_said_new, n, n)
            }
        Box(
            Modifier.size(1.dp).testTag("live").semantics {
                liveRegion = LiveRegionMode.Polite
                contentDescription = said
            },
        )
        Canvas(Modifier.fillMaxSize()) {
            val a = hand.shown
            if (a <= 0f) return@Canvas
            val at = hand.at * density
            val k = if (hand.down) .82f else 1f
            scale(k, k, pivot = at) { drawTouch(touch, at, a) }
        }
    }
}

/** Eases a value from [from] to [to] over the feed's fade, a soft in-and-out. */
private suspend fun fadeTo(from: Float, to: Float, set: (Float) -> Unit) {
    val start = withFrameNanos { it }
    var p = 0f
    while (p < 1f) {
        withFrameNanos { t -> p = ((t - start) / 1e6f / FadeMs).coerceIn(0f, 1f) }
        set(from + (to - from) * SpiderRefreshState.ease(p))
    }
}

/** The app bar: the neighborhood, its count, the refresh button and the bell. */
@Composable
private fun AppBar(
    colors: SpiderRefreshScreenColors,
    busy: Boolean,
    onRefresh: () -> Unit,
    onButton: (LayoutCoordinates) -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().padding(start = 18.dp, end = 10.dp, top = 6.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Column(Modifier.weight(1f)) {
            BasicText(
                stringResource(Res.string.spider_place),
                Modifier.semantics { heading() },
                style =
                    css(22f, FontWeight.Bold, 1.15f)
                        .copy(color = colors.ink, letterSpacing = (-.01).em),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            BasicText(
                stringResource(Res.string.spider_place_meta),
                style = css(12.5f).copy(color = colors.muted),
                maxLines = 1,
            )
        }
        val label = stringResource(Res.string.spider_refresh)
        Box(
            Modifier.size(40.dp)
                .onGloballyPositioned(onButton)
                .clip(CircleShape)
                .clickable(
                    enabled = !busy,
                    role = Role.Button,
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onRefresh,
                )
                .semantics { contentDescription = label }
                .testTag("refresh")
                .alpha(if (busy) .45f else 1f),
            contentAlignment = Alignment.Center,
        ) {
            Icon(RefreshIcon, colors.ink, 1.9f)
        }
        Box(Modifier.size(40.dp).clearAndSetSemantics {}, contentAlignment = Alignment.Center) {
            Icon(BellIcon, colors.ink, 1.9f)
        }
    }
}

/** The prototype's 22 dp stroked icons, from their 24-unit SVG paths. */
@Composable
private fun Icon(paths: List<Path>, color: Color, width: Float, size: Float = 22f) {
    Canvas(Modifier.size(size.dp)) { strokeIcon(paths, color, width) }
}

private fun DrawScope.strokeIcon(paths: List<Path>, color: Color, width: Float) {
    val s = size.width / 24f
    scale(s, s, pivot = Offset.Zero) {
        val stroke = Stroke(width, cap = StrokeCap.Round, join = StrokeJoin.Round)
        for (p in paths) drawPath(p, color, style = stroke)
    }
}

@Composable
private fun Chips(colors: SpiderRefreshScreenColors) {
    Row(
        Modifier.fillMaxWidth()
            .drawBehind {
                val w = 1.dp.toPx()
                drawRect(colors.line, Offset(0f, size.height - w), size.copy(height = w))
            }
            .padding(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 10.dp)
            .clearAndSetSemantics {},
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        val chips =
            listOf(
                Res.string.spider_chip_nearby,
                Res.string.spider_chip_events,
                Res.string.spider_chip_lost,
                Res.string.spider_chip_sale,
            )
        chips.forEachIndexed { i, chip ->
            val on = i == 0
            BasicText(
                stringResource(chip),
                Modifier.clip(RoundedCornerShape(999.dp))
                    .background(if (on) colors.ink else colors.chip)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                style =
                    css(13f, FontWeight.Medium, 1.2f)
                        .copy(color = if (on) colors.background else colors.ink),
                maxLines = 1,
                softWrap = false,
            )
        }
    }
}

@Composable
private fun Tabs(colors: SpiderRefreshScreenColors) {
    Row(
        Modifier.fillMaxWidth()
            .drawBehind { drawRect(colors.line, size = size.copy(height = 1.dp.toPx())) }
            .padding(start = 6.dp, end = 6.dp, top = 8.dp, bottom = 14.dp)
            .clearAndSetSemantics {},
    ) {
        val tabs =
            listOf(
                Res.string.spider_tab_home to HomeIcon,
                Res.string.spider_tab_events to EventsIcon,
                Res.string.spider_tab_post to PostIcon,
                Res.string.spider_tab_messages to MessagesIcon,
                Res.string.spider_tab_me to MeIcon,
            )
        tabs.forEachIndexed { i, (label, icon) ->
            val on = i == 0
            val tint = if (on) colors.ink else colors.muted
            Column(
                Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Icon(icon, tint, 1.8f)
                BasicText(
                    stringResource(label),
                    style =
                        css(11f, if (on) FontWeight.SemiBold else FontWeight.Normal, 1.2f)
                            .copy(color = tint),
                    maxLines = 1,
                )
            }
        }
    }
}

/** "You're all caught up", fading in and out as the prototype's `.toast` over .5 s. */
@Composable
private fun Toast(on: Boolean, reducedMotion: Boolean, modifier: Modifier) {
    // Read in the layer, so the fade never recomposes.
    val a =
        animateFloatAsState(
            if (on) 1f else 0f,
            tween(if (reducedMotion) 0 else 500, easing = EaseInOut),
            label = "toast",
        )
    BasicText(
        stringResource(Res.string.spider_caught_up),
        modifier
            .graphicsLayer { alpha = a.value }
            .clip(RoundedCornerShape(999.dp))
            .background(SpiderRefreshScreenColors.Toast)
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .clearAndSetSemantics {},
        style = css(13.5f, lines = 1.2f).copy(color = SpiderRefreshScreenColors.ToastInk),
        maxLines = 1,
    )
}

/**
 * One neighbor's post, as the prototype's `makePost()`: who, where and when, the text, and likes.
 */
@Composable
private fun Post(id: Int, fresh: Boolean, colors: SpiderRefreshScreenColors) {
    val post = SpiderPosts[id]
    val shadow = remember { Paint() }
    Column(
        Modifier.fillMaxWidth()
            .drawBehind { drawCard(colors, shadow) }
            .padding(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 10.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            val (from, to) = SpiderRefreshScreenColors.Avatars[id]
            Box(
                Modifier.size(36.dp)
                    .clip(CircleShape)
                    .drawBehind { drawGradient(from, to) }
                    .clearAndSetSemantics {},
                contentAlignment = Alignment.Center,
            ) {
                BasicText(
                    post.initials,
                    style =
                        css(13f, FontWeight(650), 1.2f)
                            .copy(color = Color.White, letterSpacing = .02.em),
                )
            }
            Column {
                BasicText(
                    stringResource(post.who),
                    style = css(14f, FontWeight.SemiBold, 1.25f).copy(color = colors.ink),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    BasicText(
                        stringResource(post.meta),
                        style = css(12f).copy(color = colors.muted),
                        maxLines = 1,
                    )
                    if (fresh) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Box(Modifier.size(6.dp).clip(CircleShape).background(colors.accent))
                            BasicText(
                                stringResource(Res.string.spider_new),
                                style = css(12f, FontWeight.SemiBold).copy(color = colors.accent),
                                maxLines = 1,
                            )
                        }
                    }
                }
            }
        }
        BasicText(
            stringResource(post.text),
            Modifier.padding(top = 8.dp, bottom = 9.dp),
            style = css(14f, lines = 1.42f).copy(color = colors.ink),
        )
        Row(
            Modifier.clearAndSetSemantics {},
            horizontalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Action(
                HeartIcon,
                if (post.likes > 0) "${post.likes}" else stringResource(Res.string.spider_like),
                colors,
            )
            Action(
                TalkIcon,
                if (post.talk > 0) "${post.talk}" else stringResource(Res.string.spider_reply),
                colors,
            )
        }
    }
}

@Composable
private fun Action(icon: List<Path>, label: String, colors: SpiderRefreshScreenColors) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Canvas(Modifier.size(15.dp)) {
            // The action icons are drawn in a 16-unit box.
            val s = size.width / 16f
            scale(s, s, pivot = Offset.Zero) {
                val stroke = Stroke(1.7f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                for (p in icon) drawPath(p, colors.muted, style = stroke)
            }
        }
        BasicText(label, style = css(12.5f, lines = 1.2f).copy(color = colors.muted), maxLines = 1)
    }
}

/**
 * The card: its two box shadows (CSS's `0 1px 2px` and `0 4px 14px -8px`, blur twice the sigma and
 * the spread an inset), its fill, and the 1 dp inset edge.
 */
private fun DrawScope.drawCard(colors: SpiderRefreshScreenColors, shadow: Paint) {
    val r = 16.dp.toPx()
    fun boxShadow(oy: Float, blur: Float, spread: Float, color: Color) {
        if (color.alpha <= 0f) return
        shadow.color = color
        shadow.setBlur(blur.dp.toPx() / 2)
        val s = spread.dp.toPx()
        drawIntoCanvas {
            it.drawRoundRect(
                -s,
                oy.dp.toPx() - s,
                size.width + s,
                size.height + oy.dp.toPx() + s,
                r + s,
                r + s,
                shadow,
            )
        }
    }
    boxShadow(1f, 2f, 0f, colors.shadowNear)
    boxShadow(4f, 14f, -8f, colors.shadowFar)
    drawRoundRect(colors.card, cornerRadius = androidx.compose.ui.geometry.CornerRadius(r))
    val w = 1.dp.toPx()
    drawRoundRect(
        colors.cardEdge,
        Offset(w / 2, w / 2),
        size.copy(width = size.width - w, height = size.height - w),
        androidx.compose.ui.geometry.CornerRadius(r - w / 2),
        style = Stroke(w),
    )
}

/** CSS's `linear-gradient(140deg, from, to)` across the avatar. */
private fun DrawScope.drawGradient(from: Color, to: Color) {
    val a = 140f * PI.toFloat() / 180f
    val dx = sin(a)
    val dy = -cos(a)
    val len = abs(size.width * dx) + abs(size.height * dy)
    val c = Offset(size.width / 2f, size.height / 2f)
    val half = Offset(dx * len / 2f, dy * len / 2f)
    drawRect(Brush.linearGradient(listOf(from, to), start = c - half, end = c + half))
}

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

private fun svg(vararg d: String): List<Path> = d.map { PathParser().parsePathString(it).toPath() }

private val RefreshIcon by lazy { svg("M19.5 12a7.5 7.5 0 1 1-2.2-5.3", "M19.5 4.5v4h-4") }
private val BellIcon by lazy {
    svg("M6 16.5V11a6 6 0 0 1 12 0v5.5l1.5 2h-15z", "M10 20.5a2 2 0 0 0 4 0")
}
private val HomeIcon by lazy { svg("M4 11l8-6.5 8 6.5V20h-5v-5.5h-6V20H4z") }
private val EventsIcon by lazy {
    svg(
        "M6 5.5h12a2 2 0 0 1 2 2v10a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2v-10a2 2 0 0 1 2-2z",
        "M4 10h16M9 3.5v4M15 3.5v4",
    )
}
private val PostIcon by lazy {
    svg("M12 3.5a8.5 8.5 0 1 1 0 17a8.5 8.5 0 1 1 0-17z", "M12 8v8M8 12h8")
}
private val MessagesIcon by lazy { svg("M4.5 6.5h15v10h-9l-4 3v-3h-2z") }
private val MeIcon by lazy {
    svg(
        "M12 5.4a3.6 3.6 0 1 1 0 7.2a3.6 3.6 0 1 1 0-7.2z",
        "M5 20c1.2-3.6 4-5.2 7-5.2s5.8 1.6 7 5.2",
    )
}
private val HeartIcon by lazy {
    svg("M8 13.5S2 10 2 6.2A3 3 0 0 1 8 4.8a3 3 0 0 1 6 1.4C14 10 8 13.5 8 13.5z")
}
private val TalkIcon by lazy { svg("M2.5 3.5h11v7.5h-6.5l-3 2.5V11h-1.5z") }

/** How long the feed takes to fade out, and back in, when the script resets it. */
private const val FadeMs = 600
