package dev.dimvlachos.lab.jackolanterndemo.presentation.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import dev.dimvlachos.lab.core.demo.DemoState
import dev.dimvlachos.lab.core.demo.demoInsets
import dev.dimvlachos.lab.core.platform.rememberReducedMotion
import dev.dimvlachos.lab.core.platform.rememberSoundPlayer
import dev.dimvlachos.lab.core.presentation.canvas2d.setBlur
import dev.dimvlachos.lab.core.presentation.components.jackolantern.JackOLanternScope
import dev.dimvlachos.lab.core.presentation.components.jackolantern.JackOLanternSounds
import dev.dimvlachos.lab.core.presentation.components.jackolantern.JackOLanternState
import dev.dimvlachos.lab.core.presentation.components.jackolantern.JackOLanternTheme
import dev.dimvlachos.lab.core.presentation.components.touch.ScriptedTouch
import dev.dimvlachos.lab.core.presentation.components.touch.drawTouch
import dev.dimvlachos.lab.core.presentation.ui.JackOLanternScreenColors
import dev.dimvlachos.lab.core.presentation.ui.LabTheme
import dev.dimvlachos.lab.jackolanterndemo.JackOLanternDemos
import dev.dimvlachos.lab.resources.Res
import dev.dimvlachos.lab.resources.jackolantern_also
import dev.dimvlachos.lab.resources.jackolantern_bonfire
import dev.dimvlachos.lab.resources.jackolantern_bonfire_meta
import dev.dimvlachos.lab.resources.jackolantern_bonfire_when
import dev.dimvlachos.lab.resources.jackolantern_carving
import dev.dimvlachos.lab.resources.jackolantern_carving_meta
import dev.dimvlachos.lab.resources.jackolantern_carving_price
import dev.dimvlachos.lab.resources.jackolantern_cta
import dev.dimvlachos.lab.resources.jackolantern_date
import dev.dimvlachos.lab.resources.jackolantern_day_said
import dev.dimvlachos.lab.resources.jackolantern_donuts
import dev.dimvlachos.lab.resources.jackolantern_donuts_meta
import dev.dimvlachos.lab.resources.jackolantern_donuts_price
import dev.dimvlachos.lab.resources.jackolantern_farm
import dev.dimvlachos.lab.resources.jackolantern_hayride
import dev.dimvlachos.lab.resources.jackolantern_hayride_meta
import dev.dimvlachos.lab.resources.jackolantern_hayride_price
import dev.dimvlachos.lab.resources.jackolantern_hint_blow
import dev.dimvlachos.lab.resources.jackolantern_hint_light
import dev.dimvlachos.lab.resources.jackolantern_maze
import dev.dimvlachos.lab.resources.jackolantern_maze_meta
import dev.dimvlachos.lab.resources.jackolantern_maze_when
import dev.dimvlachos.lab.resources.jackolantern_night_said
import dev.dimvlachos.lab.resources.jackolantern_switch
import dev.dimvlachos.lab.resources.jackolantern_tab_map
import dev.dimvlachos.lab.resources.jackolantern_tab_tickets
import dev.dimvlachos.lab.resources.jackolantern_tab_tonight
import dev.dimvlachos.lab.resources.jackolantern_tab_you
import dev.dimvlachos.lab.resources.jackolantern_title
import kotlin.math.PI
import kotlin.math.sin
import kotlin.time.TimeSource
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/**
 * The prototype's farm app, "Tonight at the farm" for Maple Hollow Farm: the hayride, four more
 * things on tonight, a tickets button and the tab bar, with a carved pumpkin in the top bar as its
 * theme switch.
 */
