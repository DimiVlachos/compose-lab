package dev.dimvlachos.lab.pencilcupdemo.presentation.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import dev.dimvlachos.lab.core.demo.DemoState
import dev.dimvlachos.lab.core.platform.rememberReducedMotion
import dev.dimvlachos.lab.core.platform.rememberSoundPlayer
import dev.dimvlachos.lab.core.presentation.canvas2d.setBlur
import dev.dimvlachos.lab.core.presentation.components.pencilcup.CupTools
import dev.dimvlachos.lab.core.presentation.components.pencilcup.PencilCupFab
import dev.dimvlachos.lab.core.presentation.components.pencilcup.PencilCupSounds
import dev.dimvlachos.lab.core.presentation.components.pencilcup.PencilCupState
import dev.dimvlachos.lab.core.presentation.components.touch.ScriptedTouch
import dev.dimvlachos.lab.core.presentation.components.touch.drawTouch
import dev.dimvlachos.lab.core.presentation.ui.LabTheme
import dev.dimvlachos.lab.core.presentation.ui.PencilCupScreenColors
import dev.dimvlachos.lab.pencilcupdemo.FieldNote
import dev.dimvlachos.lab.pencilcupdemo.PencilCupDemos
import dev.dimvlachos.lab.pencilcupdemo.StartingNotes
import dev.dimvlachos.lab.resources.Res
import dev.dimvlachos.lab.resources.pencilcup_add_item
import dev.dimvlachos.lab.resources.pencilcup_added
import dev.dimvlachos.lab.resources.pencilcup_clip
import dev.dimvlachos.lab.resources.pencilcup_clip_about
import dev.dimvlachos.lab.resources.pencilcup_closed
import dev.dimvlachos.lab.resources.pencilcup_count
import dev.dimvlachos.lab.resources.pencilcup_create
import dev.dimvlachos.lab.resources.pencilcup_done
import dev.dimvlachos.lab.resources.pencilcup_just_now
import dev.dimvlachos.lab.resources.pencilcup_list
import dev.dimvlachos.lab.resources.pencilcup_made_clip_detail
import dev.dimvlachos.lab.resources.pencilcup_made_list_detail
import dev.dimvlachos.lab.resources.pencilcup_made_scan
import dev.dimvlachos.lab.resources.pencilcup_made_scan_detail
import dev.dimvlachos.lab.resources.pencilcup_note
import dev.dimvlachos.lab.resources.pencilcup_opened
import dev.dimvlachos.lab.resources.pencilcup_paste_link
import dev.dimvlachos.lab.resources.pencilcup_scan
import dev.dimvlachos.lab.resources.pencilcup_scan_hint
import dev.dimvlachos.lab.resources.pencilcup_search
import dev.dimvlachos.lab.resources.pencilcup_title
import dev.dimvlachos.lab.resources.pencilcup_untitled
import kotlin.math.roundToInt
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/**
 * The prototype's notes app, Fieldbook: a list of notes whose floating + button is a cobalt pencil
 * cup in the corner. Its tools fan out as the actions; the one picked opens its sheet, and Done
 * adds what was typed there to the top of the list, or cancels when nothing was.
 */
