package dev.dimvlachos.lab.paperfandemo.presentation.components

import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
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
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import dev.dimvlachos.lab.core.demo.DemoState
import dev.dimvlachos.lab.core.demo.demoInsets
import dev.dimvlachos.lab.core.platform.rememberReducedMotion
import dev.dimvlachos.lab.core.platform.rememberSoundPlayer
import dev.dimvlachos.lab.core.presentation.components.paperfan.FanSlots
import dev.dimvlachos.lab.core.presentation.components.paperfan.FaqEntry
import dev.dimvlachos.lab.core.presentation.components.paperfan.PaperFanFaq
import dev.dimvlachos.lab.core.presentation.components.paperfan.PaperFanSounds
import dev.dimvlachos.lab.core.presentation.components.paperfan.PaperFanState
import dev.dimvlachos.lab.core.presentation.components.touch.drawTouch
import dev.dimvlachos.lab.core.presentation.ui.LabTheme
import dev.dimvlachos.lab.core.presentation.ui.PaperFanScreenColors
import dev.dimvlachos.lab.paperfandemo.PaperFanDemos
import dev.dimvlachos.lab.resources.Res
import dev.dimvlachos.lab.resources.paperfan_a1
import dev.dimvlachos.lab.resources.paperfan_a2
import dev.dimvlachos.lab.resources.paperfan_a3
import dev.dimvlachos.lab.resources.paperfan_a4
import dev.dimvlachos.lab.resources.paperfan_a5
import dev.dimvlachos.lab.resources.paperfan_back
import dev.dimvlachos.lab.resources.paperfan_collapsed
import dev.dimvlachos.lab.resources.paperfan_expanded
import dev.dimvlachos.lab.resources.paperfan_heading
import dev.dimvlachos.lab.resources.paperfan_help_avatar
import dev.dimvlachos.lab.resources.paperfan_help_subtitle
import dev.dimvlachos.lab.resources.paperfan_help_title
import dev.dimvlachos.lab.resources.paperfan_intro
import dev.dimvlachos.lab.resources.paperfan_q1
import dev.dimvlachos.lab.resources.paperfan_q2
import dev.dimvlachos.lab.resources.paperfan_q3
import dev.dimvlachos.lab.resources.paperfan_q4
import dev.dimvlachos.lab.resources.paperfan_q5
import dev.dimvlachos.lab.resources.paperfan_title
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource

private val Questions =
    listOf(
        Res.string.paperfan_q1 to Res.string.paperfan_a1,
        Res.string.paperfan_q2 to Res.string.paperfan_a2,
        Res.string.paperfan_q3 to Res.string.paperfan_a3,
        Res.string.paperfan_q4 to Res.string.paperfan_a4,
        Res.string.paperfan_q5 to Res.string.paperfan_a5,
    )

/**
 * The prototype's help page for an invented outdoor outfitter: five questions whose expand arrows
 * are paper fans, and a chat button under them. The script's taps and its sweep of a fan go through
 * the same fans a finger does, with a scripted fingertip shown over them.
 */
