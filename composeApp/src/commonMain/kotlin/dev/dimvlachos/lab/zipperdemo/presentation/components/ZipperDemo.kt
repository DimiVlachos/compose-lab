package dev.dimvlachos.lab.zipperdemo.presentation.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import dev.dimvlachos.lab.core.demo.DemoState
import dev.dimvlachos.lab.core.demo.demoInsets
import dev.dimvlachos.lab.core.platform.rememberReducedMotion
import dev.dimvlachos.lab.core.platform.rememberSoundPlayer
import dev.dimvlachos.lab.core.presentation.components.touch.ScriptedTouch
import dev.dimvlachos.lab.core.presentation.components.touch.drawTouch
import dev.dimvlachos.lab.core.presentation.components.zipper.Zipper
import dev.dimvlachos.lab.core.presentation.components.zipper.ZipperInset
import dev.dimvlachos.lab.core.presentation.components.zipper.ZipperSounds
import dev.dimvlachos.lab.core.presentation.components.zipper.ZipperState
import dev.dimvlachos.lab.core.presentation.components.zipper.ZipperTrack
import dev.dimvlachos.lab.core.presentation.ui.LabTheme
import dev.dimvlachos.lab.core.presentation.ui.ZipperScreenColors
import dev.dimvlachos.lab.resources.Res
import dev.dimvlachos.lab.resources.zipper_add
import dev.dimvlachos.lab.resources.zipper_label
import dev.dimvlachos.lab.resources.zipper_list
import dev.dimvlachos.lab.resources.zipper_me
import dev.dimvlachos.lab.resources.zipper_next
import dev.dimvlachos.lab.resources.zipper_previous
import dev.dimvlachos.lab.resources.zipper_search
import dev.dimvlachos.lab.resources.zipper_tab_contacts
import dev.dimvlachos.lab.resources.zipper_tab_favorites
import dev.dimvlachos.lab.resources.zipper_tab_keypad
import dev.dimvlachos.lab.resources.zipper_tab_recents
import dev.dimvlachos.lab.resources.zipper_title
import dev.dimvlachos.lab.zipperdemo.ContactItem
import dev.dimvlachos.lab.zipperdemo.Contacts
import dev.dimvlachos.lab.zipperdemo.ZipperDemos
import kotlin.math.pow
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/**
 * The prototype's Contacts screen: 88 people in a lazy list with a brass zipper sewn down its right
 * edge as the fast scroller. The script's finger takes the same pull and flicks the same list a
 * finger does.
 */
