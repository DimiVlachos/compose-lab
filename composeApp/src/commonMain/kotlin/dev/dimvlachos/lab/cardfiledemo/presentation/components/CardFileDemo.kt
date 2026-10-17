package dev.dimvlachos.lab.cardfiledemo.presentation.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.dimvlachos.lab.cardfiledemo.CardFileDemos
import dev.dimvlachos.lab.cardfiledemo.Contact
import dev.dimvlachos.lab.cardfiledemo.ContactDeck
import dev.dimvlachos.lab.cardfiledemo.DemoContacts
import dev.dimvlachos.lab.core.demo.DemoState
import dev.dimvlachos.lab.core.demo.demoInsets
import dev.dimvlachos.lab.core.platform.Sound
import dev.dimvlachos.lab.core.platform.rememberReducedMotion
import dev.dimvlachos.lab.core.platform.rememberSoundPlayer
import dev.dimvlachos.lab.core.presentation.components.cardfile.CardFile
import dev.dimvlachos.lab.core.presentation.components.cardfile.CardFileState
import dev.dimvlachos.lab.core.presentation.components.cardfile.FileCard
import dev.dimvlachos.lab.core.presentation.components.cardfile.PaperCard
import dev.dimvlachos.lab.core.presentation.ui.CardFileSheetColors
import dev.dimvlachos.lab.core.presentation.ui.LabTheme
import dev.dimvlachos.lab.resources.Res
import dev.dimvlachos.lab.resources.cardfile_call
import dev.dimvlachos.lab.resources.cardfile_call_failed
import dev.dimvlachos.lab.resources.cardfile_hint
import dev.dimvlachos.lab.resources.cardfile_hint_pulled
import dev.dimvlachos.lab.resources.cardfile_kind_number
import dev.dimvlachos.lab.resources.cardfile_letter
import dev.dimvlachos.lab.resources.cardfile_many_behind_tab
import dev.dimvlachos.lab.resources.cardfile_message
import dev.dimvlachos.lab.resources.cardfile_one_behind_tab
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

/** How long the note that a call or message couldn't open stays up. */
private const val CallFailedNoteMs = 3_000L

/** How long the wheel rests before the hint shows. */
private const val HintAfterMs = 1_400L

/**
 * Contacts as a rotary card file on a walnut desk under a lamp, as the prototype shows them. The
 * sheet at the foot names the front contact; a pulled-out card calls or messages them. The script's
 * spins, pull and file go through the same state a finger drives.
 */
@Composable
internal fun CardFileDemo(
    state: DemoState,
    deck: ContactDeck = remember { ContactDeck(DemoContacts) },
    file: CardFileState = remember { CardFileState(initial = CardFileDemos.StartIndex) },
    reducedMotion: Boolean = rememberReducedMotion(),
) {
    val sounds = rememberSoundPlayer(setOf(Sound.CardTick))
    val uri = LocalUriHandler.current
    SideEffect { file.reducedMotion = reducedMotion }
    // Counts failed opens, so a second failure while the note shows starts its time again.
    var failures by remember { mutableIntStateOf(0) }
    var callFailed by remember { mutableStateOf(false) }
    // The description is built outside composition, so the letter's format is fetched here once.
    val letterFormat = stringResource(Res.string.cardfile_letter, "%s")

    fun open(url: String) {
        try {
            uri.openUri(url)
        } catch (_: IllegalArgumentException) {
            // What UriHandler promises, and what Android throws when nothing can take the link.
            failures++
        } catch (_: IllegalStateException) {
            failures++
        }
    }

    LaunchedEffect(failures) {
        if (failures == 0) return@LaunchedEffect
        callFailed = true
        delay(CallFailedNoteMs)
        callFailed = false
    }

    // Stopping the script cancels the step it is on; the wheel then settles on a whole card, and a
    // card caught half out comes all the way out, rather than either being left in between. A step
    // cut short by the user's own spin or file is left to the user: only a cancelled step settles.
    val scope = rememberCoroutineScope()

    suspend fun step(block: suspend () -> Unit) {
        try {
            block()
        } catch (e: CancellationException) {
            if (currentCoroutineContext().job.isCancelled) scope.launch { file.settle() }
            throw e
        }
    }

    DisposableEffect(state, file) {
        state.setCardSpinHandler { by, duration -> step { file.spinBy(by, duration) } }
        state.setControlTapHandler { control ->
            step {
                when (control) {
                    CardFileDemos.Pull -> file.pull()
                    CardFileDemos.File -> file.file()
                }
            }
        }
        // Puts the card back and the wheel on its first card, so the clip loops.
        state.setFeedResetHandler {
            step {
                file.file()
                file.spinTo(CardFileDemos.StartIndex)
            }
        }
        onDispose {
            state.setCardSpinHandler(null)
            state.setControlTapHandler(null)
            state.setFeedResetHandler(null)
        }
    }

    Box(Modifier.fillMaxSize().windowInsetsPadding(demoInsets(state))) {
        CardFile(
            cards = deck.cards,
            paper = { entry ->
                val c = deck.contact(entry)
                PaperCard.Contact(c.name, c.kind, listOf(c.number, c.line, c.note), c.seed)
            },
            describe = { card ->
                when (card) {
                    is FileCard.Divider -> letterFormat.replace("%s", card.letter.toString())
                    is FileCard.Entry -> deck.contact(card).let { "${it.name}, ${it.number}" }
                }
            },
            modifier = Modifier.fillMaxSize(),
            state = file,
            sounds = sounds,
        )
        Column(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Hint(file)
            Sheet(
                deck,
                file,
                callFailed,
                onCall = { open("tel:${it.number.filter(Char::isDigit)}") },
                onMessage = { open("sms:${it.number.filter(Char::isDigit)}") },
            )
        }
    }
}