@Composable
internal fun JackOLanternDemo(
    state: DemoState,
    // In the dark theme the candle starts lit, so the screen opens by night; a clip, and its
    // replay, always start with it out, so the script's tap and flick play the right way round.
    // The theme picks only where it starts: the pumpkin changes the screen, never the app.
    lantern: JackOLanternState =
        (LabTheme.isDark && !state.recording && !state.replay).let { lit ->
            remember { JackOLanternState(lit = lit) }
        },
    reducedMotion: Boolean = rememberReducedMotion(),
) {
    SideEffect { lantern.scripted = state.recording || state.replay }
    val sounds = rememberSoundPlayer(JackOLanternSounds)
    val finger = remember { LanternFinger() }
    // The script's moves take turns, as the prototype awaits each: on a slow device a move that
    // runs late holds the next back rather than steering the fingertip at the same time.
    val turn = remember { Mutex() }
    DisposableEffect(state, lantern) {
        state.setControlTapHandler { control -> turn.withLock { play(control, lantern, finger) } }
        state.setFeedResetHandler {
            turn.withLock {
                lantern.setInstant(false)
                finger.hide()
            }
        }
        onDispose {
            state.setControlTapHandler(null)
            state.setFeedResetHandler(null)
        }
    }
    // A script the user takes over leaves no fingertip behind, and the hint can come back.
    LaunchedEffect(state.replay) { if (!state.replay) finger.hide() }

    val touch = LabTheme.colors.touch
    val texts = farmTexts()
    Box(
        Modifier.fillMaxSize()
            .background(JackOLanternScreenColors.Day.background)
            .windowInsetsPadding(demoInsets(state)),
    ) {
        JackOLanternTheme(
            state = lantern,
            sounds = sounds,
            switchLabel = stringResource(Res.string.jackolantern_switch),
            nightSaid = stringResource(Res.string.jackolantern_night_said),
            daySaid = stringResource(Res.string.jackolantern_day_said),
            hintLight = stringResource(Res.string.jackolantern_hint_light),
            hintBlow = stringResource(Res.string.jackolantern_hint_blow),
            modifier = Modifier.fillMaxSize(),
            reducedMotion = reducedMotion,
        ) {
            Farm(texts)
        }
        Canvas(Modifier.fillMaxSize()) { with(finger) { drawFinger(touch) } }
    }
}

/** The screen's words, read once and shared by both copies. */
private class FarmTexts(
    val farm: String,
    val title: String,
    val date: String,
    val hayride: String,
    val hayrideMeta: String,
    val hayridePrice: String,
    val also: String,
    val rows: List<Triple<String, String, String>>,
    val cta: String,
    val tabs: List<String>,
)

@Composable
private fun farmTexts(): FarmTexts {
    @Composable fun s(r: StringResource) = stringResource(r)
    return FarmTexts(
        farm = s(Res.string.jackolantern_farm),
        title = s(Res.string.jackolantern_title),
        date = s(Res.string.jackolantern_date),
        hayride = s(Res.string.jackolantern_hayride),
        hayrideMeta = s(Res.string.jackolantern_hayride_meta),
        hayridePrice = s(Res.string.jackolantern_hayride_price),
        also = s(Res.string.jackolantern_also),
        rows =
            listOf(
                Triple(
                    s(Res.string.jackolantern_maze),
                    s(Res.string.jackolantern_maze_meta),
                    s(Res.string.jackolantern_maze_when),
                ),
                Triple(
                    s(Res.string.jackolantern_carving),
                    s(Res.string.jackolantern_carving_meta),
                    s(Res.string.jackolantern_carving_price),
                ),
                Triple(
                    s(Res.string.jackolantern_donuts),
                    s(Res.string.jackolantern_donuts_meta),
                    s(Res.string.jackolantern_donuts_price),
                ),
                Triple(
                    s(Res.string.jackolantern_bonfire),
                    s(Res.string.jackolantern_bonfire_meta),
                    s(Res.string.jackolantern_bonfire_when),
                ),
            ),
        cta = s(Res.string.jackolantern_cta),
        tabs =
            listOf(
                s(Res.string.jackolantern_tab_tonight),
                s(Res.string.jackolantern_tab_map),
                s(Res.string.jackolantern_tab_tickets),
                s(Res.string.jackolantern_tab_you),
            ),
    )
}

