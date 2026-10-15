package dev.dimvlachos.lab.smokedemo.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.platform.LocalAccessibilityManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import dev.dimvlachos.lab.core.demo.DemoState
import dev.dimvlachos.lab.core.demo.demoInsets
import dev.dimvlachos.lab.core.platform.Sound
import dev.dimvlachos.lab.core.platform.rememberSoundPlayer
import dev.dimvlachos.lab.core.presentation.components.smoke.SmokeAction
import dev.dimvlachos.lab.core.presentation.components.smoke.SmokeMaterial
import dev.dimvlachos.lab.core.presentation.components.smoke.SmokeOverlay
import dev.dimvlachos.lab.core.presentation.components.smoke.SmokeRow
import dev.dimvlachos.lab.core.presentation.components.smoke.SmokeSide
import dev.dimvlachos.lab.core.presentation.components.smoke.SmokeState
import dev.dimvlachos.lab.core.presentation.components.smoke.rememberSmokeState
import dev.dimvlachos.lab.core.presentation.components.touch.ScriptedTouch
import dev.dimvlachos.lab.core.presentation.components.touch.drawTouch
import dev.dimvlachos.lab.core.presentation.ui.LabTheme
import dev.dimvlachos.lab.core.presentation.ui.isWideStage
import dev.dimvlachos.lab.resources.Res
import dev.dimvlachos.lab.resources.smoke_archive
import dev.dimvlachos.lab.resources.smoke_archive_empty
import dev.dimvlachos.lab.resources.smoke_archived
import dev.dimvlachos.lab.resources.smoke_archived_one
import dev.dimvlachos.lab.resources.smoke_archived_row
import dev.dimvlachos.lab.resources.smoke_back
import dev.dimvlachos.lab.resources.smoke_chats
import dev.dimvlachos.lab.resources.smoke_clear_read
import dev.dimvlachos.lab.resources.smoke_cleared
import dev.dimvlachos.lab.resources.smoke_delete
import dev.dimvlachos.lab.resources.smoke_deleted
import dev.dimvlachos.lab.resources.smoke_earlier
import dev.dimvlachos.lab.resources.smoke_hint
import dev.dimvlachos.lab.resources.smoke_hint_archive
import dev.dimvlachos.lab.resources.smoke_restore
import dev.dimvlachos.lab.resources.smoke_restored
import dev.dimvlachos.lab.resources.smoke_title
import dev.dimvlachos.lab.resources.smoke_today
import dev.dimvlachos.lab.resources.smoke_undo
import dev.dimvlachos.lab.smokedemo.Chat
import dev.dimvlachos.lab.smokedemo.ChatChange
import dev.dimvlachos.lab.smokedemo.ChatDay
import dev.dimvlachos.lab.smokedemo.ChatPlace
import dev.dimvlachos.lab.smokedemo.CycladesChats
import dev.dimvlachos.lab.smokedemo.CycladesInbox
import dev.dimvlachos.lab.smokedemo.InboxAction
import dev.dimvlachos.lab.smokedemo.SmokeDemos
import dev.dimvlachos.lab.smokedemo.rememberCycladesInbox
import kotlin.time.Duration
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

// How long the snackbar offers Undo, at least.
private const val UndoMs = 5_000L

// A wave of rows clearing starts each this long after the last; undo breathes them back in turn.
private const val WaveSeconds = 0.12f
private const val UndoWaveSeconds = 0.07f

// The list is at most this wide, in the middle of a wide stage.
private val ListWidth = 640.dp

/**
 * A Cyclades chat list whose chats go up in smoke. Swipe one left and it burns away; right and it
 * goes in mist to the archive, which the row on top opens, and where a swipe right brings one back.
 * Clear read burns every read chat in a wave, and the snackbar's Undo breathes the smoke of the
 * last of these back in. The script's swipes and taps are played by a fingertip drawn on the list,
 * through the same drag the rows take from a finger.
 */
