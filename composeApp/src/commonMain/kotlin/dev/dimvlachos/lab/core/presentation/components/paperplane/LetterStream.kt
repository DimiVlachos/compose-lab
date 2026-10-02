package dev.dimvlachos.lab.core.presentation.components.paperplane

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.util.lerp
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * The letters of sent messages on their way into the send button: lifted out of the text where they
 * were typed, the nearest first, each arcing into the button, tumbling, shrinking and fading as it
 * goes in.
 */
@Stable
class LetterStream internal constructor() {
    internal val pours = mutableStateListOf<Pour>()

    /** Where the stream is drawn, set by its stage as it is laid out. */
    internal var stage: Offset by mutableStateOf(Offset.Zero)

    /**
     * Pours the text laid out as [layout], its top left at [origin] in the root, into wherever
     * [into] is as each letter flies, calling [arrived] as each goes in; only the letters [shown]
     * (in the root), if given: a field scrolled through a long text shows only some of it. Returns
     * once the last one has.
     */
    suspend fun pour(
        layout: TextLayoutResult,
        origin: Offset,
        into: () -> Offset,
        arrived: () -> Unit,
        shown: Rect = Rect.Zero,
    ) {
        val target = into()
        val boxes =
            letterBoxes(layout).filter { shown == Rect.Zero || shown.contains(origin + it.center) }
        if (boxes.isEmpty()) return
        val nearestFirst = boxes.sortedBy { (origin + it.center - target).getDistance() }
        val gap = letterGap(nearestFirst.size)
        val letters = nearestFirst.mapIndexed { i, box -> Letter(box, i * gap, spinFor(i)) }
        val total = letters.last().start + PaperPlaneDimens.LetterMs
        val pour = Pour(layout, origin, into, letters)
        pours += pour
        // Each letter in on the stream's own clock, so with animations off they are all in at
        // once, as the stream is.
        var inside = 0
        fun arrivals(clock: Float) {
            while (
                inside < letters.size && clock >= letters[inside].start + PaperPlaneDimens.LetterMs
            ) {
                inside++
                arrived()
            }
        }
        try {
            pour.clock.animateTo(total, tween(total.toInt(), easing = LinearEasing)) {
                arrivals(value)
            }
            arrivals(total)
        } finally {
            pours -= pour
        }
    }
}

@Composable fun rememberLetterStream(): LetterStream = remember { LetterStream() }

/** Where a [LetterStream]'s letters are drawn: lay it over the field and the button. */
@Composable
fun LetterStage(stream: LetterStream, modifier: Modifier = Modifier) {
    Spacer(
        modifier
            .onGloballyPositioned { stream.stage = it.positionInRoot() }
            .drawBehind {
                for (pour in stream.pours) {
                    val clock = pour.clock.value
                    val origin = pour.origin - stream.stage
                    val into = pour.into() - stream.stage
                    for (letter in pour.letters) {
                        val p =
                            ((clock - letter.start) / PaperPlaneDimens.LetterMs).coerceIn(0f, 1f)
                        if (p >= 1f) continue
                        // Drawn into the button: it gathers speed as it goes.
                        val e = p * p
                        val from = origin + letter.box.center
                        val bend =
                            (from + into) / 2f -
                                Offset(
                                    0f,
                                    PaperPlaneDimens.LetterLift * density +
                                        abs(into.x - from.x) * 0.04f,
                                )
                        val u = 1f - e
                        val at = from * (u * u) + bend * (2f * u * e) + into * (e * e)
                        val scale = lerp(1f, PaperPlaneDimens.LetterEndScale, e)
                        val fade = 1f - ease((p - 0.65f) / 0.35f)
                        withTransform({
                            rotate(letter.spin * e, at)
                            scale(scale, scale, at)
                            translate(at.x - from.x, at.y - from.y)
                        }) {
                            translate(origin.x, origin.y) {
                                with(pour.atlas) { drawLetter(letter.box, fade) }
                            }
                        }
                    }
                }
            }
    )
}

internal class Pour(
    val layout: TextLayoutResult,
    val origin: Offset,
    val into: () -> Offset,
    val letters: List<Letter>,
) {
    /** Milliseconds since the first letter left. */
    val clock = Animatable(0f)

    /** What its letters are drawn from. */
    val atlas = LetterAtlas(layout)
}

/** One letter: its box in the text, when it leaves, ms after the first, and how far it tumbles. */
internal class Letter(val box: Rect, val start: Float, val spin: Float)

/**
 * The box of every letter in [layout] with ink to it, spaces left behind. A letter is what the eye
 * takes for one: a character made of two code units, and a cluster drawn as one (an emoji of
 * several, a flag, a letter and its accent), which some platforms give each part of the full box
 * of, others the first part only.
 */
internal fun letterBoxes(layout: TextLayoutResult): List<Rect> {
    val text = layout.layoutInput.text.text
    val boxes = mutableListOf<Rect>()
    var i = 0
    while (i < text.length) {
        val c = text[i]
        val pair = c.isHighSurrogate() && i + 1 < text.length && text[i + 1].isLowSurrogate()
        if (!c.isWhitespace()) {
            var box = layout.getBoundingBox(i)
            if (pair) box = box.union(layout.getBoundingBox(i + 1))
            boxes += box
        }
        i += if (pair) 2 else 1
    }
    return oneEach(boxes)
}

/**
 * The letters among the boxes of a text's characters, in order: one box for each part of a letter
 * drawn as one is one letter, whether every part was given the whole box or the first part only
 * (and the rest none).
 */
internal fun oneEach(boxes: List<Rect>): List<Rect> {
    val letters = mutableListOf<Rect>()
    for (box in boxes) {
        val last = letters.lastOrNull()
        when {
            box.width <= 0f -> Unit
            last != null && last.sharesInkWith(box) -> letters[letters.lastIndex] = last.union(box)
            else -> letters += box
        }
    }
    return letters
}

// Two boxes on the same line that overlap by more than a sliver are parts of one letter.
private fun Rect.sharesInkWith(other: Rect): Boolean {
    val across = min(right, other.right) - max(left, other.left)
    val sameLine = min(bottom, other.bottom) - max(top, other.top) > 0f
    return sameLine && across > SameLetter * min(width, other.width)
}

private const val SameLetter = 0.5f

/**
 * The time between letters leaving, in ms: a steady stream, closed up for a long message so the
 * whole of it is in within the stream's time.
 */
internal fun letterGap(count: Int): Float {
    if (count <= 1) return 0f
    val room = PaperPlaneDimens.StreamMs - PaperPlaneDimens.LetterMs
    return min(PaperPlaneDimens.LetterGapMs, room / (count - 1))
}

// Each letter tumbles its own way, some one way, some the other, up to a quarter turn; the same
// way each time, so a replay looks the same.
internal fun spinFor(i: Int): Float {
    val r = ((i * 37 + 11) % 19) / 9f - 1f
    return r * PaperPlaneDimens.MaxSpin
}

private fun Rect.union(other: Rect) =
    Rect(
        min(left, other.left),
        min(top, other.top),
        max(right, other.right),
        max(bottom, other.bottom),
    )