/** The screen the candle themes, composed once by day and once by night. */
@Composable
private fun JackOLanternScope.Farm(t: FarmTexts) {
    val c = colors
    Column(Modifier.fillMaxSize().background(c.background)) {
        // The top bar: the farm's name, tonight's title, and the pumpkin's slot.
        Row(
            Modifier.fillMaxWidth().height(66.dp).padding(start = 20.dp, end = 14.dp, top = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(
                    t.farm.uppercase(),
                    12.sp,
                    c.accent,
                    FontWeight.SemiBold,
                    letterSpacing = .06.em,
                )
                Text(
                    t.title,
                    23.sp,
                    c.ink,
                    FontWeight.Bold,
                    Modifier.semantics { heading() },
                    letterSpacing = (-.015).em,
                    lineHeight = 1.15.em,
                )
            }
            Box(Modifier.size(64.dp, 60.dp).pumpkinSlot())
        }
        Text(
            t.date,
            13.sp,
            c.muted,
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 2.dp, bottom = 12.dp),
        )
        // The hayride: the farm at dusk, or by moonlight.
        Column(
            Modifier.padding(horizontal = 16.dp)
                .cardShadow(c, 18f)
                .clip(RoundedCornerShape(18.dp))
                .background(c.card),
        ) {
            Canvas(Modifier.fillMaxWidth().aspectRatio(328f / 118f).clearAndSetSemantics {}) {
                drawLandscape(c)
            }
            Row(
                Modifier.fillMaxWidth()
                    .padding(start = 14.dp, end = 14.dp, top = 10.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Column(Modifier.weight(1f)) {
                    Text(t.hayride, 15.5.sp, c.ink, FontWeight(650))
                    Text(t.hayrideMeta, 12.5.sp, c.muted)
                }
                Text(
                    t.hayridePrice,
                    12.sp,
                    c.accent,
                    FontWeight.SemiBold,
                    Modifier.clip(RoundedCornerShape(999.dp))
                        .background(c.chip)
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                )
            }
        }
        Text(
            t.also.uppercase(),
            11.5.sp,
            c.muted,
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 6.dp),
            letterSpacing = .08.em,
        )
        Column(
            Modifier.padding(horizontal = 16.dp)
                .cardShadow(c, 16f)
                .clip(RoundedCornerShape(16.dp))
                .background(c.card),
        ) {
            for (i in t.rows.indices) {
                val (name, meta, side) = t.rows[i]
                Row(
                    Modifier.fillMaxWidth()
                        .drawBehind {
                            if (i > 0) drawRect(c.line, size = Size(size.width, 1.dp.toPx()))
                        }
                        .padding(start = 12.dp, end = 14.dp, top = 10.dp, bottom = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(
                        Modifier.size(38.dp).clip(RoundedCornerShape(11.dp)).background(c.tile),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(RowIcons[i], c.tileInk, 20f)
                    }
                    Column(Modifier.weight(1f)) {
                        Text(name, 14.5.sp, c.ink, FontWeight.SemiBold, maxLines = 1)
                        Text(meta, 12.5.sp, c.muted, maxLines = 1)
                    }
                    Text(side, 12.5.sp, c.muted, FontWeight.SemiBold)
                }
            }
        }
        Box(
            Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(c.accent)
                .padding(13.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(t.cta, 15.sp, c.onAccent, FontWeight(650))
        }
        Spacer(Modifier.weight(1f).heightIn(min = 8.dp))
        // The tab bar.
        Row(
            Modifier.fillMaxWidth()
                .background(c.tabBar)
                .drawBehind { drawRect(c.line, size = Size(size.width, 1.dp.toPx())) }
                .padding(start = 8.dp, end = 8.dp, top = 8.dp, bottom = 18.dp),
        ) {
            for (i in t.tabs.indices) {
                val on = i == 0
                val ink = if (on) c.accent else c.muted
                Column(
                    Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    Icon(TabIcons[i], ink, 22f)
                    Text(t.tabs[i], 11.sp, ink, if (on) FontWeight.SemiBold else FontWeight.Normal)
                }
            }
        }
    }
}

@Composable
private fun Text(
    text: String,
    size: TextUnit,
    color: Color,
    weight: FontWeight = FontWeight.Normal,
    modifier: Modifier = Modifier,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    maxLines: Int = Int.MAX_VALUE,
    // The page's `line-height: 1.55`, which the screen inherits: CSS's half-leading, above and
    // below.
    lineHeight: TextUnit = 1.55.em,
) {
    BasicText(
        text,
        modifier,
        style =
            TextStyle(
                color = color,
                fontSize = size,
                fontWeight = weight,
                letterSpacing = letterSpacing,
                lineHeight = lineHeight,
                lineHeightStyle =
                    LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.None),
            ),
        maxLines = maxLines,
        overflow = if (maxLines == 1) TextOverflow.Ellipsis else TextOverflow.Clip,
        softWrap = maxLines != 1,
    )
}

