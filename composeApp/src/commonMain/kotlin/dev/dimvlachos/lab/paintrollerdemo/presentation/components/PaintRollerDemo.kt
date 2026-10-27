package dev.dimvlachos.lab.paintrollerdemo.presentation.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.layout.layout
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import dev.dimvlachos.lab.core.demo.DemoState
import dev.dimvlachos.lab.core.demo.demoInsets
import dev.dimvlachos.lab.core.platform.rememberReducedMotion
import dev.dimvlachos.lab.core.platform.rememberSoundPlayer
import dev.dimvlachos.lab.core.presentation.canvas2d.setBlur
import dev.dimvlachos.lab.core.presentation.components.paintroller.PaintRollerAccent
import dev.dimvlachos.lab.core.presentation.components.paintroller.PaintRollerScope
import dev.dimvlachos.lab.core.presentation.components.paintroller.PaintRollerSounds
import dev.dimvlachos.lab.core.presentation.components.paintroller.PaintRollerState
import dev.dimvlachos.lab.core.presentation.components.touch.ScriptedTouch
import dev.dimvlachos.lab.core.presentation.components.touch.drawTouch
import dev.dimvlachos.lab.core.presentation.ui.LabTheme
import dev.dimvlachos.lab.core.presentation.ui.PaintRollerScreenColors
import dev.dimvlachos.lab.paintrollerdemo.PaintRollerDemos
import dev.dimvlachos.lab.resources.Res
import dev.dimvlachos.lab.resources.paintroller_accent
import dev.dimvlachos.lab.resources.paintroller_auto_pause
import dev.dimvlachos.lab.resources.paintroller_back
import dev.dimvlachos.lab.resources.paintroller_color_route
import dev.dimvlachos.lab.resources.paintroller_easy
import dev.dimvlachos.lab.resources.paintroller_goal
import dev.dimvlachos.lab.resources.paintroller_goal_done
import dev.dimvlachos.lab.resources.paintroller_goal_of
import dev.dimvlachos.lab.resources.paintroller_hills
import dev.dimvlachos.lab.resources.paintroller_km
import dev.dimvlachos.lab.resources.paintroller_mi
import dev.dimvlachos.lab.resources.paintroller_now
import dev.dimvlachos.lab.resources.paintroller_paint_1
import dev.dimvlachos.lab.resources.paintroller_paint_2
import dev.dimvlachos.lab.resources.paintroller_paint_3
import dev.dimvlachos.lab.resources.paintroller_paint_4
import dev.dimvlachos.lab.resources.paintroller_paint_5
import dev.dimvlachos.lab.resources.paintroller_reset
import dev.dimvlachos.lab.resources.paintroller_route
import dev.dimvlachos.lab.resources.paintroller_run
import dev.dimvlachos.lab.resources.paintroller_said
import dev.dimvlachos.lab.resources.paintroller_splits
import dev.dimvlachos.lab.resources.paintroller_start
import dev.dimvlachos.lab.resources.paintroller_title
import dev.dimvlachos.lab.resources.paintroller_trails
import dev.dimvlachos.lab.resources.paintroller_undo
import dev.dimvlachos.lab.resources.paintroller_units
import kotlin.math.roundToInt
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.stringResource

/**
 * The prototype's running app's Appearance settings: a preview of a run on a map, three switches,
 * the units and a reset, and under them the accent colours as five paint trays with a roller.
 */