@Composable
internal fun SmokeDemo(
    state: DemoState,
    inbox: CycladesInbox = rememberCycladesInbox(),
    smoke: SmokeState =
        rememberSmokeState(
            rememberSoundPlayer(setOf(Sound.BurnLoop, Sound.MistWhoosh, Sound.Inhale))
        ),
) {
    // Not saved, as the chats and their smoke aren't: a screen made again starts in the inbox.
    var archive by remember { mutableStateOf(false) }
    // The system back gesture leaves the archive for the inbox; in the inbox it is the screen's.
    NavigationBackHandler(
        state = rememberNavigationEventState(NavigationEventInfo.None),
        isBackEnabled = archive,
        onBackCompleted = { archive = false },
    )
    val finger = remember { ChatFinger() }
    val targets = remember { Targets() }
    val density = LocalDensity.current.density
    val place = if (archive) ChatPlace.Archived else ChatPlace.Inbox
    val currentPlace by rememberUpdatedState(place)

    fun undo() {
        val action = inbox.undo() ?: return
        // The smoke comes back only into a row on this screen; elsewhere the chat just returns.
        val here = action.from == currentPlace
        action.ids.reversed().forEachIndexed { i, id ->
            if (!here || !smoke.reform(id, delay = i * UndoWaveSeconds)) smoke.forget(id)
        }
    }

    fun clearRead() {
        if (archive || inbox.isClearing) return
        // Only the chats the smoke takes are in the clear: one already burning isn't.
        val going = mutableListOf<String>()
        for (chat in inbox.readInInbox()) {
            if (smoke.swipeAway(chat.id, SmokeSide.Left, delay = going.size * WaveSeconds)) {
                going += chat.id
            }
        }
        inbox.clearing(going)
    }

    DisposableEffect(state, inbox, smoke) {
        state.setChatSwipeHandler { id, by, duration ->
            finger.swipe(smoke, id, by, duration, density)
        }
        state.setControlTapHandler { control ->
            when (control) {
                SmokeDemos.Undo -> finger.tap(targets.centre(targets.undo)) { undo() }
                SmokeDemos.ClearRead -> finger.tap(targets.centre(targets.clear)) { clearRead() }
            }
        }
        state.setFeedResetHandler {
            inbox.reset()
            smoke.clear()
            archive = false
        }
        onDispose {
            state.setChatSwipeHandler(null)
            state.setControlTapHandler(null)
            state.setFeedResetHandler(null)
        }
    }

    // The snackbar offers Undo for a while; once it goes, or the next action takes its place,
    // the smoke kept for the last can go.
    val last = inbox.last
    // Long enough to reach Undo with a screen reader, or however long the person has asked for.
    val accessibility = LocalAccessibilityManager.current
    val undoMs =
        accessibility?.calculateRecommendedTimeoutMillis(
            UndoMs,
            containsIcons = false,
            containsText = true,
            containsControls = true,
        ) ?: UndoMs
    var kept by remember { mutableStateOf<InboxAction?>(null) }
    LaunchedEffect(last) {
        kept?.let { old -> if (old !== last) old.ids.forEach(smoke::forget) }
        kept = last
        if (last != null) {
            delay(undoMs)
            inbox.settle(last.serial)
        }
    }

    val colors = LabTheme.colors
    val touch = colors.touch
    val insets = demoInsets(state).asPaddingValues()
    val direction = LocalLayoutDirection.current
    BoxWithConstraints(
        Modifier.fillMaxSize()
            .background(colors.background)
            .onPlaced { targets.stage = it }
            .drawWithContent {
                drawContent()
                with(finger) { drawFinger(touch) }
            }
    ) {
        val wide = isWideStage(maxWidth, maxHeight)
        SmokeOverlay(smoke, Modifier.fillMaxSize()) {
            Column(
                Modifier.fillMaxSize()
                    .padding(
                        start = insets.calculateStartPadding(direction),
                        end = insets.calculateEndPadding(direction),
                    )
            ) {
                Header(
                    archive = archive,
                    count = inbox.chatsIn(place).size,
                    canClear = !archive && !inbox.isClearing && inbox.readInInbox().isNotEmpty(),
                    onClear = ::clearRead,
                    onBack = { archive = false },
                    onClearPlaced = { targets.clear = it },
                    wide = wide,
                )
                Column(
                    Modifier.fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .testTag("chats"),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Column(Modifier.widthIn(max = ListWidth).padding(horizontal = 14.dp)) {
                        ChatList(inbox, smoke, place, onOpenArchive = { archive = true })
                        Text(
                            stringResource(
                                when {
                                    archive && inbox.chatsIn(ChatPlace.Archived).isEmpty() ->
                                        Res.string.smoke_archive_empty
                                    archive -> Res.string.smoke_hint_archive
                                    else -> Res.string.smoke_hint
                                }
                            ),
                            style = LabTheme.typography.label,
                            color = colors.textMuted,
                            modifier =
                                Modifier.align(Alignment.CenterHorizontally)
                                    .padding(top = LabTheme.spacing.medium),
                        )
                        // Room to scroll the last chat clear of the snackbar.
                        Spacer(Modifier.height(SnackbarRoom + insets.calculateBottomPadding()))
                    }
                }
            }
        }
        UndoSnackbar(
            action = last,
            onUndo = ::undo,
            onUndoPlaced = { targets.undo = it },
            modifier =
                Modifier.align(Alignment.BottomCenter)
                    .windowInsetsPadding(demoInsets(state))
                    .widthIn(max = ListWidth)
                    .padding(LabTheme.spacing.medium),
        )
    }
}