private val ShadowPaint = Paint()

/** The cards' `box-shadow: 0 6px 16px -10px`: a blurred shape 10 dp inside the card, 6 dp down. */
private fun Modifier.cardShadow(c: JackOLanternScreenColors, radius: Float) = drawBehind {
    drawIntoCanvas {
        ShadowPaint.color = c.shadow
        ShadowPaint.setBlur(c.shadowBlur / 2f * density)
        val inset = 10.dp.toPx()
        it.drawRoundRect(
            inset,
            inset + 6.dp.toPx(),
            size.width - inset,
            size.height - inset + 6.dp.toPx(),
            radius.dp.toPx(),
            radius.dp.toPx(),
            ShadowPaint,
        )
    }
}

private fun path(d: String) = PathParser().parsePathString(d).toPath()

/** An icon of the prototype's: its paths in a 24 box, the second dashed when [dashed]. */
private class Glyph(val paths: List<androidx.compose.ui.graphics.Path>, val dashed: Int = -1)

private fun glyph(vararg d: String, dashed: Int = -1) = Glyph(d.map { path(it) }, dashed)

private val RowIcons =
    listOf(
        glyph("M4 4h16v16H4z", "M8 4v8h4M16 8v12M12 16H8M12 12V8"),
        glyph(
            "M12 4c-4.4 0-8 3.2-8 7.6S7.6 20 12 20s8-3.9 8-8.4S16.4 4 12 4z",
            "M12 4c-1.6 2.6-1.6 13.4 0 16M12 4c1.6 2.6 1.6 13.4 0 16M12 4V2.4",
        ),
        glyph(
            "M12 4a8 8 0 1 0 0 16a8 8 0 1 0 0-16z",
            "M12 9.4a2.6 2.6 0 1 0 0 5.2a2.6 2.6 0 1 0 0-5.2z",
            "M8 7.6l.6.8M15.6 7.2l-.4.9M17 12.6l-.9.2M7 13.4l.8-.5",
        ),
        glyph(
            "M12 21c3.6 0 6-2.4 6-5.6 0-3.6-3-5.4-3.6-8.9-2 1.2-3.4 3.2-3.2 5.4-1-.6-1.8-1.6-2-2.8C7.4 10.6 6 12.8 6 15.4 6 18.6 8.4 21 12 21z",
        ),
    )

private val TabIcons =
    listOf(
        glyph("M4 11l8-6.5L20 11v8.5H4z", "M10 19.5v-5h4v5"),
        glyph("M9 5L4 7v12l5-2 6 2 5-2V5l-5 2z", "M9 5v12M15 7v12"),
        glyph(
            "M4 8V6h16v2a2 2 0 000 4v2a2 2 0 000 4v2H4v-2a2 2 0 000-4v-2a2 2 0 000-4z",
            "M14 6v14",
            dashed = 1,
        ),
        glyph(
            "M12 4.9a3.6 3.6 0 1 0 0 7.2a3.6 3.6 0 1 0 0-7.2z",
            "M5 20c.8-3.6 3.6-5.6 7-5.6s6.2 2 7 5.6",
        ),
    )

