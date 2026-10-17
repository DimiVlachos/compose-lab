package dev.dimvlachos.lab.snowglobedemo.presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import dev.dimvlachos.lab.core.demo.DemoState
import dev.dimvlachos.lab.core.demo.demoInsets
import dev.dimvlachos.lab.core.platform.Sound
import dev.dimvlachos.lab.core.platform.rememberReducedMotion
import dev.dimvlachos.lab.core.platform.rememberSoundPlayer
import dev.dimvlachos.lab.core.presentation.components.snowglobe.GlobeHint
import dev.dimvlachos.lab.core.presentation.components.snowglobe.SnowGlobe
import dev.dimvlachos.lab.core.presentation.components.snowglobe.SnowGlobeState
import dev.dimvlachos.lab.core.presentation.components.snowglobe.globeLayout
import dev.dimvlachos.lab.core.presentation.components.snowglobe.rememberSnowGlobeState
import dev.dimvlachos.lab.core.presentation.components.touch.drawTouch
import dev.dimvlachos.lab.core.presentation.ui.LabTheme
import dev.dimvlachos.lab.core.presentation.ui.SnowGlobeScreenColors
import dev.dimvlachos.lab.resources.Res
import dev.dimvlachos.lab.resources.snowglobe_announce_updated
import dev.dimvlachos.lab.resources.snowglobe_day_label
import dev.dimvlachos.lab.resources.snowglobe_extra
import dev.dimvlachos.lab.resources.snowglobe_failed
import dev.dimvlachos.lab.resources.snowglobe_hint_idle
import dev.dimvlachos.lab.resources.snowglobe_hint_keep
import dev.dimvlachos.lab.resources.snowglobe_hint_let_settle
import dev.dimvlachos.lab.resources.snowglobe_hint_settling
import dev.dimvlachos.lab.resources.snowglobe_hint_updated
import dev.dimvlachos.lab.resources.snowglobe_place
import dev.dimvlachos.lab.resources.snowglobe_refresh
import dev.dimvlachos.lab.resources.snowglobe_updated_now
import dev.dimvlachos.lab.snowglobedemo.DayForecast
import dev.dimvlachos.lab.snowglobedemo.FakeForecastSource
import dev.dimvlachos.lab.snowglobedemo.Forecast
import dev.dimvlachos.lab.snowglobedemo.WeatherBoard
import dev.dimvlachos.lab.snowglobedemo.rememberWeatherBoard
import kotlin.math.PI
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.time.Duration
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

/**
 * A weather screen for Oia built round a souvenir snow globe of Santorini, laid out as the HTML
 * prototype lays it out: the place and how fresh the forecast is, the day and its temperature, the
 * globe in the room below, a hint under its base, and a strip of day cards. Shaking the globe
 * refreshes the forecast, which lands once the glitter has settled; swiping across the screen,
 * tapping a day or moving along the days with the arrow keys carries the globe to that day.
 */
