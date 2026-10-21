package dev.dimvlachos.lab.mailboxdemo.presentation.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
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
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import dev.dimvlachos.lab.core.demo.DemoState
import dev.dimvlachos.lab.core.demo.demoInsets
import dev.dimvlachos.lab.core.platform.rememberReducedMotion
import dev.dimvlachos.lab.core.presentation.components.mailbox.Mailbox
import dev.dimvlachos.lab.core.presentation.components.mailbox.MailboxState
import dev.dimvlachos.lab.core.presentation.components.touch.ScriptedTouch
import dev.dimvlachos.lab.core.presentation.components.touch.drawTouch
import dev.dimvlachos.lab.core.presentation.ui.LabTheme
import dev.dimvlachos.lab.core.presentation.ui.MailboxFaces
import dev.dimvlachos.lab.core.presentation.ui.MailboxScreenColors
import dev.dimvlachos.lab.mailboxdemo.Letter
import dev.dimvlachos.lab.mailboxdemo.MailboxDemos
import dev.dimvlachos.lab.mailboxdemo.NeighborMail
import dev.dimvlachos.lab.resources.Res
import dev.dimvlachos.lab.resources.mailbox_back
import dev.dimvlachos.lab.resources.mailbox_caught_up
import dev.dimvlachos.lab.resources.mailbox_home_subtitle
import dev.dimvlachos.lab.resources.mailbox_home_title
import dev.dimvlachos.lab.resources.mailbox_inbox_none
import dev.dimvlachos.lab.resources.mailbox_inbox_title
import dev.dimvlachos.lab.resources.mailbox_inbox_unread
import dev.dimvlachos.lab.resources.mailbox_mark_all_read
import dev.dimvlachos.lab.resources.mailbox_new_message
import dev.dimvlachos.lab.resources.mailbox_no_unread
import dev.dimvlachos.lab.resources.mailbox_row
import dev.dimvlachos.lab.resources.mailbox_row_unread
import dev.dimvlachos.lab.resources.mailbox_unread
import kotlin.math.PI
import kotlin.math.sin
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.stringResource

/**
 * A neighborhood app whose inbox icon is a tin mailbox, laid out as the HTML prototype lays it out:
 * a top bar with the user's avatar, the street and the mailbox, a morning feed of neighborhood
 * news, and an inbox that slides in from the right. Mail drops into the mailbox and raises its
 * flag; tapping or pulling down on it opens the inbox; reading everything lowers the flag.
 */