@Composable
internal fun PaintRollerDemo(
    state: DemoState,
    roller: PaintRollerState = remember { PaintRollerState() },
    reducedMotion: Boolean = rememberReducedMotion(),
) {
    val dark = LabTheme.isDark
    val colors = if (dark) PaintRollerScreenColors.Dark else PaintRollerScreenColors.Light
    val sounds = rememberSoundPlayer(PaintRollerSounds)
    val switches = remember { mutableStateListOf(true, true, false) }
    val finger = remember { RollerFinger() }
    val names =
        listOf(
            stringResource(Res.string.paintroller_paint_1),
            stringResource(Res.string.paintroller_paint_2),
            stringResource(Res.string.paintroller_paint_3),
            stringResource(Res.string.paintroller_paint_4),
            stringResource(Res.string.paintroller_paint_5),
        )
    val now = stringResource(Res.string.paintroller_now, "%s")
    val said = stringResource(Res.string.paintroller_said, "%s")

    // The script's moves take turns, as the prototype awaits each: on a slow device a move that
    // runs late holds the next back rather than steering the fingertip at the same time.
    val turn = remember { Mutex() }

    DisposableEffect(state, roller) {
        state.setControlTapHandler { control -> turn.withLock { play(control, roller, finger) } }
        state.setFeedResetHandler {
            turn.withLock {
                roller.reset()
                switches[0] = true
                switches[1] = true
                switches[2] = false
                finger.hide()
            }
        }
        onDispose {
            state.setControlTapHandler(null)
            state.setFeedResetHandler(null)
        }
    }
    // A script the user takes over leaves no fingertip behind.
    LaunchedEffect(state.replay) { if (!state.replay) finger.hide() }

    val touch = LabTheme.colors.touch
    Box(
        Modifier.fillMaxSize().background(colors.background).windowInsetsPadding(demoInsets(state)),
    ) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            // The prototype's breakpoints, on its screen's height, which had a 30 dp status row
            // the demo screen's own bars take the place of.
            val h = maxHeight.value + 30f
            val size =
                when {
                    h < 690f -> ScreenSize.Tiny
                    h < 800f -> ScreenSize.Short
                    h >= 880f -> ScreenSize.Tall
                    else -> ScreenSize.Normal
                }
            PaintRollerAccent(
                state = roller,
                colors = colors,
                dark = dark,
                sounds = sounds,
                names = names,
                snackText = { now.replace("%s", it) },
                said = { said.replace("%s", it) },
                undoLabel = stringResource(Res.string.paintroller_undo),
                modifier = Modifier.fillMaxSize(),
                reducedMotion = reducedMotion,
            ) {
                Appearance(size, names, switches)
            }
        }
        Canvas(Modifier.fillMaxSize()) { with(finger) { drawFinger(touch) } }
    }
}

private enum class ScreenSize {
    Tiny,
    Short,
    Normal,
    Tall;

    val short
        get() = this == Tiny || this == Short
}

/** The screen the roller paints, composed once for each coat. */
@Composable
private fun PaintRollerScope.Appearance(
    size: ScreenSize,
    names: List<String>,
    switches: MutableList<Boolean>,
) {
    val c = colors
    Column(Modifier.fitHeight().fillMaxSize().background(c.background).padding(bottom = 8.dp)) {
        // The app bar: a back link in the accent, and the title.
        Column(
            Modifier.padding(
                start = 18.dp,
                end = 18.dp,
                top = 6.dp,
                bottom = if (size.short) 4.dp else 8.dp,
            ),
        ) {
            Row(
                Modifier.heightIn(min = 32.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                val ink = accent
                Canvas(Modifier.size(18.dp)) {
                    val s = this.size.width / 24f
                    val p =
                        Path().apply {
                            moveTo(15 * s, 5 * s)
                            lineTo(8 * s, 12 * s)
                            lineTo(15 * s, 19 * s)
                        }
                    drawPath(
                        p,
                        ink,
                        style = Stroke(2.4f * s, cap = StrokeCap.Round, join = StrokeJoin.Round),
                    )
                }
                Text(
                    stringResource(Res.string.paintroller_back),
                    15.sp,
                    accent,
                    FontWeight.SemiBold,
                )
            }
            val titleSize =
                when (size) {
                    ScreenSize.Tiny -> 22.sp
                    ScreenSize.Short -> 24.sp
                    else -> 28.sp
                }
            BasicText(
                stringResource(Res.string.paintroller_title),
                Modifier.padding(top = 2.dp).cssLineBox(titleSize, 1.05f).semantics { heading() },
                style =
                    TextStyle(
                        color = c.ink,
                        fontSize = titleSize,
                        fontWeight = FontWeight(750),
                        letterSpacing = (-.02).em,
                    ),
            )
        }
        val cardGap = if (size.short) 8.dp else if (size == ScreenSize.Tall) 14.dp else 10.dp
        Card(c, cardGap) {
            Preview(size)
        }
        Card(c, cardGap) {
            Column(Modifier.padding(vertical = 2.dp)) {
                val rowHeight =
                    when (size) {
                        ScreenSize.Tiny -> 34.dp
                        ScreenSize.Short -> 39.dp
                        ScreenSize.Tall -> 52.dp
                        else -> 46.dp
                    }
                val labels =
                    listOf(
                        stringResource(Res.string.paintroller_splits),
                        stringResource(Res.string.paintroller_color_route),
                        stringResource(Res.string.paintroller_auto_pause),
                    )
                for (i in 0 until 3) {
                    SettingRow(
                        rowHeight,
                        i > 0,
                        c.line,
                        { Text(labels[i], 14.5.sp, c.ink) },
                    ) {
                        Switch(i, labels[i], switches)
                    }
                }
                SettingRow(
                    rowHeight,
                    true,
                    c.line,
                    { Text(stringResource(Res.string.paintroller_units), 14.5.sp, c.ink) },
                ) {
                    Units()
                }
                SettingRow(
                    rowHeight,
                    true,
                    c.line,
                    {
                        Text(
                            stringResource(Res.string.paintroller_reset),
                            14.5.sp,
                            accent,
                            FontWeight(650),
                            if (isCoat) Modifier
                            else
                                Modifier.clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    role = Role.Button,
                                ) {
                                    state.resetToDefault()
                                },
                        )
                    },
                )
            }
        }
        // The accent colours: five trays, the roller resting in the current one.
        Column(
            Modifier.padding(
                    start = 16.dp,
                    end = 16.dp,
                    top = if (size == ScreenSize.Tall) 18.dp else 12.dp,
                )
                .accentSection()
                .padding(bottom = 4.dp),
        ) {
            Row(
                Modifier.fillMaxWidth().padding(start = 4.dp, end = 4.dp, bottom = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    stringResource(Res.string.paintroller_accent).uppercase(),
                    12.sp,
                    c.muted,
                    FontWeight.Bold,
                    Modifier.alignByBaseline(),
                    letterSpacing = .06.em,
                )
                Text(
                    names[state.current],
                    13.sp,
                    c.ink,
                    FontWeight(650),
                    Modifier.alignByBaseline(),
                )
            }
            Trays(
                names,
                when (size) {
                    ScreenSize.Tiny -> 66.dp
                    ScreenSize.Short -> 78.dp
                    ScreenSize.Tall -> 100.dp
                    else -> 92.dp
                },
            )
        }
    }
}