@Composable
internal fun PaperFanDemo(
    state: DemoState,
    fans: PaperFanState = remember { PaperFanState(Questions.size) },
    reducedMotion: Boolean = rememberReducedMotion(),
) {
    val sounds = rememberSoundPlayer(PaperFanSounds)
    SideEffect { fans.reducedMotion = reducedMotion }
    val colors = if (LabTheme.isDark) PaperFanScreenColors.Dark else PaperFanScreenColors.Light
    val entries = Questions.map { (q, a) -> FaqEntry(stringResource(q), stringResource(a)) }
    val slots = remember { FanSlots(Questions.size) }
    val finger = remember { ScriptFinger() }
    val density = LocalDensity.current.density
    val where = remember { Where() }

    DisposableEffect(state, fans, finger) {
        state.setControlTapHandler { control ->
            val tapped = PaperFanDemos.tapped(control)
            val folded = PaperFanDemos.folded(control)
            when {
                tapped != null -> {
                    if (!finger.shown) {
                        where.screen()?.let { finger.at = Offset(it.x * .72f, it.y * .9f) }
                        finger.shown = true
                    }
                    where.rowPoint(slots, tapped, density)?.let {
                        finger.glide(it, PaperFanDemos.GlideMs)
                    }
                    finger.down = true
                    delay(PaperFanDemos.PressMs)
                    finger.down = false
                    fans.toggle(tapped)
                }
                folded != null -> foldByHand(fans, folded, slots, finger, where, density)
            }
        }
        // Every fan folded and the fingertip gone, so the clip loops.
        state.setFeedResetHandler {
            fans.reset()
            finger.shown = false
            finger.down = false
        }
        onDispose {
            state.setControlTapHandler(null)
            state.setFeedResetHandler(null)
        }
    }

    val touch = LabTheme.colors.touch
    Box(Modifier.fillMaxSize().onGloballyPositioned { where.root = it }) {
        Column(
            Modifier.fillMaxSize()
                .background(colors.background)
                .windowInsetsPadding(demoInsets(state)),
        ) {
            AppBar(colors)
            Column(Modifier.padding(start = 22.dp, end = 22.dp, top = 4.dp, bottom = 10.dp)) {
                Text(
                    stringResource(Res.string.paperfan_heading),
                    Modifier.semantics { heading() },
                    color = colors.ink,
                    fontSize = 23.sp,
                    lineHeight = 1.15.em,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-.01).em,
                )
                Text(
                    stringResource(Res.string.paperfan_intro),
                    Modifier.padding(top = 4.dp),
                    color = colors.muted,
                    fontSize = 13.5.sp,
                    lineHeight = BodyLine,
                )
            }
            PaperFanFaq(
                entries = entries,
                state = fans,
                sounds = sounds,
                colors = colors,
                expanded = stringResource(Res.string.paperfan_expanded),
                collapsed = stringResource(Res.string.paperfan_collapsed),
                modifier =
                    Modifier.weight(1f).fillMaxWidth().onGloballyPositioned { where.faq = it },
                slots = slots,
            )
            HelpChip(colors)
        }
        Canvas(Modifier.matchParentSize().clearAndSetSemantics {}) {
            if (finger.shown) drawTouch(touch, finger.at, if (finger.down) 1f else .85f)
        }
    }
}

/** Where the demo's parts are, for the scripted fingertip to find them. */
private class Where {
    var root: LayoutCoordinates? = null
    var faq: LayoutCoordinates? = null

    /** The screen's size, in px. */
    fun screen(): Offset? = root?.size?.let { Offset(it.width.toFloat(), it.height.toFloat()) }

    /** A point given in the questions' own dp, on the screen in px. */
    fun fromFaq(x: Float, y: Float, density: Float): Offset? {
        val r = root?.takeIf { it.isAttached } ?: return null
        val f = faq?.takeIf { it.isAttached } ?: return null
        return r.localPositionOf(f, Offset(x * density, y * density))
    }

    /** Where a finger taps question [i]: a little left of the middle of its row. */
    fun rowPoint(slots: FanSlots, i: Int, density: Float): Offset? =
        fromFaq(slots.rowX[i] + slots.rowW[i] * .4f, slots.rowY[i] + slots.rowH[i] * .55f, density)
}

/**
 * The prototype's sweep: the finger rides question [i]'s row up as the answer above folds away,
 * reaches for the open fan's leading guard and sweeps it round to the left, folding it shut.
 */
private suspend fun foldByHand(
    fans: PaperFanState,
    i: Int,
    slots: FanSlots,
    finger: ScriptFinger,
    where: Where,
    density: Float,
) {
    var start = -1L
    while (true) {
        val now = withFrameNanos { it }
        if (start < 0) start = now
        where.rowPoint(slots, i, density)?.let { finger.at = it }
        if ((now - start) / 1_000_000 >= PaperFanDemos.RideMs) break
    }
    val rr = slots.radius * .78f
    fun on(a: Float) =
        where.fromFaq(slots.pivotX[i] + cos(a) * rr, slots.pivotY[i] + sin(a) * rr, density)
    on(-.18f)?.let { finger.glide(it, PaperFanDemos.ReachMs) }
    finger.down = true
    if (!fans.dragStart(i)) {
        finger.down = false
        return
    }
    try {
        animate(0f, 1f, animationSpec = tween(PaperFanDemos.SweepMs, easing = QuadInOut)) { p, _ ->
            val a = -.18f + (-PI.toFloat() + .3f + .18f) * p
            on(a)?.let { finger.at = it }
            fans.dragTo(i, PaperFanState.dragFraction(cos(a), sin(a)))
        }
    } finally {
        fans.dragEnd(i, moved = true)
        finger.down = false
    }
    delay(PaperFanDemos.LeaveMs)
    finger.shown = false
}