@Composable
internal fun ZipperDemo(
    state: DemoState,
    list: LazyListState = rememberLazyListState(),
    zipper: ZipperState = remember { ZipperState() },
    reducedMotion: Boolean = rememberReducedMotion(),
) {
    val sounds = rememberSoundPlayer(ZipperSounds)
    SideEffect { zipper.reducedMotion = reducedMotion }
    val density = LocalDensity.current.density
    val track =
        remember(list) {
            ZipperTrack(list, Contacts.kinds, estimates(density), Contacts.sectionLetters)
        }
    val finger = remember { ScriptFinger() }
    // The list area's width in dp, for where the script's finger finds the zipper.
    val area = remember { FloatArray(1) }

    DisposableEffect(state, zipper, list) {
        state.setControlTapHandler { control ->
            val drag = ZipperDemos.dragged(control)
            val tap = ZipperDemos.tapped(control)
            val cx = area[0] - ZipperInset
            when {
                control == ZipperDemos.Grab -> {
                    val c = zipper.pullCenter()
                    finger.place(Offset(cx - 60f, c + 120f))
                    finger.moveTo(Offset(cx, c), ZipperDemos.ReachMs)
                    finger.press(true)
                    zipper.down(c)
                }
                drag != null -> {
                    val (to, ms) = drag
                    val from = finger.at.y
                    val y1 = zipper.sliderTopFor(to) + ZipperState.SliderHeight / 2f + GrabOffset
                    playOver(ms) { p ->
                        val y = from + (y1 - from) * Ease.transform(p)
                        finger.at = Offset(cx, y)
                        zipper.move(y)
                    }
                }
                control == ZipperDemos.Release -> {
                    finger.press(false)
                    zipper.up(finger.at.y)
                }
                control == ZipperDemos.Flick -> {
                    // An ordinary flick on the list: the pull just follows.
                    val lx = area[0] * .45f
                    val ly = zipper.height * .7f
                    finger.moveTo(Offset(lx, ly), ZipperDemos.FlickReachMs)
                    finger.press(true)
                    val y0 = finger.at.y
                    val px = density
                    var done = 0f
                    playOver(ZipperDemos.FlickMs, LinearEasing) { p ->
                        finger.at = Offset(lx, y0 - 120f * p)
                        list.dispatchRawDelta((300f * p - done) * px)
                        done = 300f * p
                    }
                    finger.press(false)
                    playOver(ZipperDemos.CoastMs, LinearEasing) { p ->
                        val to = 300f + 420f * (1f - (1f - p).pow(3))
                        list.dispatchRawDelta((to - done) * px)
                        done = to
                    }
                    delay(300)
                    finger.hide()
                }
                tap != null -> {
                    // A tap on the tape, away from the pull: the pull glides there.
                    val y = zipper.sliderTopFor(tap) + ZipperState.SliderHeight / 2f
                    finger.moveTo(Offset(cx, y), ZipperDemos.TapReachMs)
                    finger.press(true)
                    zipper.down(y)
                    delay(ScriptedTouch.TapHoldMs)
                    finger.press(false)
                    zipper.up(y)
                    delay(ZipperDemos.TapGlideMs.toLong())
                    finger.hide()
                }
            }
        }
        state.setFeedResetHandler {
            finger.hide()
            zipper.reset()
        }
        onDispose {
            state.setControlTapHandler(null)
            state.setFeedResetHandler(null)
        }
    }

    val colors = if (LabTheme.isDark) ZipperScreenColors.Dark else ZipperScreenColors.Light
    Column(
        Modifier.fillMaxSize().background(colors.background).windowInsetsPadding(demoInsets(state)),
    ) {
        AppBar(colors)
        Search(colors)
        MeCard(colors)
        Box(
            Modifier.weight(1f).fillMaxWidth().onSizeChanged { area[0] = it.width / density },
        ) {
            ContactList(
                list,
                colors,
                Modifier.pointerInput(zipper) {
                    // Touching the list itself takes over from the zipper's glide or drag.
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                        zipper.listTouched()
                    }
                },
            )
            Zipper(
                zipper,
                track,
                label = stringResource(Res.string.zipper_label),
                nextLetter = stringResource(Res.string.zipper_next),
                previousLetter = stringResource(Res.string.zipper_previous),
                sounds = sounds,
                modifier = Modifier.fillMaxSize(),
            )
            val touch = colors.ink
            Canvas(Modifier.fillMaxSize().clearAndSetSemantics {}) {
                with(finger) { drawFinger(touch) }
            }
        }
        Tabs(colors)
    }
}

/** Where the script's finger holds the pull, below the middle of its body: on the tab. */
private const val GrabOffset = ZipperState.SliderHeight / 2f + 12f

/** Each row kind's height in px before one has been laid out, from the prototype's CSS. */
private fun estimates(density: Float) =
    floatArrayOf(33.15f * density, 68.6f * density, 69.6f * density)

/** The prototype's `ease`: in and out, quadratic. */
private val Ease = Easing { t -> if (t < .5f) 2f * t * t else 1f - (-2f * t + 2f).pow(2) / 2f }

private suspend fun playOver(ms: Int, easing: Easing = LinearEasing, frame: (Float) -> Unit) {
    val a = Animatable(0f)
    a.animateTo(1f, tween(ms, easing = easing)) { frame(value) }
}

/** The scripted fingertip, in the list area's dp: it moves where the script takes it. */
@Stable
private class ScriptFinger {
    var at by mutableStateOf(Offset.Zero)
    private val alpha = Animatable(0f)
    private var pressed by mutableStateOf(false)

    fun place(to: Offset) {
        at = to
    }

    suspend fun moveTo(to: Offset, ms: Int) {
        if (alpha.value < 1f) alpha.snapTo(1f)
        val from = at
        playOver(ms, Ease) { p -> at = from + (to - from) * p }
    }

    fun press(down: Boolean) {
        pressed = down
    }

    suspend fun hide() {
        pressed = false
        alpha.animateTo(0f, tween(ScriptedTouch.UpMs))
    }

