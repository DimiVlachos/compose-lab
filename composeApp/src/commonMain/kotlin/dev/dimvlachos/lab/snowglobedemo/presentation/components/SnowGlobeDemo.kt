package dev.dimvlachos.lab.snowglobedemo.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.dimvlachos.lab.core.demo.DemoState
import dev.dimvlachos.lab.core.demo.demoInsets
import dev.dimvlachos.lab.core.platform.Sound
import dev.dimvlachos.lab.core.platform.rememberReducedMotion
import dev.dimvlachos.lab.core.platform.rememberSoundPlayer
import dev.dimvlachos.lab.core.presentation.components.snowglobe.SnowGlobe
import dev.dimvlachos.lab.core.presentation.components.snowglobe.SnowGlobeState
import dev.dimvlachos.lab.core.presentation.components.snowglobe.rememberSnowGlobeState
import dev.dimvlachos.lab.core.presentation.components.touch.drawTouch
import dev.dimvlachos.lab.core.presentation.ui.LabTheme
import dev.dimvlachos.lab.resources.Res
import dev.dimvlachos.lab.resources.snowglobe_announce
import dev.dimvlachos.lab.resources.snowglobe_day_tomorrow
import dev.dimvlachos.lab.resources.snowglobe_failed
import dev.dimvlachos.lab.resources.snowglobe_high_low
import dev.dimvlachos.lab.resources.snowglobe_place
import dev.dimvlachos.lab.resources.snowglobe_refresh
import dev.dimvlachos.lab.resources.snowglobe_updated
import dev.dimvlachos.lab.snowglobedemo.FakeForecastSource
import dev.dimvlachos.lab.snowglobedemo.WeatherBoard
import dev.dimvlachos.lab.snowglobedemo.rememberWeatherBoard
import kotlin.math.PI
import kotlin.math.min
import kotlin.math.sin
import kotlin.time.Duration
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

// The globe's shape, as the component draws it: its width over its height, and the glass's centre
// and radius as fractions of its width.
private const val GlobeAspect = 0.86f
private const val GlassCenterYOfWidth = 0.46f
private const val GlassRadiusOfWidth = 0.42f