private val Dash = PathEffect.dashPathEffect(floatArrayOf(2f, 2f))

@Composable
private fun Icon(g: Glyph, ink: Color, size: Float) {
    Canvas(Modifier.size(size.dp).clearAndSetSemantics {}) {
        val s = this.size.width / 24f
        scale(s, s, pivot = Offset.Zero) {
            for (i in g.paths.indices) {
                drawPath(
                    g.paths[i],
                    ink,
                    style =
                        Stroke(
                            1.7f,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round,
                            pathEffect = if (i == g.dashed) Dash else null,
                        ),
                )
            }
        }
    }
}

private val Hill1 = path("M0 86 C60 62 120 70 180 82 S280 64 328 74 V118 H0z")
private val Barn = path("M64 96 V70 L84 56 L104 70 V96z")
private val Roof = path("M58 72 L84 52 L110 72 L106 74 L84 58 L62 74z")
private val Doors = path("M78 96 V80 H90 V96 M78 80 L90 96 M90 80 L78 96")
private val Hill2 = path("M0 102 C80 88 160 98 230 94 S300 90 328 96 V118 H0z")
private val Trees = path("M214 96 l9 -34 l9 34z M228 96 l7 -24 l7 24z")
private val Stars =
    arrayOf(
        floatArrayOf(34f, 18f, .9f),
        floatArrayOf(72f, 34f, .7f),
        floatArrayOf(118f, 14f, 1f),
        floatArrayOf(160f, 30f, .7f),
        floatArrayOf(196f, 12f, .8f),
        floatArrayOf(292f, 40f, .7f),
        floatArrayOf(16f, 44f, .6f),
        floatArrayOf(140f, 46f, .6f),
    )

/** The hero's inline SVG, a 328 by 118 box: sky, stars, the sun or moon, hills, barn, pumpkins. */
private fun DrawScope.drawLandscape(c: JackOLanternScreenColors) {
    val s = size.width / 328f
    scale(s, s, pivot = Offset.Zero) {
        drawRect(Brush.verticalGradient(listOf(c.sky1, c.sky2), 0f, 118f), size = Size(328f, 118f))
        if (c.stars > 0f) {
            for (st in Stars) {
                drawCircle(
                    JackOLanternScreenColors.Star,
                    st[2],
                    Offset(st[0], st[1]),
                    alpha = c.stars,
                )
            }
        }
        drawCircle(c.orb, 15f, Offset(258f, 34f))
        drawPath(Hill1, c.hill1)
        drawPath(Barn, c.barn)
        drawPath(Roof, c.barn)
        drawPath(Doors, c.barnTrim, style = Stroke(1.6f))
        drawRect(c.barnTrim, Offset(80f, 64f), Size(8f, 6f))
        drawPath(Hill2, c.hill2)
        drawPath(Trees, c.hill2)
        val pumpkin = JackOLanternScreenColors.FieldPumpkin
        drawOval(pumpkin, Offset(142f - 6.5f, 104f - 5f), Size(13f, 10f))
        drawOval(pumpkin, Offset(158f - 5f, 107f - 4f), Size(10f, 8f))
        drawOval(pumpkin, Offset(174f - 6f, 103f - 4.6f), Size(12f, 9.2f))
    }
}

// ---------- The scripted fingertip ----------

/** The prototype's finger `ease`: in and out, quadratic. */
private val FingerEase = Easing { p ->
    if (p < .5f) 2 * p * p else 1 - (-2 * p + 2) * (-2 * p + 2) / 2
}

private class LanternFinger {
    private val alpha = Animatable(0f)
    private val down = Animatable(0f)
    private val clock = TimeSource.Monotonic.markNow()
    var at by mutableStateOf(Offset.Zero)
        private set