    fun DrawScope.drawFinger(color: Color) {
        val a = alpha.value
        if (a <= 0f) return
        val k = if (pressed) 1f else .75f
        drawTouch(color, Offset(at.x * density, at.y * density), a * k)
    }
}

@Composable
private fun AppBar(colors: ZipperScreenColors) {
    Row(
        Modifier.fillMaxWidth().padding(start = 20.dp, end = 12.dp, top = 18.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            stringResource(Res.string.zipper_title),
            Modifier.semantics { heading() },
            color = colors.ink,
            fontSize = 27.sp,
            lineHeight = 1.1.em,
            fontWeight = FontWeight(750),
            letterSpacing = (-.015).em,
        )
        val add = stringResource(Res.string.zipper_add)
        Box(
            Modifier.size(44.dp)
                .clip(CircleShape)
                .clickable(role = Role.Button) {}
                .semantics { contentDescription = add },
            contentAlignment = Alignment.Center,
        ) {
            Canvas(Modifier.size(24.dp)) {
                val s = size.width / 24f
                val w = 2.1f * s
                drawLine(
                    colors.accent,
                    Offset(12f * s, 5f * s),
                    Offset(12f * s, 19f * s),
                    w,
                    StrokeCap.Round,
                )
                drawLine(
                    colors.accent,
                    Offset(5f * s, 12f * s),
                    Offset(19f * s, 12f * s),
                    w,
                    StrokeCap.Round,
                )
            }
        }
    }
}