@Composable
internal fun SnowGlobeDemo(
    state: DemoState,
    board: WeatherBoard = rememberWeatherBoard(),
    source: FakeForecastSource = remember { FakeForecastSource() },
    globe: SnowGlobeState = rememberSnowGlobeState(),
    reducedMotion: Boolean = rememberReducedMotion(),
) {
    val sounds = rememberSoundPlayer(setOf(Sound.GlobeSwish, Sound.MusicBoxPhrase))
    val scope = rememberCoroutineScope()
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val colors = if (LabTheme.isDark) SnowGlobeScreenColors.Dark else SnowGlobeScreenColors.Light
    // The scripted fingertip, across the glass as a fraction of its radius, or null when lifted.
    // Only the drawing reads it, so the shake doesn't recompose the screen.
    val fingertip = remember { mutableStateOf<Float?>(null) }
    // Where the header ends and the day strip starts, which the globe is placed between.
    var headerBottom by remember { mutableFloatStateOf(0f) }
    var daysTop by remember { mutableFloatStateOf(0f) }
    var width by remember { mutableFloatStateOf(0f) }
    var rootTop by remember { mutableFloatStateOf(0f) }
    val touch = LabTheme.colors.touch

    fun select(index: Int) {
        val direction = board.select(index)
        if (direction != 0) globe.rock(direction)
    }

    // As in the prototype, the new forecast lands once the glitter has settled: the fetch starts
    // with the shake, and its answer waits for the settle.
    var fetching by remember { mutableStateOf(false) }
    var fetched by remember { mutableStateOf<Pair<Int, Result<Forecast>>?>(null) }
    var fetch by remember { mutableStateOf<Job?>(null) }

    DisposableEffect(state, board, globe) {
        state.setGlobeShakeHandler { duration ->
            scriptedShake(globe, duration) { fingertip.value = it }
        }
        state.setControlTapHandler { control ->
            control.removePrefix("day-").toIntOrNull()?.let(::select)
        }
        state.setFeedResetHandler {
            // Forget the fetch in flight too, or its answer would land on the next shake's settle.
            fetch?.cancel()
            fetch = null
            fetching = false
            fetched = null
            board.reset()
            source.reset()
            globe.reset()
        }
        onDispose {
            state.setGlobeShakeHandler(null)
            state.setControlTapHandler(null)
            state.setFeedResetHandler(null)
        }
    }

    // "Updated" shows under the globe for a moment after a refresh lands.
    var justUpdated by remember { mutableStateOf(false) }
    LaunchedEffect(justUpdated) {
        if (justUpdated) {
            delay(2400)
            justUpdated = false
        }
    }

    val day = board.selectedDay
    val conditions = stringResource(day.conditions)
    val fresh = board.updated == Res.string.snowglobe_updated_now && !board.failed
    val updatedText =
        if (board.failed) stringResource(Res.string.snowglobe_failed)
        else stringResource(board.updated)
    val announcement =
        board.lastChange?.let {
            stringResource(
                Res.string.snowglobe_announce_updated,
                stringResource(it.label),
                stringResource(it.conditions),
                it.high,
            )
        }
    val layout =
        if (width > 0f && daysTop > headerBottom) globeLayout(width, headerBottom, daysTop)
        else null

    Box(
        Modifier.fillMaxSize()
            .background(colors.background)
            .onGloballyPositioned {
                width = it.size.width.toFloat()
                rootTop = it.positionInRoot().y
            }
            // The globe keeps its own drags, so only a swipe elsewhere changes the day.
            .pointerInput(board, rtl) {
                val step = 64.dp.toPx()
                var travelled = 0f
                detectHorizontalDragGestures(onDragStart = { travelled = 0f }) { change, dx ->
                    change.consume()
                    travelled += if (rtl) -dx else dx
                    if (travelled <= -step) {
                        select(board.selected + 1)
                        travelled = 0f
                    } else if (travelled >= step) {
                        select(board.selected - 1)
                        travelled = 0f
                    }
                }
            }
    ) {
        SnowGlobe(
            scene = day.scene,
            refreshPending = fetching,
            onShaken = {
                board.startRefresh()?.let { token ->
                    fetching = true
                    fetch = scope.launch {
                        val result = source.refresh()
                        fetched = token to result
                        fetching = false
                    }
                }
            },
            onSettled = {
                fetched?.let { (token, result) -> board.finishRefresh(token, result) }
                fetched = null
                if (!board.failed) {
                    sounds.play(Sound.MusicBoxPhrase, 0.8f)
                    justUpdated = true
                }
            },
            modifier =
                Modifier.fillMaxSize().drawWithContent {
                    drawContent()
                    val x = fingertip.value ?: return@drawWithContent
                    val place = layout ?: return@drawWithContent
                    drawTouch(
                        touch,
                        Offset(place.cx + x * place.radius, place.cy + 0.15f * place.radius),
                        1f,
                    )
                },
            state = globe,
            sounds = sounds,
            shakeSensorEnabled = !state.recording,
            description = stringResource(Res.string.snowglobe_refresh),
            reducedMotion = reducedMotion,
            stateDescription = conditions,
            layout = layout,
        )

        Column(Modifier.fillMaxSize().windowInsetsPadding(demoInsets(state))) {
            // The app bar: where, and how fresh the forecast is.
            Row(
                Modifier.padding(start = 20.dp, top = 18.dp, end = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Image(
                    rememberVectorPainter(PinIcon),
                    null,
                    Modifier.size(18.dp),
                    colorFilter = tint(colors.ink),
                )
                Column {
                    BasicText(
                        stringResource(Res.string.snowglobe_place),
                        style =
                            TextStyle(
                                colors.ink,
                                15.sp,
                                FontWeight.SemiBold,
                                letterSpacing = (-0.005).em,
                            ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    BasicText(
                        updatedText,
                        style =
                            TextStyle(
                                if (fresh) colors.accent else colors.muted,
                                12.sp,
                                if (fresh) FontWeight.SemiBold else FontWeight.Normal,
                            ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.testTag("updated"),
                    )
                }
            }
            // Now: the day, its temperature and conditions.
            Column(
                Modifier.padding(start = 22.dp, end = 22.dp, top = 6.dp).onGloballyPositioned {
                    headerBottom = it.positionInRoot().y - rootTop + it.size.height
                }
            ) {
                BasicText(
                    stringResource(day.label).uppercase(),
                    style =
                        TextStyle(
                            colors.muted,
                            11.5.sp,
                            FontWeight.SemiBold,
                            letterSpacing = 0.09.em,
                        ),
                    maxLines = 1,
                )
                Row(
                    Modifier.padding(top = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.Bottom,
                ) {
                    BasicText(
                        "${day.high}°",
                        style =
                            TextStyle(
                                colors.ink,
                                64.sp,
                                FontWeight(250),
                                lineHeight = 0.95.em,
                                letterSpacing = (-0.04).em,
                            ),
                        modifier = Modifier.testTag("temperature"),
                    )
                    Column(Modifier.padding(bottom = 6.dp)) {
                        BasicText(
                            conditions,
                            style =
                                TextStyle(
                                    colors.ink,
                                    17.sp,
                                    FontWeight.SemiBold,
                                    letterSpacing = (-0.01).em,
                                ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        BasicText(
                            stringResource(
                                Res.string.snowglobe_extra,
                                day.high,
                                day.low,
                                stringResource(day.note),
                            ),
                            style = TextStyle(colors.muted, 12.5.sp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
            Box(Modifier.weight(1f).fillMaxWidth())
            DayStrip(
                board.days,
                board.selected,
                colors,
                onSelect = ::select,
                rtl = rtl,
                reducedMotion = reducedMotion,
                modifier =
                    Modifier.padding(start = 12.dp, end = 12.dp, bottom = 14.dp)
                        .onGloballyPositioned {
                            daysTop = it.positionInRoot().y - rootTop
                        },
            )
        }

        // The hint under the globe's base.
        if (layout != null) {
            val hint =
                when (globe.hint) {
                    GlobeHint.LetItSettle -> stringResource(Res.string.snowglobe_hint_let_settle)
                    GlobeHint.Settling -> stringResource(Res.string.snowglobe_hint_settling)
                    GlobeHint.KeepShaking -> stringResource(Res.string.snowglobe_hint_keep)
                    GlobeHint.Idle ->
                        if (justUpdated && fresh) {
                            stringResource(Res.string.snowglobe_hint_updated, conditions)
                        } else {
                            stringResource(Res.string.snowglobe_hint_idle)
                        }
                }
            Hint(
                hint,
                colors,
                Modifier.fillMaxWidth().offset { IntOffset(0, layout.hintTop.roundToInt()) },
            )
        }

        // Read out when a refresh lands.
        Box(
            Modifier.size(1.dp).testTag("live").semantics {
                liveRegion = LiveRegionMode.Polite
                if (announcement != null && !board.failed) contentDescription = announcement
            }
        )
    }
}

@Composable
private fun Hint(text: String, colors: SnowGlobeScreenColors, modifier: Modifier) {
    Box(modifier, contentAlignment = Alignment.TopCenter) {
        Row(
            Modifier.testTag("hint")
                .background(colors.chip, RoundedCornerShape(999.dp))
                .border(1.dp, colors.cardLine, RoundedCornerShape(999.dp))
                .padding(start = 11.dp, top = 7.dp, end = 13.dp, bottom = 7.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                rememberVectorPainter(ShakeIcon),
                null,
                Modifier.size(14.dp),
                colorFilter = tint(colors.ink),
            )
            BasicText(
                text,
                style = TextStyle(colors.ink, 12.5.sp, FontWeight.SemiBold),
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun DayStrip(
    days: List<DayForecast>,
    selected: Int,
    colors: SnowGlobeScreenColors,
    onSelect: (Int) -> Unit,
    rtl: Boolean,
    reducedMotion: Boolean,
    modifier: Modifier,
) {
    val strip = RoundedCornerShape(24.dp)
    val pill = RoundedCornerShape(17.dp)
    val gap = 2.dp
    // The highlight slides from the day it leaves to the one chosen, as one piece.
    val at by
        animateFloatAsState(
            selected.toFloat(),
            if (reducedMotion) snap() else spring(dampingRatio = .82f, stiffness = 420f),
            label = "day highlight",
        )
    Box(
        modifier
            .fillMaxWidth()
            .background(colors.card, strip)
            .border(1.dp, colors.cardLine, strip)
            .padding(7.dp)
    ) {
        // Where the row placed each day, left edge and width: the highlight runs between them.
        val placed = remember { mutableStateMapOf<Int, DaySpan>() }
        Box(
            Modifier.matchParentSize()
                .layout { measurable, constraints ->
                    val a = placed[floor(at).toInt()]
                    val b = placed[ceil(at).toInt()] ?: a
                    if (a == null || b == null) return@layout layout(0, 0) {}
                    val f = at - floor(at)
                    val x = (a.left + (b.left - a.left) * f).roundToInt()
                    val w = (a.width + (b.width - a.width) * f).roundToInt()
                    val placeable = measurable.measure(Constraints.fixed(w, constraints.maxHeight))
                    layout(constraints.maxWidth, constraints.maxHeight) { placeable.place(x, 0) }
                }
                .testTag("day-indicator")
                .background(colors.selected, pill)
        )
        DayRow(days, selected, colors, onSelect, rtl, gap, pill) { i, x, w ->
            placed[i] = DaySpan(x, w)
        }
    }
}

/** Where the row placed a day: its [left] edge and [width], in pixels. */
private data class DaySpan(val left: Int, val width: Int)

@Composable
private fun DayRow(
    days: List<DayForecast>,
    selected: Int,
    colors: SnowGlobeScreenColors,
    onSelect: (Int) -> Unit,
    rtl: Boolean,
    gap: Dp,
    pill: Shape,
    onPlaced: (day: Int, left: Int, width: Int) -> Unit,
) {
    Row(
        Modifier.fillMaxWidth()
            .selectableGroup()
            // Left and right move along the days, as the selection follows focus in a tab strip.
            .onPreviewKeyEvent { e ->
                if (e.type == KeyEventType.KeyDown) {
                    val forward = if (rtl) Key.DirectionLeft else Key.DirectionRight
                    val back = if (rtl) Key.DirectionRight else Key.DirectionLeft
                    when (e.key) {
                        forward -> onSelect(selected + 1)
                        back -> onSelect(selected - 1)
                    }
                }
                false
            },
        horizontalArrangement = Arrangement.spacedBy(gap),
    ) {
        days.forEachIndexed { i, d ->
            val on = i == selected
            val label =
                stringResource(
                    Res.string.snowglobe_day_label,
                    stringResource(d.label),
                    stringResource(d.conditions),
                    d.high,
                    d.low,
                )
            val dayInk by animateColorAsState(if (on) colors.ink else colors.muted, label = "day")
            Column(
                Modifier.weight(1f)
                    .heightIn(min = 44.dp)
                    .onPlaced { onPlaced(i, it.positionInParent().x.roundToInt(), it.size.width) }
                    // The press ripple stays inside the day's pill.
                    .clip(pill)
                    .selectable(selected = on, role = Role.Tab, onClick = { onSelect(i) })
                    .semantics { contentDescription = label }
                    .testTag("day-$i")
                    .padding(top = 8.dp, bottom = 7.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                BasicText(
                    stringResource(d.short),
                    style = TextStyle(dayInk, 12.sp, FontWeight.SemiBold),
                    maxLines = 1,
                )
                Image(rememberVectorPainter(WeatherIcons.of(d.scene)), null, Modifier.size(26.dp))
                BasicText("${d.high}°", style = TextStyle(colors.ink, 14.sp, FontWeight.SemiBold))
                BasicText(
                    "${d.low}°",
                    style = TextStyle(colors.muted, 11.5.sp),
                    modifier = Modifier.offset(y = (-2).dp),
                )
            }
        }
    }
}

private fun tint(color: Color) = androidx.compose.ui.graphics.ColorFilter.tint(color)

// The prototype's location pin and the hint's double arrow, drawn in the current ink.
private val PinIcon: ImageVector by lazy {
    ImageVector.Builder("Pin", 24.dp, 24.dp, 24f, 24f)
        .apply {
            addPath(
                addPathNodes("M12 21s-6.5-5.6-6.5-11a6.5 6.5 0 0 1 13 0c0 5.4-6.5 11-6.5 11z"),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            )
            addPath(
                addPathNodes("M12 7.7a2.3 2.3 0 1 0 0.01 0z"),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
            )
        }
        .build()
}

private val ShakeIcon: ImageVector by lazy {
    ImageVector.Builder("Shake", 16.dp, 16.dp, 16f, 16f)
        .apply {
            addPath(
                addPathNodes("M2 8h12M4.5 5.2 2 8l2.5 2.8M11.5 5.2 14 8l-2.5 2.8"),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.7f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            )
        }
        .build()
}

/**
 * A fingertip on the globe, swung side to side for [duration] at about 2.5 swings a second, and
 * lifted. [show] places it, as a fraction of the glass's radius, or hides it with null.
 */
private suspend fun scriptedShake(
    globe: SnowGlobeState,
    duration: Duration,
    show: (Float?) -> Unit,
) {
    // The prototype's demo: a grip at 0.35 r right of centre, swinging 64 px at 3.4 Hz, easing in
    // over the first stretch and out over the last 15 %.
    val frames = (duration.inWholeMilliseconds / 16).toInt().coerceAtLeast(1)
    var previous = 0f
    try {
        for (i in 0..frames) {
            val p = i.toFloat() / frames
            val out = if (p < .85f) 1f else 1f - smoothstep((p - .85f) / .15f)
            val x =
                64f *
                    globe.scale *
                    sin(p * 2f * PI.toFloat() * 3.4f) *
                    sin(min(1f, p * 1.6f) * PI.toFloat() / 2f) *
                    out
            globe.drag(x - previous)
            previous = x
            show(.35f + globe.offset.x / globe.radius)
            delay(16)
        }
    } finally {
        globe.release()
        show(null)
    }
}

private fun smoothstep(t: Float) = t * t * (3 - 2 * t)