// The title over the list, with how many chats it holds, and Clear read; in the archive, a way
// back instead. On a wide stage it takes one line.
@Composable
private fun Header(
    archive: Boolean,
    count: Int,
    canClear: Boolean,
    onClear: () -> Unit,
    onBack: () -> Unit,
    onClearPlaced: (LayoutCoordinates) -> Unit,
    wide: Boolean,
) {
    val colors = LabTheme.colors
    val spacing = LabTheme.spacing
    Row(
        Modifier.fillMaxWidth()
            .padding(
                start = spacing.mediumLarge,
                end = spacing.mediumLarge,
                top = if (wide) spacing.small else spacing.mediumLarge,
                bottom = spacing.extraSmall,
            ),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(spacing.smallMedium),
    ) {
        if (archive) {
            val back = stringResource(Res.string.smoke_back)
            Box(
                Modifier.clip(LabTheme.shapes.small)
                    .clickable(role = Role.Button, onClick = onBack)
                    .semantics { contentDescription = back }
                    .padding(BackPadding)
            ) {
                Chevron(colors.accent, pointsRight = false, Modifier.size(BackGlyph))
            }
        }
        val title =
            stringResource(if (archive) Res.string.smoke_archived else Res.string.smoke_title)
        val chats = pluralStringResource(Res.plurals.smoke_chats, count, count)
        if (wide) {
            Row(
                Modifier.weight(1f),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(spacing.smallMedium),
            ) {
                Text(
                    title,
                    style = LabTheme.typography.title,
                    color = colors.textPrimary,
                    modifier = Modifier.alignByBaseline().semantics { heading() },
                )
                Text(
                    chats,
                    style = SubtitleStyle,
                    color = colors.textMuted,
                    modifier = Modifier.alignByBaseline(),
                )
            }
        } else {
            Column(Modifier.weight(1f)) {
                Text(
                    title,
                    style = LabTheme.typography.title,
                    color = colors.textPrimary,
                    modifier = Modifier.semantics { heading() },
                )
                Text(chats, style = SubtitleStyle, color = colors.textMuted)
            }
        }
        if (!archive) {
            Text(
                stringResource(Res.string.smoke_clear_read),
                style = ActionStyle,
                color = if (canClear) colors.accent else colors.textMuted.copy(alpha = 0.5f),
                modifier =
                    Modifier.onPlaced(onClearPlaced)
                        .clip(LabTheme.shapes.small)
                        .clickable(enabled = canClear, role = Role.Button, onClick = onClear)
                        .padding(horizontal = spacing.extraSmall, vertical = 6.dp),
            )
        }
    }
}

// The chats of [place]: in the inbox, the archive's row on top and the chats by day, each day's
// heading going with its last chat; in the archive, its chats in one group. Every chat has a row
// in both, standing only where it is, so one that comes back opens in its place.
@Composable
private fun ChatList(
    inbox: CycladesInbox,
    smoke: SmokeState,
    place: ChatPlace,
    onOpenArchive: () -> Unit,
) {
    val spacing = LabTheme.spacing
    if (place == ChatPlace.Inbox) {
        val archived = inbox.chatsIn(ChatPlace.Archived).size
        AnimatedVisibility(
            visible = archived > 0,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut(),
        ) {
            ArchivedRow(
                stringResource(Res.string.smoke_archived_row, archived),
                Modifier.padding(top = spacing.small)
                    .clip(LabTheme.shapes.large)
                    .clickable(role = Role.Button, onClick = onOpenArchive)
                    .testTag("archived"),
            )
        }
        for (day in ChatDay.entries) {
            val chats = CycladesChats.all.filter { it.section == day }
            AnimatedVisibility(
                visible = chats.any { inbox.placeOf(it.id) == place },
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                SectionHeading(
                    stringResource(
                        if (day == ChatDay.Today) Res.string.smoke_today
                        else Res.string.smoke_earlier
                    )
                )
            }
            ChatGroup(chats, inbox, smoke, place)
        }
    } else {
        Spacer(Modifier.height(spacing.small))
        ChatGroup(CycladesChats.all, inbox, smoke, place)
    }
}