/** The scripted fingertip, as the prototype's: it glides, presses and lifts. */
@Stable
private class ScriptFinger {
    var at by mutableStateOf(Offset.Zero)
    var shown by mutableStateOf(false)
    var down by mutableStateOf(false)

    suspend fun glide(to: Offset, ms: Int) {
        val from = at
        animate(0f, 1f, animationSpec = tween(ms, easing = QuadInOut)) { p, _ ->
            at = Offset(from.x + (to.x - from.x) * p, from.y + (to.y - from.y) * p)
        }
    }
}

/** The help page's bar: back, and its title. */
@Composable
private fun AppBar(colors: PaperFanScreenColors) {
    val back = stringResource(Res.string.paperfan_back)
    Row(
        Modifier.fillMaxWidth().padding(start = 6.dp, end = 14.dp, top = 16.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Canvas(Modifier.size(44.dp).clearAndSetSemantics { contentDescription = back }) {
            chevron(colors.ink, 22.dp.toPx(), left = true)
        }
        Text(
            stringResource(Res.string.paperfan_title),
            Modifier.weight(1f),
            color = colors.ink,
            fontSize = 15.5.sp,
            lineHeight = BodyLine,
            fontWeight = FontWeight(650),
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.size(44.dp))
    }
}

/** The chat button under the questions: Dana's initial, who she is, and an arrow. */
@Composable
private fun HelpChip(colors: PaperFanScreenColors) {
    Row(
        Modifier.padding(start = 14.dp, end = 14.dp, bottom = 18.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(colors.chip)
            .semantics(mergeDescendants = true) {}
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier.size(36.dp)
                .clip(RoundedCornerShape(50))
                .drawBehind {
                    // CSS linear-gradient(140deg, …) across the circle's box.
                    val a = 140f * PI.toFloat() / 180f
                    val dx = sin(a)
                    val dy = -cos(a)
                    val half =
                        (kotlin.math.abs(size.width * dx) + kotlin.math.abs(size.height * dy)) / 2f
                    val c = Offset(size.width / 2f, size.height / 2f)
                    drawRect(
                        Brush.linearGradient(
                            colors.avatar,
                            start = c - Offset(dx, dy) * half,
                            end = c + Offset(dx, dy) * half,
                        ),
                    )
                }
                .clearAndSetSemantics {},
            contentAlignment = Alignment.Center,
        ) {
            Text(
                stringResource(Res.string.paperfan_help_avatar),
                color = colors.avatarInk,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        Column(Modifier.weight(1f)) {
            Text(
                stringResource(Res.string.paperfan_help_title),
                color = colors.ink,
                fontSize = 14.sp,
                lineHeight = BodyLine,
                fontWeight = FontWeight.Bold,
            )
            Text(
                stringResource(Res.string.paperfan_help_subtitle),
                color = colors.muted,
                fontSize = 12.5.sp,
                lineHeight = BodyLine,
            )
        }
        Canvas(Modifier.size(18.dp).clearAndSetSemantics {}) {
            chevron(colors.muted, size.width, left = false)
        }
    }
}

/** The prototype's chevron path in a 24-unit box drawn [box] px wide, centred: back or forward. */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.chevron(
    color: Color,
    box: Float,
    left: Boolean,
) {
    val s = box / 24f
    val o = Offset((size.width - box) / 2f, (size.height - box) / 2f)
    val w = 2.1f * s
    val (x0, x1) = if (left) 14.5f to 8f else 9.5f to 16f
    drawLine(color, o + Offset(x0 * s, 5.5f * s), o + Offset(x1 * s, 12f * s), w, StrokeCap.Round)
    drawLine(color, o + Offset(x1 * s, 12f * s), o + Offset(x0 * s, 18.5f * s), w, StrokeCap.Round)
}

/** The prototype's `ease()`: quadratic in and out. */
private val QuadInOut =
    androidx.compose.animation.core.Easing { t ->
        if (t < .5f) 2f * t * t else 1f - (-2f * t + 2f) * (-2f * t + 2f) / 2f
    }

/** The prototype's body line height. */
private val BodyLine = 1.55.em
