package dev.dimvlachos.lab.photocopierdemo.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import dev.dimvlachos.lab.core.demo.DemoState
import dev.dimvlachos.lab.core.demo.demoInsets
import dev.dimvlachos.lab.core.platform.rememberReducedMotion
import dev.dimvlachos.lab.core.platform.rememberSoundPlayer
import dev.dimvlachos.lab.core.presentation.components.photocopier.CopierItem
import dev.dimvlachos.lab.core.presentation.components.photocopier.CopierTitleSlot
import dev.dimvlachos.lab.core.presentation.components.photocopier.CopierWords
import dev.dimvlachos.lab.core.presentation.components.photocopier.PhotocopierList
import dev.dimvlachos.lab.core.presentation.components.photocopier.PhotocopierSounds
import dev.dimvlachos.lab.core.presentation.components.photocopier.PhotocopierState
import dev.dimvlachos.lab.core.presentation.components.touch.drawTouch
import dev.dimvlachos.lab.core.presentation.ui.LabTheme
import dev.dimvlachos.lab.core.presentation.ui.PhotocopierScreenColors
import dev.dimvlachos.lab.photocopierdemo.PhotocopierHand
import dev.dimvlachos.lab.resources.Res
import dev.dimvlachos.lab.resources.photocopier_copy
import dev.dimvlachos.lab.resources.photocopier_count
import dev.dimvlachos.lab.resources.photocopier_duplicate
import dev.dimvlachos.lab.resources.photocopier_duplicated
import dev.dimvlachos.lab.resources.photocopier_item_1
import dev.dimvlachos.lab.resources.photocopier_item_1_meta
import dev.dimvlachos.lab.resources.photocopier_item_1_summary
import dev.dimvlachos.lab.resources.photocopier_item_2
import dev.dimvlachos.lab.resources.photocopier_item_2_meta
import dev.dimvlachos.lab.resources.photocopier_item_2_summary
import dev.dimvlachos.lab.resources.photocopier_item_3
import dev.dimvlachos.lab.resources.photocopier_item_3_meta
import dev.dimvlachos.lab.resources.photocopier_item_3_summary
import dev.dimvlachos.lab.resources.photocopier_item_4
import dev.dimvlachos.lab.resources.photocopier_item_4_meta
import dev.dimvlachos.lab.resources.photocopier_item_4_summary
import dev.dimvlachos.lab.resources.photocopier_item_5
import dev.dimvlachos.lab.resources.photocopier_item_5_meta
import dev.dimvlachos.lab.resources.photocopier_item_5_summary
import dev.dimvlachos.lab.resources.photocopier_item_6
import dev.dimvlachos.lab.resources.photocopier_item_6_meta
import dev.dimvlachos.lab.resources.photocopier_item_6_summary
import dev.dimvlachos.lab.resources.photocopier_just_now
import dev.dimvlachos.lab.resources.photocopier_list
import dev.dimvlachos.lab.resources.photocopier_menu
import dev.dimvlachos.lab.resources.photocopier_more
import dev.dimvlachos.lab.resources.photocopier_pin
import dev.dimvlachos.lab.resources.photocopier_said_duplicated
import dev.dimvlachos.lab.resources.photocopier_said_pinned
import dev.dimvlachos.lab.resources.photocopier_said_removed
import dev.dimvlachos.lab.resources.photocopier_search
import dev.dimvlachos.lab.resources.photocopier_title
import dev.dimvlachos.lab.resources.photocopier_undo
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/** The prototype's six lists of the Parker household, in its order. */
@Composable
internal fun rememberPhotocopier(): PhotocopierState {
    val items =
        listOf(
            CopierItem(
                1,
                stringResource(Res.string.photocopier_item_1),
                stringResource(Res.string.photocopier_item_1_summary),
                stringResource(Res.string.photocopier_item_1_meta),
            ),
            CopierItem(
                2,
                stringResource(Res.string.photocopier_item_2),
                stringResource(Res.string.photocopier_item_2_summary),
                stringResource(Res.string.photocopier_item_2_meta),
            ),
            CopierItem(
                3,
                stringResource(Res.string.photocopier_item_3),
                stringResource(Res.string.photocopier_item_3_summary),
                stringResource(Res.string.photocopier_item_3_meta),
            ),
            CopierItem(
                4,
                stringResource(Res.string.photocopier_item_4),
                stringResource(Res.string.photocopier_item_4_summary),
                stringResource(Res.string.photocopier_item_4_meta),
            ),
            CopierItem(
                5,
                stringResource(Res.string.photocopier_item_5),
                stringResource(Res.string.photocopier_item_5_summary),
                stringResource(Res.string.photocopier_item_5_meta),
            ),
            CopierItem(
                6,
                stringResource(Res.string.photocopier_item_6),
                stringResource(Res.string.photocopier_item_6_summary),
                stringResource(Res.string.photocopier_item_6_meta),
            ),
        )
    val copy = stringResource(Res.string.photocopier_copy)
    val justNow = stringResource(Res.string.photocopier_just_now)
    return remember { PhotocopierState(items, copy, justNow) }
}