@Composable
internal fun PencilCupDemo(
    state: DemoState,
    cup: PencilCupState = remember { PencilCupState() },
    reducedMotion: Boolean = rememberReducedMotion(),
) {
    val dark = LabTheme.isDark
    val colors = if (dark) PencilCupScreenColors.Dark else PencilCupScreenColors.Light
    val sounds = rememberSoundPlayer(PencilCupSounds)
    val notes = remember { mutableStateListOf<FieldNote>().apply { addAll(StartingNotes) } }
    var nextId by remember { mutableIntStateOf(100) }
    var fresh by remember { mutableIntStateOf(-1) }
    val finger = remember { CupFinger() }
    val places = remember { Places() }
    var announcement by remember { mutableStateOf("") }
    val density = LocalDensity.current.density
    SideEffect { places.density = density }
    val input = LocalInputModeManager.current
    val cupFocus = remember { FocusRequester() }
    val doneFocus = remember { FocusRequester() }

    val labels =
        listOf(
            stringResource(Res.string.pencilcup_scan),
            stringResource(Res.string.pencilcup_clip),
            stringResource(Res.string.pencilcup_list),
            stringResource(Res.string.pencilcup_note),
        )
    val justNow = stringResource(Res.string.pencilcup_just_now)
    val scanned = stringResource(Res.string.pencilcup_made_scan)
    // Under each new item's title: the scan's page, and the others' kind, made just now.
    val details =
        listOf(
            stringResource(Res.string.pencilcup_made_scan_detail),
            stringResource(Res.string.pencilcup_made_clip_detail),
            stringResource(Res.string.pencilcup_made_list_detail),
            justNow,
        )
    // What is typed on the sheet: the note's title, the checklist's first item or the link.
    var draft by remember { mutableStateOf("") }
    val focus = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val opened = stringResource(Res.string.pencilcup_opened)
    val closed = stringResource(Res.string.pencilcup_closed)
    val added = stringResource(Res.string.pencilcup_added, "%s")

    // The keyboard goes with the sheet, so it slides away with it instead of staying up.
    fun dropKeyboard() {
        focus.clearFocus(force = true)
        keyboard?.hide()
    }

    // Back and Escape cancel the sheet: the tool goes home, and only Done makes the item.
    fun cancelSheet() {
        dropKeyboard()
        if (cup.done() < 0) return
        announcement = closed
        if (input.inputMode == InputMode.Keyboard) runCatching { cupFocus.requestFocus() }
    }

    // Done makes what was typed, as the item's title; with nothing typed it cancels. The scan has
    // nothing to type, so its Done always makes the page.
    fun finish() {
        if (!cup.sheetOpen) return
        val tool = cup.sheetTool
        val title = if (tool == CupTools.Scan) scanned else draft.trim()
        if (title.isEmpty()) return cancelSheet()
        dropKeyboard()
        val i = cup.done()
        if (i < 0) return
        val note = FieldNote(nextId++, title, details[i])
        notes.add(0, note)
        fresh = note.id
        announcement = added.replace("%s", note.title)
        // Keys go back to the cup, as the prototype's do; a touch leaves focus where it was.
        if (input.inputMode == InputMode.Keyboard) runCatching { cupFocus.requestFocus() }
    }

    // A sheet opens blank. One opened from the keyboard hands it Done, as the prototype's does, but
    // not in the clip.
    LaunchedEffect(cup) {
        snapshotFlow { cup.sheetOpen }
            .collect { open ->
                if (open) draft = ""
                if (open && !state.replay && input.inputMode == InputMode.Keyboard) {
                    delay(50)
                    runCatching { doneFocus.requestFocus() }
                }
            }
    }

    // What changed, for a screen reader: the fan out, shut, or a tool picked, in today's words.
    val said by rememberUpdatedState(Triple(labels, opened, closed))
    LaunchedEffect(cup) {
        var wasOpen = false
        var wasSheet = false
        snapshotFlow { cup.isOpen to cup.sheetOpen }
            .collect { (open, sheet) ->
                when {
                    sheet && !wasSheet -> announcement = said.first[cup.sheetTool]
                    open && !wasOpen -> announcement = said.second
                    !open && wasOpen && !sheet -> announcement = said.third
                }
                wasOpen = open
                wasSheet = sheet
            }
    }

    DisposableEffect(state, cup) {
        state.setControlTapHandler { control ->
            try {
                when (control) {
                    PencilCupDemos.TapCup -> {
                        finger.appear(Offset(cup.width * .55f, cup.height * .62f))
                        finger.glide(cup.cupPoint, 480)
                        finger.press { cup.down(it.x, it.y) }
                        delay(ScriptedTouch.TapHoldMs)
                        finger.lift { cup.up(it.x, it.y) }
                    }
                    PencilCupDemos.SlideToNote -> {
                        finger.appear(Offset(cup.width * .55f, cup.height * .62f))
                        finger.glide(cup.cupPoint, 420)
                        finger.press { cup.down(it.x, it.y) }
                        delay(160)
                        val to = cup.alongTool(CupTools.Note, .72f)
                        finger.glide(to, 700) { cup.move(it.x, it.y) }
                        delay(350)
                        finger.lift { cup.up(it.x, it.y) }
                    }
                    PencilCupDemos.TypeTitle -> {
                        // A letter at a time, at a typing pace, as the prototype's tour types.
                        for (ch in PencilCupDemos.Typed) {
                            draft += ch
                            delay(PencilCupDemos.keyMs(ch))
                        }
                    }
                    PencilCupDemos.Done -> {
                        val at = places.doneCentre()
                        if (at != null) finger.glide(at, 500)
                        finger.press {}
                        delay(ScriptedTouch.TapHoldMs)
                        finger.lift { finish() }
                        delay(500)
                        finger.fadeOut()
                    }
                }
            } finally {
                // A move cut short lets go of the cup.
                if (finger.pressed) withContext(NonCancellable) { cup.cancel() }
                finger.pressed = false
            }
        }
        state.setFeedResetHandler {
            cup.reset()
            notes.clear()
            notes.addAll(StartingNotes)
            fresh = -1
            announcement = ""
            draft = ""
            finger.hide()
        }
        onDispose {
            state.setControlTapHandler(null)
            state.setFeedResetHandler(null)
        }
    }

    // A script the user takes over leaves no fingertip behind.
    LaunchedEffect(state.replay) { if (!state.replay) finger.hide() }

    NavigationBackHandler(
        state = rememberNavigationEventState(NavigationEventInfo.None),
        isBackEnabled = cup.sheetOpen,
        onBackCompleted = { cancelSheet() },
    )

    // The keyboard, frame by frame as it slides: how far it covers this screen's foot, in dp. The
    // sheet rides up on it, and the tool in its pocket with it; the list and the cup stay put.
    val ime = WindowInsets.ime
    val densityNow = LocalDensity.current
    val windowHeight by rememberUpdatedState(LocalWindowInfo.current.containerSize.height)
    var rootBottom by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(cup, ime, densityNow) {
        snapshotFlow { ime.getBottom(densityNow) - (windowHeight - rootBottom) }
            .collect { cup.keyboardOverlap(it / densityNow.density) }
    }

    val touch = LabTheme.colors.touch
    Box(
        Modifier.fillMaxSize()
            .background(colors.background)
            // The bars and the cutout, but not the keyboard: it only lifts the sheet (see
            // keyboardOverlap), so the list and the cup stay put while it slides.
            .windowInsetsPadding(
                if (state.recording) WindowInsets(0)
                else
                    WindowInsets.systemBars
                        .union(WindowInsets.displayCutout)
                        .only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom),
            )
            .onGloballyPositioned {
                places.root = it
                rootBottom = it.positionInWindow().y + it.size.height
            }
            .onPreviewKeyEvent {
                if (it.type == KeyEventType.KeyDown && it.key == Key.Escape && cup.sheetOpen) {
                    cancelSheet()
                    true
                } else false
            },
    ) {
        Column(
            Modifier.fillMaxSize()
                .then(if (cup.sheetOpen) Modifier.clearAndSetSemantics {} else Modifier),
        ) {
            AppBar(notes.size, colors)
            Search(colors)
            // No spacedBy: each card carries the 8 dp gap below it, so a new card's gap grows with
            // it.
            Column(
                Modifier.weight(1f).fillMaxWidth().clipToBounds().padding(horizontal = 16.dp),
            ) {
                for (note in notes) key(note.id) {
                    NoteCard(note, note.id == fresh, colors, reducedMotion)
                }
            }
        }
        PencilCupFab(
            state = cup,
            labels = labels,
            cupLabel = stringResource(Res.string.pencilcup_create),
            colors = colors,
            dark = dark,
            sounds = sounds,
            modifier = Modifier.fillMaxSize(),
            reducedMotion = reducedMotion,
            cupFocus = cupFocus,
        ) {
            Sheet(
                cup = cup,
                open = cup.sheetOpen,
                title = labels[cup.sheetTool],
                tool = cup.sheetTool,
                colors = colors,
                reducedMotion = reducedMotion,
                places = places,
                doneFocus = doneFocus,
                draft = draft,
                onDraft = { draft = it },
                onDone = { finish() },
            )
        }
        // What changed, for a screen reader.
        Box(
            Modifier.size(1.dp).semantics {
                liveRegion = LiveRegionMode.Polite
                contentDescription = announcement
            },
        )
        Canvas(Modifier.fillMaxSize()) { with(finger) { drawFinger(touch) } }
    }
}