@Composable
internal fun MailboxDemo(state: DemoState, reducedMotion: Boolean = rememberReducedMotion()) {
    val dark = LabTheme.isDark
    val colors = if (dark) MailboxScreenColors.Dark else MailboxScreenColors.Light
    val mail = remember { NeighborMail() }
    val box = remember { MailboxState(reducedMotion) }
    SideEffect { box.reducedMotion = reducedMotion }
    // The inbox is showing: the top bar says so and the page has slid in, or is sliding.
    var inbox by remember { mutableStateOf(false) }
    val page = remember { Animatable(0f) }
    // The letter that has just arrived slides into the list.
    var fresh by remember { mutableStateOf<Int?>(null) }
    var announcement by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val targets = remember { Targets() }
    val finger = remember { ScriptFinger() }

    fun deliver() {
        box.deliver {
            val letter = mail.arrive()
            fresh = letter.id
            announcement = letter.from
        }
    }

    fun openInbox() {
        box.open {
            inbox = true
            scope.launch {
                page.animateTo(1f, if (reducedMotion) snap() else tween(OpenMs, easing = EaseOut))
            }
        }
    }

    fun closeInbox() {
        if (!box.isOpen) return
        inbox = false
        mail.collapseAll()
        box.close()
        scope.launch {
            page.animateTo(0f, if (reducedMotion) snap() else tween(CloseMs, easing = EaseOut))
        }
    }

    fun toggleInbox() = if (box.isOpen) closeInbox() else openInbox()

    DisposableEffect(state, mail, box) {
        state.setControlTapHandler { control ->
            when (control) {
                MailboxDemos.Arrive -> deliver()
                MailboxDemos.OpenBox -> finger.tap(targets.centre(MailboxTarget)) { toggleInbox() }
                MailboxDemos.ReadFirst -> {
                    val first = mail.letters.firstOrNull { !it.read }
                    if (first != null) {
                        finger.tap(targets.centre(rowTarget(first.id))) { mail.toggle(first.id) }
                    }
                }
                MailboxDemos.MarkAllRead ->
                    finger.tap(targets.centre(MarkAllTarget)) { mail.readAll() }
                MailboxDemos.Back ->
                    finger.tap(targets.centre(BackTarget), leave = targets.leave()) {
                        closeInbox()
                    }
            }
        }
        state.setFeedResetHandler {
            inbox = false
            page.snapTo(0f)
            mail.reset()
            box.reset()
            fresh = null
            finger.hide()
        }
        onDispose {
            state.setControlTapHandler(null)
            state.setFeedResetHandler(null)
        }
    }

    NavigationBackHandler(
        state = rememberNavigationEventState(NavigationEventInfo.None),
        isBackEnabled = inbox,
        onBackCompleted = { closeInbox() },
    )

    val unread = mail.unread
    val boxLabel =
        if (unread > 0) stringResource(Res.string.mailbox_inbox_unread, unread)
        else stringResource(Res.string.mailbox_inbox_none)
    val newMessage =
        if (announcement.isEmpty()) ""
        else stringResource(Res.string.mailbox_new_message, announcement)
    val touch = LabTheme.colors.touch

    Box(
        Modifier.fillMaxSize()
            .background(colors.background)
            .windowInsetsPadding(demoInsets(state))
            .onGloballyPositioned { targets.root = it }
            .onPreviewKeyEvent {
                if (it.type == KeyEventType.KeyDown && it.key == Key.Escape && inbox) {
                    closeInbox()
                    true
                } else false
            },
    ) {
        BoxWithConstraints(Modifier.fillMaxSize().padding(top = BarHeight)) {
            val width = constraints.maxWidth.toFloat()
            HomeFeed(
                colors,
                Modifier.fillMaxSize()
                    .graphicsLayer {
                        translationX = -page.value * HomeDrift * width
                        alpha = 1f - page.value * HomeFade
                    }
                    .then(if (inbox) Modifier.clearAndSetSemantics {} else Modifier),
            )
            InboxPage(
                mail,
                fresh,
                colors,
                reducedMotion,
                targets,
                Modifier.fillMaxSize()
                    .graphicsLayer { translationX = (1f - page.value) * width }
                    .background(colors.background)
                    .then(if (inbox) Modifier else Modifier.clearAndSetSemantics {}),
            )
        }
        TopBar(
            inbox = inbox,
            unread = unread,
            colors = colors,
            onBack = { closeInbox() },
            targets = targets,
        ) {
            Mailbox(
                box,
                unread = unread,
                letters = mail.letters.size,
                contentDescription = boxLabel,
                dark = dark,
                onClick = { toggleInbox() },
                modifier = Modifier.onGloballyPositioned { targets[MailboxTarget] = it },
            )
        }
        // Arrivals, for a screen reader.
        Box(
            Modifier.size(1.dp).semantics {
                liveRegion = LiveRegionMode.Polite
                contentDescription = newMessage
            },
        )
        Canvas(Modifier.fillMaxSize()) { with(finger) { drawFinger(touch) } }
    }
}

@Composable
private fun TopBar(
    inbox: Boolean,
    unread: Int,
    colors: MailboxScreenColors,
    onBack: () -> Unit,
    targets: Targets,
    mailbox: @Composable () -> Unit,
) {
    val swap by animateFloatAsState(if (inbox) 1f else 0f, tween(SwapMs))
    Row(
        Modifier.fillMaxWidth()
            .height(BarHeight)
            .background(colors.background)
            .hairline(colors.line)
            .padding(start = 12.dp, end = 8.dp, top = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(44.dp)) {
            Box(
                Modifier.padding(6.dp)
                    .size(32.dp)
                    .alpha(1f - swap)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(MailboxFaces.avatarTop, MailboxFaces.avatarBottom),
                        ),
                    )
                    .clearAndSetSemantics {},
                contentAlignment = Alignment.Center,
            ) {
                BasicText("M", style = AvatarStyle.copy(color = MailboxFaces.initials))
            }
            if (inbox || swap > 0f) {
                val backLabel = stringResource(Res.string.mailbox_back)
                Canvas(
                    Modifier.size(44.dp)
                        .alpha(swap)
                        .clip(CircleShape)
                        .onGloballyPositioned { targets[BackTarget] = it }
                        .then(
                            if (inbox) {
                                Modifier.clickable(role = Role.Button, onClick = onBack).semantics {
                                    contentDescription = backLabel
                                }
                            } else Modifier,
                        ),
                ) {
                    drawChevron(colors.ink)
                }
            }
        }
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            BasicText(
                stringResource(
                    if (inbox) Res.string.mailbox_inbox_title else Res.string.mailbox_home_title,
                ),
                Modifier.semantics { heading() },
                style = BarTitleStyle.copy(color = colors.ink),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            BasicText(
                when {
                    !inbox -> stringResource(Res.string.mailbox_home_subtitle)
                    unread > 0 -> stringResource(Res.string.mailbox_unread, unread)
                    else -> stringResource(Res.string.mailbox_caught_up)
                },
                style = BarSubtitleStyle.copy(color = colors.muted),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(8.dp))
        mailbox()
    }
}