@Composable
private fun Card(c: PaintRollerScreenColors, bottom: Dp, content: @Composable () -> Unit) {
    Box(
        Modifier.padding(start = 16.dp, end = 16.dp, bottom = bottom)
            .fillMaxWidth()
            .clip(CardShape)
            .background(c.card)
            .border(1.dp, c.line, CardShape)
            .padding(1.dp),
    ) {
        content()
    }
}

/** The preview: the route on a map, the run, its kind, the weekly goal and Start run. */
@Composable
private fun PaintRollerScope.Preview(size: ScreenSize) {
    val c = colors
    val pad = if (size.short) 8.dp else 10.dp
    Column(
        Modifier.padding(start = pad, end = pad, top = pad, bottom = pad + 2.dp),
        verticalArrangement = Arrangement.spacedBy(if (size.short) 7.dp else 9.dp),
    ) {
        val route = accent
        Box(
            Modifier.fillMaxWidth()
                .height(
                    when (size) {
                        ScreenSize.Tiny,
                        ScreenSize.Short -> 60.dp
                        ScreenSize.Tall -> 104.dp
                        else -> 74.dp
                    },
                )
                .clip(RoundedCornerShape(11.dp))
                .background(c.map)
                .clearAndSetSemantics {}
                .drawBehind { drawRouteMap(route) },
        )
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.Start),
        ) {
            Text(
                stringResource(Res.string.paintroller_run),
                15.sp,
                c.ink,
                FontWeight.Bold,
                Modifier.alignByBaseline().weight(1f),
            )
            Text(
                stringResource(Res.string.paintroller_route),
                12.sp,
                c.muted,
                modifier = Modifier.alignByBaseline(),
            )
        }
        Row(
            Modifier.padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Chip(stringResource(Res.string.paintroller_easy), false)
            Chip(stringResource(Res.string.paintroller_trails), true)
            Chip(stringResource(Res.string.paintroller_hills), false)
        }
        Column(
            Modifier.padding(horizontal = 4.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stringResource(Res.string.paintroller_goal), 12.sp, c.muted)
                val done = stringResource(Res.string.paintroller_goal_done)
                val of = stringResource(Res.string.paintroller_goal_of, done)
                val goal = buildAnnotatedString {
                    val at = of.indexOf(done)
                    append(of.substring(0, at.coerceAtLeast(0)))
                    withStyle(SpanStyle(color = c.ink, fontWeight = FontWeight(650))) {
                        append(done)
                    }
                    append(of.substring((at + done.length).coerceAtMost(of.length)))
                }
                BasicText(goal, style = body(12.sp, c.muted))
            }
            Box(Modifier.fillMaxWidth().height(8.dp).clip(Bar).background(c.chip)) {
                Box(
                    Modifier.fillMaxWidth(.72f)
                        .fillMaxHeight()
                        .clip(Bar)
                        .background(accent)
                        .wetPaint(1, Bar),
                )
            }
        }
        Box(
            Modifier.padding(start = 4.dp, end = 4.dp, top = 2.dp)
                .fillMaxWidth()
                .clip(StartShape)
                .background(accent)
                .wetPaint(2, StartShape)
                .padding(if (size.short) 9.dp else 11.dp),
            contentAlignment = Alignment.Center,
        ) {
            BasicText(
                stringResource(Res.string.paintroller_start),
                style = TextStyle(color = onAccent, fontSize = 15.sp, fontWeight = FontWeight.Bold),
            )
        }
    }
}