/**
 * A weather screen for Wellfleet built round a snow globe of its lighthouse. Shaking the globe
 * refreshes the forecast, which lands once the glitter has settled; swiping across the screen
 * outside the globe, tapping a day or moving along the days with the arrow keys carries the globe
 * to that day's weather. The script's shake is played by a fingertip drawn over the globe, through
 * the same drag a finger gives it.
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
    // The scripted fingertip, across the glass as a fraction of its radius, or null when lifted.
    // Only the drawing reads it, so the shake doesn't recompose the screen.
    val fingertip = remember { mutableStateOf<Float?>(null) }

    fun select(index: Int) {
        val direction = board.select(index)
        if (direction != 0) globe.rock(direction)
    }

    DisposableEffect(state, board, globe) {
        state.setGlobeShakeHandler { duration ->
            scriptedShake(globe, duration) { fingertip.value = it }
        }
        state.setControlTapHandler { control ->
            control.removePrefix("day-").toIntOrNull()?.let(::select)
        }
        state.setFeedResetHandler {
            board.reset()
            source.reset()
        }
        onDispose {
            state.setGlobeShakeHandler(null)
            state.setControlTapHandler(null)
            state.setFeedResetHandler(null)
        }
    }

    val colors = LabTheme.colors
    val type = LabTheme.typography
    val day = board.selectedDay
    val conditions = stringResource(day.conditions)
    val change = board.lastChange
    val announcement = change?.let {
        stringResource(
            Res.string.snowglobe_announce,
            stringResource(Res.string.snowglobe_day_tomorrow),
            stringResource(it.conditions),
            it.high,
        )
    }
    val updatedText =
        if (board.failed) stringResource(Res.string.snowglobe_failed)
        else stringResource(Res.string.snowglobe_updated, board.updated)

    Column(
        Modifier.fillMaxSize()
            .background(colors.background)
            .windowInsetsPadding(demoInsets(state))
            // The globe consumes its own drags, so only a swipe outside it changes the day.
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
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            stringResource(Res.string.snowglobe_place),
            style = type.title,
            color = colors.textPrimary,
        )
        Text(
            updatedText,
            style = type.caption,
            color = if (board.failed) colors.textPrimary else colors.textMuted,
            modifier =
                Modifier.testTag("updated").semantics {
                    liveRegion = LiveRegionMode.Polite
                    if (announcement != null && !board.failed) contentDescription = announcement
                },
        )
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                "${day.high}°",
                fontSize = 56.sp,
                fontWeight = FontWeight.Light,
                color = colors.textPrimary,
                modifier = Modifier.testTag("temperature"),
            )
            Column(Modifier.padding(bottom = 10.dp)) {
                Text(conditions, style = type.subtitle, color = colors.textPrimary)
                Text(
                    stringResource(Res.string.snowglobe_high_low, day.high, day.low) +
                        " · " +
                        stringResource(day.note),
                    style = type.caption,
                    color = colors.textMuted,
                )
            }
        }
        Box(
            Modifier.weight(1f).fillMaxWidth().drawWithContent {
                drawContent()
                val x = fingertip.value ?: return@drawWithContent
                // The globe keeps its aspect, centred in this box; the fingertip sits on the
                // glass's centre, carried as far as the drag carries the glass.
                val width = min(size.width, size.height * GlobeAspect)
                val left = (size.width - width) / 2f
                val top = (size.height - width / GlobeAspect) / 2f
                val at =
                    Offset(
                        left + width / 2f + x * width * GlassRadiusOfWidth,
                        top + width * GlassCenterYOfWidth,
                    )
                drawTouch(colors.touch, at, 1f)
            },
            contentAlignment = Alignment.Center,
        ) {
            SnowGlobe(
                scene = day.scene,
                refreshPending = board.refreshPending,
                onShaken = {
                    if (board.startRefresh()) scope.launch { board.finishRefresh(source.refresh()) }
                },
                onSettled = { if (!board.failed) sounds.play(Sound.MusicBoxPhrase, 0.8f) },
                state = globe,
                sounds = sounds,
                shakeSensorEnabled = !state.recording,
                description = stringResource(Res.string.snowglobe_refresh),
                reducedMotion = reducedMotion,
                stateDescription = conditions,
            )
        }
        PrimaryTabRow(
            selectedTabIndex = board.selected,
            containerColor = colors.background,
            contentColor = colors.textPrimary,
            indicator = {
                TabRowDefaults.PrimaryIndicator(
                    Modifier.tabIndicatorOffset(board.selected),
                    color = colors.accent,
                )
            },
            divider = {},
            // Left and right move along the days, as the selection follows focus in a tab row.
            modifier =
                Modifier.onPreviewKeyEvent { e ->
                    if (e.type == KeyEventType.KeyDown) {
                        val forward = if (rtl) Key.DirectionLeft else Key.DirectionRight
                        val back = if (rtl) Key.DirectionRight else Key.DirectionLeft
                        when (e.key) {
                            forward -> select(board.selected + 1)
                            back -> select(board.selected - 1)
                        }
                    }
                    // Not consumed: focus moves along with the selection.
                    false
                },
        ) {
            board.days.forEachIndexed { i, d ->
                Tab(
                    selected = i == board.selected,
                    onClick = { select(i) },
                    selectedContentColor = colors.textPrimary,
                    unselectedContentColor = colors.textMuted,
                    modifier = Modifier.testTag("day-$i"),
                    text = { Text(stringResource(d.day), style = type.label) },
                )
            }
        }
    }
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
    val frames = (duration.inWholeMilliseconds / 16).toInt()
    var previous = 0f
    try {
        repeat(frames) { i ->
            val t = i * 0.016f
            val x = 0.55f * sin(t * 2.5f * 2f * PI.toFloat())
            globe.drag(Offset(x - previous, 0f))
            previous = x
            show(x)
            delay(16)
        }
    } finally {
        globe.release()
        show(null)
    }
}