// The bar's hairline along its foot.
private fun Modifier.hairline(color: Color) = drawBehind {
    val y = size.height - 0.5.dp.toPx()
    drawLine(color, Offset(0f, y), Offset(size.width, y), 1.dp.toPx())
}

private fun DrawScope.drawChevron(color: Color) {
    val s = 24.dp.toPx() / 24f
    val ox = (size.width - 24.dp.toPx()) / 2f
    val oy = (size.height - 24.dp.toPx()) / 2f
    val path =
        Path().apply {
            moveTo(ox + 14.5f * s, oy + 5.5f * s)
            lineTo(ox + 8f * s, oy + 12f * s)
            lineTo(ox + 14.5f * s, oy + 18.5f * s)
        }
    drawPath(path, color, style = Stroke(2.1f * s, cap = StrokeCap.Round, join = StrokeJoin.Round))
}

@Composable
private fun HomeFeed(colors: MailboxScreenColors, modifier: Modifier) {
    Column(
        modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Column {
            BasicText(
                Feed.greeting,
                Modifier.semantics { heading() },
                style = GreetingStyle.copy(color = colors.ink),
            )
            BasicText(Feed.date, style = DateStyle.copy(color = colors.muted))
        }
        for ((i, item) in Feed.cards.withIndex()) {
            Column(
                Modifier.fillMaxWidth()
                    .clip(CardShape)
                    .background(colors.card)
                    .border(1.dp, colors.line, CardShape)
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                BasicText(item.kicker.uppercase(), style = KickerStyle.copy(color = colors.accent))
                BasicText(item.title, style = CardTitleStyle.copy(color = colors.ink))
                BasicText(item.text, style = CardTextStyle.copy(color = colors.muted))
                if (i == 0) Faces(colors)
            }
        }
    }
}

@Composable
private fun Faces(colors: MailboxScreenColors) {
    Row(Modifier.padding(top = 4.dp).clearAndSetSemantics {}) {
        for ((i, initials) in Feed.faces.withIndex()) {
            Box(
                Modifier.offset(x = (-6).dp * i)
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(colors.card)
                    .padding(2.dp)
                    .clip(CircleShape)
                    .background(MailboxFaces.neighbors[i]),
                contentAlignment = Alignment.Center,
            ) {
                BasicText(initials, style = FaceStyle.copy(color = MailboxFaces.initials))
            }
        }
    }
}

@Composable
private fun InboxPage(
    mail: NeighborMail,
    fresh: Int?,
    colors: MailboxScreenColors,
    reducedMotion: Boolean,
    targets: Targets,
    modifier: Modifier,
) {
    val unread = mail.unread
    Column(modifier) {
        Row(
            Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            BasicText(
                if (unread > 0) stringResource(Res.string.mailbox_unread, unread)
                else stringResource(Res.string.mailbox_no_unread),
                style = UnreadStyle.copy(color = colors.muted),
            )
            BasicText(
                stringResource(Res.string.mailbox_mark_all_read),
                Modifier.clip(PillShape)
                    .background(colors.chip)
                    .onGloballyPositioned { targets[MarkAllTarget] = it }
                    .clickable(enabled = unread > 0, role = Role.Button) { mail.readAll() }
                    .alpha(if (unread > 0) 1f else 0.4f)
                    .padding(horizontal = 12.dp, vertical = 7.dp),
                style = PillStyle.copy(color = colors.ink),
            )
        }
        Column(
            Modifier.fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 8.dp, end = 8.dp, bottom = 24.dp),
        ) {
            for (letter in mail.letters) {
                key(letter.id) {
                    LetterRow(
                        letter,
                        slideIn = letter.id == fresh && !reducedMotion,
                        colors = colors,
                        onClick = { mail.toggle(letter.id) },
                        modifier =
                            Modifier.onGloballyPositioned { targets[rowTarget(letter.id)] = it },
                    )
                }
            }
        }
    }
}