// One rounded group of chats. A chat's left swipe deletes it; its right swipe archives it, or,
// in the archive, restores it.
@Composable
private fun ChatGroup(
    chats: List<Chat>,
    inbox: CycladesInbox,
    smoke: SmokeState,
    place: ChatPlace,
) {
    val delete = stringResource(Res.string.smoke_delete)
    val archive = stringResource(Res.string.smoke_archive)
    val restore = stringResource(Res.string.smoke_restore)
    Column(Modifier.clip(LabTheme.shapes.large)) {
        var above = false
        for (chat in chats) {
            val present = inbox.placeOf(chat.id) == place
            val divider = above
            if (present) above = true
            key(chat.id) {
                SmokeRow(
                    smoke,
                    chat.id,
                    present = present,
                    left = SmokeAction(delete, SmokeMaterial.Smoke) { inbox.deleted(chat.id) },
                    right =
                        if (place == ChatPlace.Inbox) {
                            SmokeAction(archive, SmokeMaterial.Mist) { inbox.archived(chat.id) }
                        } else {
                            SmokeAction(restore, SmokeMaterial.Mist) { inbox.restored(chat.id) }
                        },
                ) {
                    ChatRow(chat, divider)
                }
            }
        }
    }
}

/**
 * The snackbar for the last [action]: what happened, and Undo. It says what happened politely to a
 * screen reader as it comes up, and keeps its words as it goes.
 */
@Composable
private fun UndoSnackbar(
    action: InboxAction?,
    onUndo: () -> Unit,
    onUndoPlaced: (LayoutCoordinates) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LabTheme.colors
    val words = remember { Words() }
    if (action != null) words.text = snackbarText(action)
    AnimatedVisibility(
        visible = action != null,
        modifier = modifier,
        enter = fadeIn(tween(200)) + slideInVertically(tween(250)) { it / 3 },
        exit = fadeOut(tween(200)) + slideOutVertically(tween(250)) { it / 3 },
    ) {
        Row(
            Modifier.fillMaxWidth()
                .shadow(SnackbarShadow, LabTheme.shapes.medium)
                .background(colors.smokeSnackbar, LabTheme.shapes.medium)
                .padding(start = LabTheme.spacing.medium, end = LabTheme.spacing.small),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                words.text,
                style = SnackbarStyle,
                color = colors.onSmokeSnackbar,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier =
                    Modifier.weight(1f).padding(vertical = 14.dp).semantics {
                        liveRegion = LiveRegionMode.Polite
                    },
            )
            Text(
                stringResource(Res.string.smoke_undo),
                style = SnackbarStyle.copy(fontWeight = FontWeight.SemiBold),
                color = colors.smokeSnackbarAction,
                modifier =
                    Modifier.onPlaced(onUndoPlaced)
                        .clip(LabTheme.shapes.small)
                        .clickable(role = Role.Button, onClick = onUndo)
                        .padding(horizontal = 10.dp, vertical = 12.dp),
            )
        }
    }
}

private class Words {
    var text = ""
}

@Composable
private fun snackbarText(action: InboxAction): String {
    val first = CycladesChats.all.firstOrNull { it.id == action.ids.firstOrNull() }
    val name = first?.let { stringResource(it.name) }.orEmpty()
    return when (action.change) {
        ChatChange.Deleted -> stringResource(Res.string.smoke_deleted, name)
        ChatChange.Archived -> stringResource(Res.string.smoke_archived_one, name)
        ChatChange.Restored -> stringResource(Res.string.smoke_restored, name)
        ChatChange.Cleared ->
            pluralStringResource(Res.plurals.smoke_cleared, action.ids.size, action.ids.size)
    }
}