@Composable
private fun PaintRollerScope.Chip(text: String, on: Boolean) {
    Box(
        Modifier.clip(CircleShape)
            .background(if (on) accent else colors.chip)
            .then(if (on) Modifier.wetPaint(0, CircleShape) else Modifier)
            .padding(horizontal = 11.dp, vertical = 5.dp),
    ) {
        Text(text, 12.5.sp, if (on) onAccent else colors.ink, FontWeight.SemiBold)
    }
}

@Composable
private fun SettingRow(
    height: Dp,
    divided: Boolean,
    line: Color,
    label: @Composable () -> Unit,
    trailing: @Composable () -> Unit = {},
) {
    Row(
        Modifier.fillMaxWidth()
            .heightIn(min = height)
            .drawBehind {
                if (divided)
                    drawRect(
                        line,
                        size = androidx.compose.ui.geometry.Size(size.width, 1.dp.toPx()),
                    )
            }
            .padding(start = 14.dp, end = 14.dp, top = if (divided) 1.dp else 0.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.Start),
    ) {
        Box(Modifier.weight(1f)) { label() }
        trailing()
    }
}

/** A switch: its track in the accent when on, its knob gliding across with a little overshoot. */
@Composable
private fun PaintRollerScope.Switch(i: Int, label: String, switches: MutableList<Boolean>) {
    val on = switches[i]
    val knob by
        animateFloatAsState(
            if (on) 1f else 0f,
            if (state.reducedMotion) snap() else tween(180, easing = KnobEase),
            label = "knob",
        )
    val track = if (on) accent else colors.off
    Box(
        Modifier.size(46.dp, 28.dp)
            .clip(Track)
            .drawBehind { drawRect(track) }
            .wetPaint(3 + i, Track, under = true)
            .then(
                if (isCoat) Modifier
                else
                    Modifier.semantics { contentDescription = label }
                        .toggleable(
                            value = on,
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            role = Role.Switch,
                        ) {
                            switches[i] = it
                        },
            ),
    ) {
        Box(
            Modifier.offset(x = 3.dp + 18.dp * knob, y = 3.dp).size(22.dp).drawBehind {
                // box-shadow: 0 1px 3px rgba(0,0,0,.25), under a white knob.
                drawIntoCanvas {
                    KnobShadow.setBlur(1.5.dp.toPx())
                    it.drawCircle(
                        Offset(size.width / 2, size.height / 2 + 1.dp.toPx()),
                        size.width / 2,
                        KnobShadow,
                    )
                }
                drawCircle(Color.White)
            },
        )
    }
}