@Composable
private fun LetterRow(
    letter: Letter,
    slideIn: Boolean,
    colors: MailboxScreenColors,
    onClick: () -> Unit,
    modifier: Modifier,
) {
    val enter = remember { Animatable(if (slideIn) 0f else 1f) }
    LaunchedEffect(Unit) { enter.animateTo(1f, tween(SlideInMs, easing = SlideInEasing)) }
    val dot by animateFloatAsState(if (letter.read) 0f else 1f, tween(DotMs))
    val label =
        if (letter.read) stringResource(Res.string.mailbox_row, letter.from, letter.subject)
        else stringResource(Res.string.mailbox_row_unread, letter.from, letter.subject)
    Row(
        modifier
            .fillMaxWidth()
            .graphicsLayer {
                alpha = enter.value
                translationY = (enter.value - 1f) * 10.dp.toPx()
            }
            .clip(RowShape)
            .background(if (letter.expanded) colors.chip else Color.Transparent)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = label }
            .padding(horizontal = 10.dp, vertical = 11.dp),
    ) {
        Box(Modifier.width(14.dp).padding(top = 7.dp)) {
            Canvas(Modifier.size(8.dp)) {
                val a = dot
                if (a > 0f) {
                    drawCircle(
                        colors.accent.copy(alpha = a),
                        radius = size.minDimension / 2f * (0.4f + 0.6f * a),
                    )
                }
            }
        }
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                BasicText(
                    letter.from,
                    Modifier.weight(1f),
                    style =
                        FromStyle.copy(
                            color = colors.ink,
                            fontWeight = if (letter.read) FontWeight.Medium else FromWeight,
                        ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.width(8.dp))
                BasicText(
                    letter.time,
                    Modifier.padding(top = 2.dp),
                    style = TimeStyle.copy(color = colors.muted),
                )
            }
            BasicText(
                letter.subject,
                style = SubjectStyle.copy(color = if (letter.read) colors.muted else colors.ink),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (letter.expanded) {
                BasicText(
                    letter.body,
                    Modifier.padding(top = 6.dp),
                    style = BodyStyle.copy(color = colors.ink),
                )
            }
        }
    }
}

/** Where the scripted finger's targets are, by name, so it can find them on the stage. */
@Stable
private class Targets {
    var root: LayoutCoordinates? = null
    private val nodes = mutableStateMapOf<String, LayoutCoordinates>()

    operator fun set(name: String, node: LayoutCoordinates) {
        nodes[name] = node
    }

    fun centre(name: String): Offset? {
        val stage = root ?: return null
        val node = nodes[name] ?: return null
        if (!node.isAttached || !stage.isAttached) return null
        return stage.localPositionOf(node, Offset(node.size.width / 2f, node.size.height / 2f))
    }

    /** Where the finger drifts to as it leaves: a little right of the middle, as the prototype. */
    fun leave(): Offset? {
        val stage = root ?: return null
        return Offset(stage.size.width * 0.6f, stage.size.height * 0.55f)
    }
}

/**
 * The scripted fingertip: it glides to each target in a low arc, comes down, and lifts, as the
 * prototype's does. Nothing but the script moves it, and it is gone once it lifts for the last
 * time.
 */
@Stable
private class ScriptFinger {
    private val alpha = Animatable(0f)
    private var at by mutableStateOf(Offset.Zero)
    private var placed = false

    // Which gesture has the fingertip, so one cancelled mid-way doesn't hide the next one's.
    private var gesture = 0