// Where the script's taps land: the stage, and the buttons on it.
private class Targets {
    var stage: LayoutCoordinates? = null
    var undo: LayoutCoordinates? = null
    var clear: LayoutCoordinates? = null

    /** The middle of [target] on the stage, if both are laid out. */
    fun centre(target: LayoutCoordinates?): Offset? {
        val stage = stage ?: return null
        if (target == null || !target.isAttached || !stage.isAttached) return null
        val size = target.size
        return stage.localPositionOf(target, Offset(size.width / 2f, size.height / 2f))
    }
}

/**
 * The scripted fingertip, so a clip shows the hand: it swipes a chat through the rows' own drag,
 * and taps the buttons. Nothing but the script moves it.
 */
@Stable
private class ChatFinger {
    private val alpha = Animatable(0f)
    private var at by mutableStateOf(Offset.Zero)

    // Which gesture has the fingertip, so one cancelled mid-fade doesn't hide the next one's.
    private var gesture = 0

    /**
     * Comes down on chat [id] near the edge it swipes away from, drags it [by] of its width over
     * [duration], holds a moment and lets go. A script stopped mid-swipe still lets go.
     */
    suspend fun swipe(
        smoke: SmokeState,
        id: String,
        by: Float,
        duration: Duration,
        density: Float,
    ) {
        val mine = ++gesture
        var holding = false
        try {
            val bounds = smoke.boundsOf(id) ?: return
            val left = by < 0f
            val inset = FingerInset * density
            val start =
                Offset(if (left) bounds.right - inset else bounds.left + inset, bounds.center.y)
            at = start
            alpha.animateTo(1f, tween(ScriptedTouch.DownMs))
            holding = smoke.dragStart(id, if (left) SmokeSide.Left else SmokeSide.Right)
            if (!holding) return
            animate(
                0f,
                by * bounds.width,
                animationSpec =
                    tween(duration.inWholeMilliseconds.toInt(), easing = FastOutSlowInEasing),
            ) { value, velocity ->
                at = Offset(start.x + value, start.y)
                smoke.dragTo(id, value, velocity)
            }
            delay(HoldMs)
            smoke.release(id, 0f)
            holding = false
            alpha.animateTo(0f, tween(ScriptedTouch.UpMs))
        } finally {
            if (holding) withContext(NonCancellable) { smoke.release(id, 0f) }
            if (mine == gesture) withContext(NonCancellable) { alpha.snapTo(0f) }
        }
    }

    /** Taps [target], if it is on the stage, and runs [action] as the finger comes down. */
    suspend fun tap(target: Offset?, action: () -> Unit) {
        val mine = ++gesture
        try {
            at = target ?: return
            alpha.animateTo(1f, tween(ScriptedTouch.DownMs))
            action()
            delay(ScriptedTouch.TapHoldMs)
            alpha.animateTo(0f, tween(ScriptedTouch.UpMs))
        } finally {
            if (mine == gesture) withContext(NonCancellable) { alpha.snapTo(0f) }
        }
    }

    fun DrawScope.drawFinger(color: Color) {
        val alpha = alpha.value
        if (alpha <= 0f) return
        drawTouch(color, at, alpha)
    }
}

// The scripted finger comes down this far in from a row's edge, and holds this long at the end of
// a swipe before it lets go.
private const val FingerInset = 40f
private const val HoldMs = 90L

// Room under the list for the snackbar, and its shadow.
private val SnackbarRoom = 72.dp

// The way back is a 44 dp target round a larger chevron.
private val BackGlyph = 20.dp
private val BackPadding = 12.dp
private val SnackbarShadow = 10.dp

private val SubtitleStyle = LabTheme.typography.label.copy(fontSize = 13.sp)
private val ActionStyle =
    LabTheme.typography.body.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
private val SnackbarStyle = LabTheme.typography.body.copy(fontSize = 14.sp)

@Preview
@Composable
private fun SmokeDemoDarkPreview() {
    LabTheme(darkTheme = true) { SmokeDemo(remember { DemoState() }) }
}

@Preview
@Composable
private fun SmokeDemoLightPreview() {
    LabTheme(darkTheme = false) { SmokeDemo(remember { DemoState() }) }
}