/** The pill over the desk that says what to do, once the wheel has rested a moment. */
@Composable
private fun Hint(file: CardFileState) {
    val pulled by remember(file) { derivedStateOf { file.pulled != null } }
    val front by remember(file) { derivedStateOf { file.front } }
    val moving by
        remember(file) {
            derivedStateOf { file.isSpinning || (file.pulled != null && file.pullProgress < .98f) }
        }
    var show by remember { mutableStateOf(false) }
    LaunchedEffect(moving, front, pulled) {
        show = false
        if (moving) return@LaunchedEffect
        if (!pulled) delay(HintAfterMs)
        show = true
    }
    val alpha by animateFloatAsState(if (show) 1f else 0f, tween(260), label = "hint")
    val text =
        if (pulled) stringResource(Res.string.cardfile_hint_pulled)
        else stringResource(Res.string.cardfile_hint)
    Row(
        Modifier.swallowTouches()
            .alpha(alpha)
            .clip(RoundedCornerShape(50))
            .background(CardFileSheetColors.HintPill)
            .border(1.dp, CardFileSheetColors.HintEdge, RoundedCornerShape(50))
            .padding(start = 11.dp, end = 13.dp, top = 7.dp, bottom = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Image(rememberVectorPainter(DownArrow), contentDescription = null, Modifier.size(13.dp))
        Text(text, color = Color.White, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
    }
}

/**
 * The frosted sheet at the foot: who is at the front, and with their card out, Call and Message. A
 * divider at the front says how many cards stand behind its tab.
 */
@Composable
private fun Sheet(
    deck: ContactDeck,
    file: CardFileState,
    callFailed: Boolean,
    onCall: (Contact) -> Unit,
    onMessage: (Contact) -> Unit,
) {
    val colors = if (LabTheme.isDark) CardFileSheetColors.Dark else CardFileSheetColors.Light
    // Read through derivedStateOf, so the sheet recomposes as the front card changes, not on every
    // frame of a spin. A card being filed lets go of the sheet as it starts back, not as it lands.
    val shown by
        remember(deck, file) {
            derivedStateOf {
                deck.cards.getOrNull(file.pulled ?: file.front) to
                    (file.pulled != null && !file.filing)
            }
        }
    val shape = RoundedCornerShape(22.dp)
    Box(
        Modifier.fillMaxWidth()
            .swallowTouches()
            .shadow(18.dp, shape, ambientColor = Color.Black, spotColor = Color.Black)
            .clip(shape)
            .background(colors.sheet)
            .border(1.dp, colors.line, shape)
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        AnimatedContent(
            targetState = shown,
            contentKey = { it.second },
            transitionSpec = {
                (fadeIn(tween(450)) + slideInVertically(tween(450)) { it / 8 }) togetherWith
                    fadeOut(tween(150))
            },
            label = "sheet",
        ) { (card, out) ->
            Row(
                Modifier.fillMaxWidth().heightIn(min = 44.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // As a spin brings each card to the front, its name fades in over the last one's.
                Crossfade(card, Modifier.weight(1f), tween(220), label = "sheet name") { card ->
                    Column {
                        val (title, sub) = describe(deck, card, out)
                        Text(
                            title,
                            color = colors.ink,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            if (callFailed) stringResource(Res.string.cardfile_call_failed)
                            else sub,
                            color = colors.muted,
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier =
                                if (callFailed)
                                    Modifier.testTag("call-failed").semantics {
                                        liveRegion = LiveRegionMode.Polite
                                    }
                                else Modifier,
                        )
                    }
                }
                if (out && card is FileCard.Entry) {
                    val contact = deck.contact(card)
                    SheetButton(
                        stringResource(Res.string.cardfile_message),
                        Modifier.testTag("message"),
                        fill = Color.Transparent,
                        ink = colors.ink,
                        edge = colors.line,
                    ) {
                        onMessage(contact)
                    }
                    SheetButton(
                        stringResource(Res.string.cardfile_call),
                        Modifier.testTag("call"),
                        fill = colors.accent,
                        ink = Color.White,
                        icon = Phone,
                    ) {
                        onCall(contact)
                    }
                }
            }
        }
    }
}

/** The sheet's title and the line under it for [card], out of the file or not. */
@Composable
private fun describe(deck: ContactDeck, card: FileCard?, out: Boolean): Pair<String, String> =
    when (card) {
        is FileCard.Divider -> {
            val n = deck.cards.count { it is FileCard.Entry && it.letter == card.letter }
            card.letter.toString() to
                if (n == 1) stringResource(Res.string.cardfile_one_behind_tab)
                else stringResource(Res.string.cardfile_many_behind_tab, n)
        }
        is FileCard.Entry -> {
            val c = deck.contact(card)
            c.name to
                if (out) c.number
                else stringResource(Res.string.cardfile_kind_number, c.kind, c.number)
        }
        null -> "" to ""
    }

/** A pill button on the sheet, 44 dp tall: filled with the accent, or a ghost with an edge. */
@Composable
private fun SheetButton(
    label: String,
    modifier: Modifier,
    fill: Color,
    ink: Color,
    edge: Color? = null,
    icon: ImageVector? = null,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(50)
    Row(
        modifier
            .heightIn(min = 44.dp)
            .clip(shape)
            .background(fill)
            .then(if (edge != null) Modifier.border(1.5.dp, edge, shape) else Modifier)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (icon != null) {
            Image(
                rememberVectorPainter(icon),
                contentDescription = null,
                Modifier.size(16.dp).clearAndSetSemantics {},
                colorFilter = ColorFilter.tint(ink),
            )
        }
        Text(label, color = ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}

/**
 * Keeps touches that land here off the card file under it, as the prototype's sheet is a page
 * element over its canvas: a tap on the sheet must not file the pulled card. Buttons inside still
 * get their taps.
 */
private fun Modifier.swallowTouches(): Modifier =
    pointerInput(Unit) {
        awaitEachGesture {
            awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Final).consume()
        }
    }

/** The hint's arrow, pointing down: push the cards toward you. */
private val DownArrow: ImageVector =
    ImageVector.Builder("down", 13.dp, 13.dp, 14f, 14f)
        .addPath(
            addPathNodes("M7 2v9.5M3 7.5l4 4 4-4"),
            stroke = SolidColor(Color.White),
            strokeLineWidth = 1.8f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        )
        .build()

/** The prototype's handset, for Call. */
private val Phone: ImageVector =
    ImageVector.Builder("phone", 16.dp, 16.dp, 24f, 24f)
        .addPath(
            addPathNodes(
                "M6.6 3.5l2.6.4 1.2 4-1.9 1.6a12 12 0 005.9 5.9l1.6-1.9 4 1.2.4 2.6c-.1 1.4-1.3 " +
                    "2.4-2.7 2.3C10.3 19 5 13.7 4.3 6.2 4.2 4.8 5.2 3.6 6.6 3.5z"
            ),
            fill = SolidColor(Color.White),
        )
        .build()