/** The units: mi in the accent, km plain. */
@Composable
private fun PaintRollerScope.Units() {
    Row(
        Modifier.clip(RoundedCornerShape(10.dp))
            .background(colors.chip)
            .padding(3.dp)
            .clearAndSetSemantics {},
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Box(
            Modifier.widthIn(min = 40.dp)
                .clip(Seg)
                .background(accent)
                .wetPaint(6, Seg)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(stringResource(Res.string.paintroller_mi), 13.sp, onAccent, FontWeight(650))
        }
        Box(
            Modifier.widthIn(min = 40.dp).padding(horizontal = 8.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(stringResource(Res.string.paintroller_km), 13.sp, colors.ink, FontWeight(650))
        }
    }
}

/**
 * The map's SVG (`viewBox 0 0 300 74`, sliced to fill): streets, the green way, the route in the
 * accent from its start ring to its end dot.
 */
private fun DrawScope.drawRouteMap(accent: Color) {
    val w = size.width
    val h = size.height
    val s = maxOf(w / 300f, h / 74f)
    clipRect {
        translate((w - 300 * s) / 2, (h - 74 * s) / 2) {
            scale(s, s, Offset.Zero) {
                val major = Color.White.copy(alpha = .75f)
                val minor = Color.White.copy(alpha = .5f)
                fun line(x0: Float, y0: Float, x1: Float, y1: Float, c: Color, width: Float) =
                    drawLine(c, Offset(x0, y0), Offset(x1, y1), width)
                line(-10f, 52f, 320f, 40f, major, 5f)
                line(70f, -10f, 95f, 90f, major, 5f)
                line(190f, -10f, 210f, 90f, major, 5f)
                line(-10f, 18f, 320f, 10f, minor, 2.5f)
                line(130f, -10f, 150f, 90f, minor, 2.5f)
                line(250f, -10f, 262f, 90f, minor, 2.5f)
                drawPath(
                    GreenWay,
                    PaintRollerScreenColors.GreenWay,
                    style = Stroke(16f, cap = StrokeCap.Round),
                )
                drawPath(
                    Route,
                    accent,
                    style = Stroke(4.5f, cap = StrokeCap.Round, join = StrokeJoin.Round),
                )
                drawCircle(Color.White, 5.5f, Offset(34f, 58f))
                drawCircle(accent, 5.5f, Offset(34f, 58f), style = Stroke(3f))
                drawCircle(accent, 6f, Offset(240f, 46f))
                drawCircle(Color.White, 6f, Offset(240f, 46f), style = Stroke(2.5f))
            }
        }
    }
}

// M0 62 C 40 66 60 40 88 30 S 150 22 170 36 S 220 64 250 44 S 290 14 310 18
private val GreenWay =
    Path().apply {
        moveTo(0f, 62f)
        cubicTo(40f, 66f, 60f, 40f, 88f, 30f)
        cubicTo(116f, 20f, 150f, 22f, 170f, 36f)
        cubicTo(190f, 50f, 220f, 64f, 250f, 44f)
        cubicTo(280f, 24f, 290f, 14f, 310f, 18f)
    }

// M34 58 C 60 54 72 34 96 28 S 150 22 170 36 S 214 62 240 46
private val Route =
    Path().apply {
        moveTo(34f, 58f)
        cubicTo(60f, 54f, 72f, 34f, 96f, 28f)
        cubicTo(120f, 22f, 150f, 22f, 170f, 36f)
        cubicTo(190f, 50f, 214f, 62f, 240f, 46f)
    }

private val KnobShadow = Paint().apply { color = Color(0f, 0f, 0f, .25f) }
private val KnobEase = CubicBezierEasing(.3f, .9f, .3f, 1.2f)
private val CardShape = RoundedCornerShape(18.dp)
private val StartShape = RoundedCornerShape(12.dp)
private val Bar = RoundedCornerShape(4.dp)
private val Track = RoundedCornerShape(14.dp)
private val Seg = RoundedCornerShape(7.dp)

/** CSS line boxes: the leading split above and below every line, the first and last included. */
private val CssLines = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.None)

private fun body(
    size: TextUnit,
    color: Color,
    weight: FontWeight = FontWeight.Normal,
    spacing: TextUnit = TextUnit.Unspecified,
) =
    TextStyle(
        lineHeightStyle = CssLines,
        color = color,
        fontSize = size,
        lineHeight = 1.55.em,
        fontWeight = weight,
        letterSpacing = spacing,
    )

@Composable
private fun Text(
    text: String,
    size: TextUnit,
    color: Color,
    weight: FontWeight = FontWeight.Normal,
    modifier: Modifier = Modifier,
    letterSpacing: TextUnit = TextUnit.Unspecified,
) {
    BasicText(
        text,
        modifier,
        style = body(size, color, weight, letterSpacing),
        maxLines = 1,
        softWrap = false,
    )
}