/**
 * The prototype's household lists app: the Parker family's lists, whose Duplicate runs a list
 * through a little office copier. The script's presses and drags go through the same calls a finger
 * makes.
 */
@Composable
internal fun PhotocopierDemo(
    state: DemoState,
    copier: PhotocopierState = rememberPhotocopier(),
    reducedMotion: Boolean = rememberReducedMotion(),
) {
    val dark = LabTheme.isDark
    val colors = if (dark) PhotocopierScreenColors.Dark else PhotocopierScreenColors.Light
    val sounds = rememberSoundPlayer(PhotocopierSounds)
    val hand = remember(copier) { PhotocopierHand(copier) }
    // The script's steps take turns, as the prototype awaits each: on a slow device a step that
    // runs late holds the next back rather than steering the fingertip at the same time.
    val turn = remember { Mutex() }
    DisposableEffect(state, hand) {
        suspend fun step(block: suspend () -> Unit) {
            try {
                turn.withLock { block() }
            } catch (e: CancellationException) {
                hand.stop()
                throw e
            }
        }
        state.setControlTapHandler { control -> step { hand.play(control) } }
        // Puts the six lists back as they began if a stopped run left a copy, so the clip loops.
        state.setFeedResetHandler {
            step {
                hand.hide()
                if (!copier.asBegun) copier.reset()
            }
        }
        onDispose {
            state.setControlTapHandler(null)
            state.setFeedResetHandler(null)
        }
    }
    // A script the user takes over leaves no fingertip behind.
    LaunchedEffect(state.replay) { if (!state.replay) hand.stop() }

    val words =
        CopierWords(
            duplicate = stringResource(Res.string.photocopier_duplicate),
            pin = stringResource(Res.string.photocopier_pin),
            duplicated = stringResource(Res.string.photocopier_duplicated),
            undo = stringResource(Res.string.photocopier_undo),
            menu = stringResource(Res.string.photocopier_menu),
            list = stringResource(Res.string.photocopier_list),
            moreFor = stringResource(Res.string.photocopier_more, CopierTitleSlot),
            saidDuplicated =
                stringResource(Res.string.photocopier_said_duplicated, CopierTitleSlot),
            saidRemoved = stringResource(Res.string.photocopier_said_removed),
            saidPinned = stringResource(Res.string.photocopier_said_pinned, CopierTitleSlot),
        )
    val touch = LabTheme.colors.touch
    Box(Modifier.fillMaxSize().background(colors.background)) {
        Box(Modifier.fillMaxSize().windowInsetsPadding(demoInsets(state))) {
            PhotocopierList(
                copier,
                words,
                colors,
                dark,
                sounds,
                Modifier.fillMaxSize(),
                reducedMotion,
            ) {
                AppBar(colors, copier.cards.size)
                Search(colors)
            }
            Canvas(Modifier.fillMaxSize()) {
                val a = hand.shown
                if (a <= 0f) return@Canvas
                val at = hand.at * density
                val k = if (hand.down) .82f else 1f
                scale(k, k, pivot = at) { drawTouch(touch, at, a) }
            }
        }
    }
}

private val CssLines = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.None)
private val BodyLine = 1.55.em

/** The bar: the app's title, whose lists they are and how many, and the user's avatar. */
@Composable
private fun AppBar(colors: PhotocopierScreenColors, count: Int) {
    Row(
        Modifier.fillMaxWidth().padding(start = 18.dp, end = 18.dp, top = 18.dp, bottom = 6.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Column(Modifier.weight(1f)) {
            BasicText(
                stringResource(Res.string.photocopier_title),
                Modifier.semantics { heading() },
                style =
                    TextStyle(
                        lineHeightStyle = CssLines,
                        color = colors.ink,
                        fontSize = 28.sp,
                        lineHeight = 1.05.em,
                        fontWeight = FontWeight(750),
                        letterSpacing = (-.02).em,
                    ),
            )
            BasicText(
                pluralStringResource(Res.plurals.photocopier_count, count, count),
                Modifier.padding(top = 3.dp).testTag("count"),
                style =
                    TextStyle(
                        lineHeightStyle = CssLines,
                        color = colors.muted,
                        fontSize = 12.5.sp,
                        lineHeight = BodyLine,
                        fontWeight = FontWeight.Medium,
                    ),
            )
        }
        Box(
            Modifier.size(34.dp)
                .clip(CircleShape)
                .background(PhotocopierScreenColors.Avatar)
                .clearAndSetSemantics {},
            contentAlignment = Alignment.Center,
        ) {
            BasicText(
                "MP",
                style =
                    TextStyle(
                        lineHeightStyle = CssLines,
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                    ),
            )
        }
    }
}

@Composable
private fun Search(colors: PhotocopierScreenColors) {
    Row(
        Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 10.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(colors.chip)
            .padding(horizontal = 12.dp, vertical = 9.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Canvas(Modifier.size(16.dp)) {
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
        BasicText(
            stringResource(Res.string.photocopier_search),
            style =
                TextStyle(
                    lineHeightStyle = CssLines,
                    color = colors.muted,
                    fontSize = 14.sp,
                    lineHeight = BodyLine,
                ),
        )
    }
}