@Composable
private fun Search(colors: ZipperScreenColors) {
    Row(
        Modifier.padding(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 8.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(colors.chip)
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Canvas(Modifier.size(17.dp)) {
            val s = size.width / 24f
            drawCircle(colors.muted, 6.5f * s, Offset(11f * s, 11f * s), style = Stroke(2f * s))
            drawLine(
                colors.muted,
                Offset(16f * s, 16f * s),
                Offset(20f * s, 20f * s),
                2f * s,
                StrokeCap.Round,
            )
        }
        Text(
            stringResource(Res.string.zipper_search, Contacts.people.size),
            color = colors.muted,
            fontSize = 14.5.sp,
            lineHeight = BodyLine,
            maxLines = 1,
        )
    }
}

@Composable
private fun MeCard(colors: ZipperScreenColors) {
    val shape = RoundedCornerShape(14.dp)
    Row(
        Modifier.padding(start = 16.dp, end = 16.dp, bottom = 6.dp)
            .fillMaxWidth()
            .clip(shape)
            .background(colors.card)
            .border(1.dp, colors.line, shape)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Avatar(Contacts.MeInitials, Contacts.MeAvatar, 44.dp, 15.sp)
        Spacer(Modifier.width(12.dp))
        Column {
            Text(
                Contacts.MeName,
                color = colors.ink,
                fontSize = 15.sp,
                lineHeight = BodyLine,
                fontWeight = FontWeight.Bold,
            )
            Text(
                stringResource(Res.string.zipper_me),
                color = colors.muted,
                fontSize = 12.5.sp,
                lineHeight = BodyLine,
            )
        }
    }
}

@Composable
private fun Avatar(initials: String, color: Color, size: Dp, fontSize: TextUnit) {
    Box(
        Modifier.size(size).clip(CircleShape).background(color).clearAndSetSemantics {},
        contentAlignment = Alignment.Center,
    ) {
        Text(
            initials,
            color = Color.White,
            fontSize = fontSize,
            fontWeight = FontWeight(650),
            lineHeight = BodyLine,
        )
    }
}

@Composable
private fun ContactList(list: LazyListState, colors: ZipperScreenColors, modifier: Modifier) {
    val label = stringResource(Res.string.zipper_list)
    LazyColumn(
        modifier.fillMaxSize().testTag("contacts").semantics { contentDescription = label },
        state = list,
        contentPadding = PaddingValues(end = 52.dp, bottom = 16.dp),
    ) {
        itemsIndexed(Contacts.items, contentType = { _, it -> it::class }) { _, item ->
            when (item) {
                is ContactItem.Header ->
                    Text(
                        item.letter,
                        Modifier.fillMaxWidth()
                            .padding(start = 20.dp, top = 10.dp, bottom = 3.dp)
                            .semantics { heading() },
                        color = colors.muted,
                        fontSize = 13.sp,
                        lineHeight = BodyLine,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = .04.em,
                    )
                is ContactItem.Person -> PersonRow(item, colors)
            }
        }
    }
}

@Composable
private fun PersonRow(item: ContactItem.Person, colors: ZipperScreenColors) {
    val c = item.contact
    Row(
        Modifier.fillMaxWidth()
            .heightIn(min = 56.dp)
            .padding(start = 18.dp, top = 7.dp, bottom = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Avatar(c.initials, c.avatar, 40.dp, 14.sp)
        Spacer(Modifier.width(12.dp))
        Column(
            Modifier.weight(1f)
                .then(
                    if (item.first) Modifier
                    else
                        Modifier.drawBehind {
                                drawRect(colors.line, size = Size(size.width, 1.dp.toPx()))
                            }
                            .padding(top = 1.dp),
                )
                .padding(vertical = 6.dp),
        ) {
            Text(
                c.name,
                color = colors.ink,
                fontSize = 15.sp,
                lineHeight = BodyLine,
                fontWeight = FontWeight(550),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                c.sub,
                color = colors.muted,
                fontSize = 12.5.sp,
                lineHeight = BodyLine,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** The app's tab bar, as the prototype draws it: decoration, hidden from screen readers. */
@Composable
private fun Tabs(colors: ZipperScreenColors) {
    Column(Modifier.fillMaxWidth().clearAndSetSemantics {}) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(colors.line))
        Row(
            Modifier.fillMaxWidth()
                .background(colors.card)
                .padding(start = 4.dp, end = 4.dp, top = 6.dp, bottom = 12.dp),
        ) {
            for ((i, tab) in TabSpecs.withIndex()) {
                val on = i == 2
                val ink = if (on) colors.accent else colors.muted
                Column(
                    Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Canvas(Modifier.size(22.dp)) { tab.draw(this, ink) }
                    Text(
                        stringResource(tab.label),
                        color = ink,
                        fontSize = 10.5.sp,
                        lineHeight = BodyLine,
                        fontWeight = if (on) FontWeight(650) else FontWeight.Normal,
                    )
                }
            }
        }
    }
}

private class TabSpec(val label: StringResource, val draw: DrawScope.(Color) -> Unit)

private fun svg(d: String): Path = PathParser().parsePathString(d).toPath()

private val StarPath = svg("m12 4 2.4 5 5.4.6-4 3.7 1.1 5.3L12 16l-4.9 2.6 1.1-5.3-4-3.7 5.4-.6z")
private val HandsPath = svg("M12 7.5V12l3 2")
private val BodyPath = svg("M5 19.5c1.2-3.6 4-5.2 7-5.2s5.8 1.6 7 5.2")
private val KeypadDots =
    listOf(
        7f to 6f,
        12f to 6f,
        17f to 6f,
        7f to 11f,
        12f to 11f,
        17f to 11f,
        7f to 16f,
        12f to 16f,
        17f to 16f,
        12f to 20.5f,
    )

// The icons in their 24-unit SVG boxes, drawn into 22 dp.
private inline fun DrawScope.inBox(block: DrawScope.() -> Unit) {
    val s = size.width / 24f
    scale(s, s, Offset.Zero) { block() }
}

private val TabSpecs =
    listOf(
        TabSpec(Res.string.zipper_tab_favorites) { ink ->
            inBox { drawPath(StarPath, ink, style = Stroke(1.7f, join = StrokeJoin.Round)) }
        },
        TabSpec(Res.string.zipper_tab_recents) { ink ->
            inBox {
                drawCircle(ink, 8f, Offset(12f, 12f), style = Stroke(1.7f))
                drawPath(HandsPath, ink, style = Stroke(1.7f, cap = StrokeCap.Round))
            }
        },
        TabSpec(Res.string.zipper_tab_contacts) { ink ->
            inBox {
                drawCircle(ink, 3.6f, Offset(12f, 9f))
                drawPath(BodyPath, ink)
            }
        },
        TabSpec(Res.string.zipper_tab_keypad) { ink ->
            inBox { for ((x, y) in KeypadDots) drawCircle(ink, 1.6f, Offset(x, y)) }
        },
    )

/** The prototype's body line height, which every line on the screen inherits. */
private val BodyLine = 1.55.em