/** One of the script's moves, [control], played with the scripted [finger] on [roller]. */
private suspend fun play(control: String, roller: PaintRollerState, finger: RollerFinger) {
    try {
        if (!roller.laidOut) return
        when (control) {
            PaintRollerDemos.PaintSedona -> {
                // Press Sedona, drag up the left side, across, and partway down the
                // right; let go and it finishes.
                val w = roller.width
                val h = roller.height
                val t = roller.trays[1]
                val s = Offset(t.cx, t.top + t.th * .5f)
                finger.appear(Offset(w * .62f, h * .7f))
                finger.glide(s, 520)
                finger.press { roller.trayDown(1, it.x, it.y, demo = true) }
                delay(240)
                val l = roller.colL
                val r = roller.colR
                val top = roller.topY + roller.grip
                val low = roller.botY + roller.grip
                val move: (Offset) -> Unit = { roller.trayMove(it.x, it.y) }
                finger.glide(Offset(l, low), 260, move)
                finger.glide(Offset(l, top), 950, move)
                finger.glide(Offset(r - 10, top + 6), 420, move)
                finger.glide(Offset(r - 10, top + (low - top) * .55f), 650, move)
                finger.lift { roller.trayUp(it.x, it.y) }
                finger.glide(Offset(w * .5f, h * .62f), 500)
            }
            PaintRollerDemos.Undo -> {
                // Once the roller is home, a beat, then Undo rolls the old blue back on.
                while (roller.busy) delay(16)
                delay(1600)
                val at = roller.undoCentre
                if (at != null) finger.glide(at, 600)
                finger.press {}
                delay(ScriptedTouch.TapHoldMs)
                finger.lift { roller.undo() }
                finger.glide(Offset(roller.width * .7f, roller.height * .55f), 500)
                finger.fadeOut()
                while (roller.busy) delay(16)
            }
        }
    } finally {
        // A move cut short lets go where the finger was, so the roller finishes the wall.
        if (finger.pressed)
            withContext(NonCancellable) { roller.releaseDemo(finger.at.x, finger.at.y) }
        finger.pressed = false
    }
}

/** The scripted fingertip, as the prototype's demo finger: it glides on the prototype's ease. */
private class RollerFinger {
    private val alpha = Animatable(0f)
    private val down = Animatable(0f)
    var at by mutableStateOf(Offset.Zero)
        private set

    private var placed = false
    var pressed = false

    suspend fun appear(from: Offset) {
        if (!placed) {
            at = from
            placed = true
        }
        if (alpha.value < 1f) alpha.animateTo(1f, tween(200))
    }

    suspend fun glide(to: Offset, ms: Int, each: (Offset) -> Unit = {}) {
        val from = at
        val p = Animatable(0f)
        p.animateTo(1f, tween(ms, easing = PrototypeEase)) {
            at = Offset(from.x + (to.x - from.x) * value, from.y + (to.y - from.y) * value)
            each(at)
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
        drawTouch(color, at * density, a * (.6f + .4f * down.value))
    }
}

/**
 * A line box [lineHeight] times the font size, as CSS gives a line-height under the font's own
 * height: the text is centred in it and overflows it evenly, where Compose would not shrink below
 * the font's height.
 */
private fun Modifier.cssLineBox(fontSize: TextUnit, lineHeight: Float) =
    layout { measurable, constraints ->
        val p =
            measurable.measure(constraints.copy(minHeight = 0, maxHeight = Constraints.Infinity))
        val box = (fontSize.toPx() * lineHeight).roundToInt()
        layout(p.width, box) { p.place(0, (box - p.height) / 2) }
    }

/**
 * The screen at its own size when it fits; on a phone shorter than the prototype's shortest (or
 * under a large font), the whole screen scaled down from the top until it does, rather than the
 * trays cut off at the bottom.
 */
private fun Modifier.fitHeight() = layout { measurable, constraints ->
    if (!constraints.hasBoundedHeight) {
        val p = measurable.measure(constraints)
        return@layout layout(p.width, p.height) { p.place(0, 0) }
    }
    // Measured as tall as it likes: its own height, the column's fill aside.
    val p = measurable.measure(constraints.copy(minHeight = 0, maxHeight = Constraints.Infinity))
    val room = constraints.maxHeight
    layout(constraints.maxWidth, room) {
        if (p.height <= room) {
            p.place(0, 0)
        } else {
            val s = room / p.height.toFloat()
            p.placeWithLayer(0, 0) {
                scaleX = s
                scaleY = s
                transformOrigin = TransformOrigin(.5f, 0f)
            }
        }
    }
}

private val PrototypeEase = Easing { PaintRollerState.ease(it) }