/** The bar: the app's title and how many notes it holds, and the user's avatar. */
@Composable
private fun AppBar(count: Int, colors: PencilCupScreenColors) {
    Row(
        Modifier.fillMaxWidth().padding(start = 18.dp, end = 18.dp, top = 18.dp, bottom = 6.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(Modifier.weight(1f)) {
            BasicText(
                stringResource(Res.string.pencilcup_title),
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
                pluralStringResource(Res.plurals.pencilcup_count, count, count),
                Modifier.padding(top = 3.dp),
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
                .background(PencilCupScreenColors.Avatar)
                .clearAndSetSemantics {},
            contentAlignment = Alignment.Center,
        ) {
            BasicText(
                "JC",
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
private fun Search(colors: PencilCupScreenColors) {
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
            stringResource(Res.string.pencilcup_search),
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

/**
 * A note card, with the 8 dp gap below it. The one just made opens its slot from nothing, so the
 * cards under it glide down rather than jump, and fades in as it settles.
 */
@Composable
private fun NoteCard(
    note: FieldNote,
    fresh: Boolean,
    colors: PencilCupScreenColors,
    reducedMotion: Boolean,
) {
    val arrive = remember { Animatable(if (fresh && !reducedMotion) 0f else 1f) }
    val open = remember { Animatable(if (fresh && !reducedMotion) 0f else 1f) }
    LaunchedEffect(fresh) {
        if (fresh && !reducedMotion) {
            launch { open.animateTo(1f, tween(OpenMs, easing = SlotEasing)) }
            arrive.animateTo(1f, tween(600, easing = EaseOut))
        }
    }
    Column(
        Modifier.fillMaxWidth()
            .layout { measurable, constraints ->
                val card = measurable.measure(constraints)
                val slot = card.height + NoteGap.roundToPx()
                layout(card.width, (slot * open.value).roundToInt()) { card.place(0, 0) }
            }
            .graphicsLayer {
                alpha = arrive.value
                translationY = (1f - arrive.value) * -8.dp.toPx()
            }
            .clip(RoundedCornerShape(14.dp))
            .background(colors.card)
            .border(1.dp, colors.line, RoundedCornerShape(14.dp))
            .padding(start = 14.dp, end = 14.dp, top = 11.dp, bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        BasicText(
            note.title,
            style =
                TextStyle(
                    lineHeightStyle = CssLines,
                    color = colors.ink,
                    fontSize = 14.5.sp,
                    lineHeight = BodyLine,
                    fontWeight = FontWeight(650),
                ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        BasicText(
            note.text,
            style =
                TextStyle(
                    lineHeightStyle = CssLines,
                    color = colors.muted,
                    fontSize = 12.8.sp,
                    lineHeight = BodyLine,
                ),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * The new item's sheet, as the prototype's: 64 % of the screen high, sliding up from below with its
 * grab handle, the tool's name and Done, and a body for what the tool makes. The [cup] slides it,
 * on its own clock, so the tool tucked in the pocket at its corner rides with it; the title stands
 * clear of that pocket.
 */
@Composable
private fun BoxScope.Sheet(
    cup: PencilCupState,
    open: Boolean,
    title: String,
    tool: Int,
    colors: PencilCupScreenColors,
    reducedMotion: Boolean,
    places: Places,
    doneFocus: FocusRequester,
    draft: String,
    onDraft: (String) -> Unit,
    onDone: () -> Unit,
) {
    val shadow = remember { Paint() }
    val shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp)
    Column(
        Modifier.align(Alignment.BottomCenter)
            .fillMaxWidth()
            .fillMaxHeight(.64f)
            .graphicsLayer {
                cup.frame
                // Slid by the cup's clock, and lifted by the keyboard, frame by frame.
                translationY = cup.sheetSlide * size.height - cup.sheetLift * density
            }
            .drawBehind {
                // box-shadow: 0 -12px 40px rgba(20,14,8,.22).
                shadow.color = Color(20, 14, 8, 56)
                shadow.setBlur(20.dp.toPx())
                val r = 22.dp.toPx()
                drawIntoCanvas {
                    it.drawRoundRect(0f, -12.dp.toPx(), size.width, size.height + r, r, r, shadow)
                }
            }
            .clip(shape)
            .background(colors.card)
            .pointerInput(Unit) { awaitEachGesture { awaitFirstDown().consume() } }
            .then(
                if (open) Modifier.semantics { paneTitle = title }
                else Modifier.clearAndSetSemantics {},
            )
            .testTag("pencil-sheet"),
    ) {
        Box(
            Modifier.padding(top = 8.dp)
                .align(Alignment.CenterHorizontally)
                .size(38.dp, 5.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(colors.line),
        )
        Row(
            Modifier.fillMaxWidth().padding(start = 20.dp, end = 12.dp, top = 8.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            BasicText(
                title,
                Modifier.weight(1f).padding(start = 46.dp).semantics { heading() },
                style =
                    TextStyle(
                        lineHeightStyle = CssLines,
                        color = colors.ink,
                        fontSize = 18.sp,
                        lineHeight = BodyLine,
                        fontWeight = FontWeight.Bold,
                    ),
            )
            Box(
                Modifier.heightIn(min = 44.dp)
                    .widthIn(min = 64.dp)
                    .onGloballyPositioned { places.done = it }
                    .focusRequester(doneFocus)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(enabled = open, role = Role.Button, onClick = onDone)
                    .testTag("pencil-done"),
                contentAlignment = Alignment.Center,
            ) {
                BasicText(
                    stringResource(Res.string.pencilcup_done),
                    style =
                        TextStyle(
                            lineHeightStyle = CssLines,
                            color = colors.accent,
                            fontSize = 15.5.sp,
                            lineHeight = BodyLine,
                            fontWeight = FontWeight(650),
                        ),
                )
            }
        }
        Column(
            Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 6.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            when (tool) {
                CupTools.Note -> RuledNote(colors, open && !reducedMotion, draft, onDraft, onDone)
                CupTools.List -> Checklist(colors, draft, onDraft, onDone)
                CupTools.Clip -> WebClip(colors, draft, onDraft, onDone)
                else -> ScanFrame(colors)
            }
        }
    }
}

/**
 * A ruled page whose first line is the note's title: a field showing "Untitled note" until
 * something is typed. Until the field takes focus, a drawn caret blinks before the placeholder, as
 * the prototype's does; then the field's own caret takes over.
 */
@Composable
private fun RuledNote(
    colors: PencilCupScreenColors,
    blinking: Boolean,
    draft: String,
    onDraft: (String) -> Unit,
    onDone: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val drawnCaret = !focused && draft.isEmpty()
    // Only while it blinks on screen: a caret blinking off screen would keep every frame busy.
    val blink =
        if (!blinking || !drawnCaret) null
        else
            rememberInfiniteTransition(label = "caret")
                .animateFloat(
                    0f,
                    1f,
                    infiniteRepeatable(tween(1000, easing = LinearEasing), RepeatMode.Restart),
                    label = "blink",
                )
    val title =
        TextStyle(
            lineHeightStyle = CssLines,
            color = colors.ink,
            fontSize = 20.sp,
            lineHeight = BodyLine,
            fontWeight = FontWeight.Bold,
        )
    Box(
        Modifier.fillMaxWidth().height(150.dp).drawBehind {
            // repeating-linear-gradient: a 1 px line at the foot of every 28 px.
            val step = 28.dp.toPx()
            var y = 27.dp.toPx()
            while (y < size.height) {
                drawRect(
                    colors.line,
                    Offset(0f, y),
                    androidx.compose.ui.geometry.Size(size.width, 1.dp.toPx()),
                )
                y += step
            }
        },
    ) {
        SheetField(
            draft,
            onDraft,
            onDone,
            stringResource(Res.string.pencilcup_untitled),
            title,
            title.copy(color = colors.ink.copy(alpha = colors.ink.alpha * .35f)),
            colors,
            Modifier.offset(4.dp, 2.dp).fillMaxWidth().onFocusChanged { focused = it.isFocused },
        )
        if (drawnCaret) {
            Box(
                Modifier.offset(0.dp, 4.dp).size(2.dp, 24.dp).drawBehind {
                    // steps(1): on for the first half of each second, off for the second.
                    if (blink == null || blink.value < .5f) drawRect(colors.accent)
                },
            )
        }
    }
}

/**
 * One line to type on the sheet, drawn as the prototype's plain text: [placeholder] in
 * [placeholderStyle] until something is typed, then the text in [style]. The keyboard's Done is the
 * sheet's Done. Labelled by its placeholder for a screen reader.
 */
@Composable
private fun SheetField(
    value: String,
    onValue: (String) -> Unit,
    onDone: () -> Unit,
    placeholder: String,
    style: TextStyle,
    placeholderStyle: TextStyle,
    colors: PencilCupScreenColors,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
) {
    BasicTextField(
        value = value,
        onValueChange = onValue,
        modifier = modifier.testTag("pencil-field").semantics { contentDescription = placeholder },
        textStyle = style,
        singleLine = true,
        cursorBrush = SolidColor(colors.accent),
        keyboardOptions =
            KeyboardOptions(
                capitalization =
                    if (keyboardType == KeyboardType.Uri) KeyboardCapitalization.None
                    else KeyboardCapitalization.Sentences,
                keyboardType = keyboardType,
                imeAction = ImeAction.Done,
            ),
        keyboardActions = KeyboardActions(onDone = { onDone() }),
        decorationBox = { inner ->
            Box {
                if (value.isEmpty()) BasicText(placeholder, style = placeholderStyle, maxLines = 1)
                inner()
            }
        },
    )
}

@Composable
private fun Checklist(
    colors: PencilCupScreenColors,
    draft: String,
    onDraft: (String) -> Unit,
    onDone: () -> Unit,
) {
    val add = stringResource(Res.string.pencilcup_add_item)
    Column {
        // The first row takes the first item; the two below stay empty lines, as the prototype's.
        for (row in 0 until 3) {
            Row(
                Modifier.fillMaxWidth()
                    .drawBehind {
                        val w = 1.dp.toPx()
                        drawRect(
                            colors.line,
                            Offset(0f, size.height - w),
                            androidx.compose.ui.geometry.Size(size.width, w),
                        )
                    }
                    .padding(top = 6.dp, bottom = 7.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(20.dp).border(1.8.dp, colors.line, CircleShape))
                if (row == 0) {
                    SheetField(
                        draft,
                        onDraft,
                        onDone,
                        add,
                        bodyStyle(colors).copy(color = colors.ink),
                        bodyStyle(colors),
                        colors,
                        Modifier.weight(1f),
                    )
                } else BasicText(" ", style = bodyStyle(colors))
            }
        }
    }
}

@Composable
private fun WebClip(
    colors: PencilCupScreenColors,
    draft: String,
    onDraft: (String) -> Unit,
    onDone: () -> Unit,
) {
    Box(
        Modifier.fillMaxWidth()
            .dashed(colors.line, 1.dp, 12.dp)
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        SheetField(
            draft,
            onDraft,
            onDone,
            stringResource(Res.string.pencilcup_paste_link),
            bodyStyle(colors).copy(color = colors.ink),
            bodyStyle(colors),
            colors,
            Modifier.fillMaxWidth(),
            KeyboardType.Uri,
        )
    }
    BasicText(stringResource(Res.string.pencilcup_clip_about), style = bodyStyle(colors))
}

@Composable
private fun ScanFrame(colors: PencilCupScreenColors) {
    Box(
        Modifier.fillMaxWidth().height(170.dp).dashed(colors.line, 2.dp, 14.dp).padding(12.dp),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(
            stringResource(Res.string.pencilcup_scan_hint),
            style = bodyStyle(colors).copy(textAlign = TextAlign.Center),
        )
    }
}

private fun bodyStyle(colors: PencilCupScreenColors) =
    TextStyle(
        lineHeightStyle = CssLines,
        color = colors.muted,
        fontSize = 14.5.sp,
        lineHeight = BodyLine,
    )

/** A CSS dashed border: dashes three times the [width] long, as the browser draws them. */
private fun Modifier.dashed(
    color: Color,
    width: androidx.compose.ui.unit.Dp,
    radius: androidx.compose.ui.unit.Dp,
) = drawBehind {
    val w = width.toPx()
    val dash = 3 * w
    drawRoundRect(
        color,
        Offset(w / 2, w / 2),
        androidx.compose.ui.geometry.Size(size.width - w, size.height - w),
        CornerRadius(radius.toPx() - w / 2),
        style = Stroke(w, pathEffect = PathEffect.dashPathEffect(floatArrayOf(dash, dash))),
    )
}

/** Where the script's fingertip goes: the screen, and the sheet's Done. */
private class Places {
    var root: LayoutCoordinates? = null
    var done: LayoutCoordinates? = null

    /** Done's middle, in the screen's dp. */
    fun doneCentre(): Offset? {
        val r = root?.takeIf { it.isAttached } ?: return null
        val d = done?.takeIf { it.isAttached } ?: return null
        val px = r.localPositionOf(d, Offset(d.size.width / 2f, d.size.height / 2f))
        return px / density
    }

    var density = 1f
}

/**
 * The scripted fingertip, in the screen's dp: it glides between points on the prototype's ease,
 * comes down and lifts, and works the cup through the same calls a finger does.
 */
@Stable
private class CupFinger {
    private val alpha = Animatable(0f)
    private val down = Animatable(0f)
    private var at by mutableStateOf(Offset.Zero)
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

/** The prototype's ease: quadratic in, quadratic out. */
private val PrototypeEase = Easing { PencilCupState.ease(it) }

/** CSS `ease-out`. */
private val EaseOut = CubicBezierEasing(0f, 0f, .58f, 1f)

/** The prototype's sheet curve, so a new note's slot opens as the sheet moves. */
private val SlotEasing = CubicBezierEasing(.2f, .9f, .25f, 1f)
private const val OpenMs = 420
private val NoteGap = 8.dp

/** The prototype's body line height, which every line on the screen inherits. */
private val BodyLine = 1.55.em

/** CSS line boxes: the leading split above and below every line, the first and last included. */
private val CssLines = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.None)