    /**
     * Glides to [target], taps it with [action] as it comes down, and, if [leave] is given, drifts
     * there and fades away.
     */
    suspend fun tap(target: Offset?, leave: Offset? = null, action: () -> Unit) {
        val mine = ++gesture
        try {
            target ?: return
            if (!placed) {
                at = Offset(target.x * 0.9f, target.y + 260f)
                placed = true
            }
            if (alpha.value < 1f) alpha.animateTo(1f, tween(ScriptedTouch.DownMs))
            glide(target, GlideMs)
            action()
            delay(ScriptedTouch.TapHoldMs)
            if (leave != null) {
                glide(leave, LeaveMs, lift = 0f)
                alpha.animateTo(0f, tween(ScriptedTouch.UpMs))
                placed = false
            }
        } finally {
            if (mine == gesture && leave != null) {
                withContext(NonCancellable) { alpha.snapTo(0f) }
            }
        }
    }

    suspend fun hide() {
        ++gesture
        placed = false
        alpha.snapTo(0f)
    }

    private suspend fun glide(to: Offset, ms: Int, lift: Float = ArcLift) {
        val from = at
        val progress = Animatable(0f)
        progress.animateTo(1f, tween(ms, easing = EaseInOut)) {
            val p = value
            at =
                Offset(
                    from.x + (to.x - from.x) * p,
                    from.y + (to.y - from.y) * p - sin(p * PI.toFloat()) * lift,
                )
        }
    }

    fun DrawScope.drawFinger(color: Color) {
        val a = alpha.value
        if (a <= 0f) return
        drawTouch(color, at, a)
    }
}

/** The feed, as the prototype writes it: an invented street and its news. */
private object Feed {
    const val greeting = "Good morning, Maya"
    const val date = "Saturday, Oct 18 · 62°F and clear"
    val faces = listOf("DW", "ML", "RJ", "+9")

    class Card(val kicker: String, val title: String, val text: String)

    val cards =
        listOf(
            Card(
                "Today · 9:00 AM",
                "Porch swap on Elm",
                "Bring what you don't need, take what you do. 214 Elm St, under the big sycamore.",
            ),
            Card(
                "Lost & found",
                "Blue bike helmet near Cedar Park",
                "Kid's size, dinosaur stickers. It's on the bench by the drinking fountain.",
            ),
            Card(
                "Maple Grove Library",
                "Story hour at 11:00 AM",
                "This week: pumpkins, a hayride song, and a surprise guest from the fire station.",
            ),
        )
}

private const val MailboxTarget = "mailbox"
private const val MarkAllTarget = "mark-all"
private const val BackTarget = "back"

private fun rowTarget(id: Int) = "row-$id"

private val BarHeight = 70.dp

// The home feed drifts this share of the width left and fades this much as the inbox comes over.
private const val HomeDrift = 0.22f
private const val HomeFade = 0.6f

private const val OpenMs = 380
private const val CloseMs = 320
private const val SwapMs = 200
private const val SlideInMs = 500
private const val DotMs = 300
private const val GlideMs = 650
private const val LeaveMs = 600

// The finger's arc rises this many pixels at its middle.
private const val ArcLift = 40f

private val EaseOut = CubicBezierEasing(0.33f, 1f, 0.68f, 1f)
private val EaseInOut = CubicBezierEasing(0.45f, 0f, 0.55f, 1f)
private val SlideInEasing = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1f)

private val CardShape = RoundedCornerShape(16.dp)
private val RowShape = RoundedCornerShape(12.dp)
private val PillShape = RoundedCornerShape(999.dp)

private val FromWeight = FontWeight(650)

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

private val AvatarStyle = css(14f, FontWeight(650))
private val BarTitleStyle = css(17f, FontWeight(650)).copy(letterSpacing = (-0.01).em)
private val BarSubtitleStyle = css(12.5f)
private val GreetingStyle = css(24f, FontWeight.Bold).copy(letterSpacing = (-0.015).em)
private val DateStyle = css(13.5f)
private val KickerStyle = css(11f, FontWeight(650)).copy(letterSpacing = 0.07.em)
private val CardTitleStyle = css(15.5f, FontWeight(620))
private val CardTextStyle = css(13.5f, lines = 1.4f)
private val FaceStyle = css(10.5f, FontWeight(650))
private val UnreadStyle = css(13f)
private val PillStyle = css(13f, FontWeight.SemiBold, lines = 1.2f)
private val FromStyle = css(15f)
private val TimeStyle = css(12f)
private val SubjectStyle = css(13.5f)
private val BodyStyle = css(13.5f, lines = 1.45f)