    private var placed = false

    fun ms() = clock.elapsedNow().inWholeMicroseconds / 1000.0

    suspend fun appear(from: Offset) {
        if (!placed) {
            at = from
            placed = true
        }
        if (alpha.value < 1f) alpha.animateTo(1f, tween(200))
    }

    suspend fun glide(to: Offset, ms: Int, arc: Float = 0f, each: (Offset) -> Unit = {}) {
        val from = at
        val p = Animatable(0f)
        p.animateTo(1f, tween(ms, easing = LinearEasingPassThrough)) {
            val k = FingerEase.transform(value)
            at =
                Offset(
                    from.x + (to.x - from.x) * k,
                    from.y + (to.y - from.y) * k - sin(value * PI.toFloat()) * arc,
                )
            each(at)
        }
    }

    suspend fun press(action: (Offset) -> Unit) {
        action(at)
        down.animateTo(1f, tween(ScriptedTouch.DownMs))
    }

    suspend fun lift(action: (Offset) -> Unit) {
        action(at)
        down.animateTo(0f, tween(ScriptedTouch.DownMs))
    }

    suspend fun fadeOut() {
        alpha.animateTo(0f, tween(ScriptedTouch.UpMs))
        placed = false
    }

    suspend fun hide() {
        placed = false
        alpha.snapTo(0f)
        down.snapTo(0f)
    }

    fun DrawScope.drawFinger(color: Color) {
        val a = alpha.value
        if (a <= 0f) return
        drawTouch(color, at * density, a * (.6f + .4f * down.value))
    }
}

private val LinearEasingPassThrough = Easing { it }

/** The prototype's `demoTap`: to the pumpkin, a tap, and away unless [away] is false. */
private suspend fun tap(l: JackOLanternState, finger: LanternFinger, away: Boolean = true) {
    val at = Offset(l.px + 2f, l.py + 6f)
    finger.appear(Offset(l.px - 60f, l.py + 260f))
    finger.glide(at, 760)
    finger.press { l.down(it.x, it.y, finger.ms()) }
    delay(150)
    finger.lift { l.up(it.x, it.y, finger.ms()) }
    delay(260)
    if (away) finger.glide(Offset(l.px - 60f, l.py + 190f), 820)
}

/** The prototype's `demoFlick`: from low on the left, across the flame and up to the right. */
private suspend fun flick(l: JackOLanternState, finger: LanternFinger, approach: Int = 820) {
    val from = Offset(l.px - 120f, l.py + 46f)
    val to = Offset(l.px + 60f, l.py - 8f)
    finger.appear(Offset(l.px - 60f, l.py + 260f))
    finger.glide(from, approach)
    finger.press { l.down(it.x, it.y, finger.ms()) }
    delay(90)
    finger.glide(to, 230, arc = 10f) { l.move(it.x, it.y, finger.ms()) }
    finger.lift { l.up(it.x, it.y, finger.ms()) }
    delay(240)
    finger.glide(Offset(l.px - 60f, l.py + 190f), 900)
}

private suspend fun play(control: String, l: JackOLanternState, finger: LanternFinger) {
    try {
        while (!l.laidOut) delay(16)
        when (control) {
            JackOLanternDemos.Light -> {
                if (l.lit) l.setInstant(false)
                tap(l, finger)
                // The light spreads over the whole screen; a beat to see the night.
                delay(2600)
            }
            JackOLanternDemos.Blow -> {
                flick(l, finger)
                delay(2300)
                finger.fadeOut()
            }
            JackOLanternDemos.LightAndTurn -> {
                if (l.lit) l.setInstant(false)
                tap(l, finger, away = false)
                // Changed its mind while the night is still spreading: the flick turns it round.
                flick(l, finger, approach = 520)
                delay(2600)
                finger.fadeOut()
            }
        }
    } finally {
        // Taken over mid-move, the candle lets go of the scripted finger.
        l.cancel()
    }
}
