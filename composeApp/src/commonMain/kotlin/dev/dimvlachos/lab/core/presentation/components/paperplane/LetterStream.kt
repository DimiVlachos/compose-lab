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
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.drawText
import androidx.compose.ui.util.lerp
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
     * Pours the text laid out as [layout], its top left at [origin] in the root, into [into],
     * calling [arrived] as each letter goes in. Returns once the last one has.
     */
    suspend fun pour(layout: TextLayoutResult, origin: Offset, into: Offset, arrived: () -> Unit) {
        val boxes = letterBoxes(layout)
        if (boxes.isEmpty()) return
        val nearestFirst = boxes.sortedBy { (origin + it.center - into).getDistance() }
        val gap = letterGap(nearestFirst.size)
        val letters = nearestFirst.mapIndexed { i, box -> Letter(box, i * gap, spinFor(i)) }
        val total = letters.last().start + PaperPlaneDimens.LetterMs
        val pour = Pour(layout, origin, into, letters)
        pours += pour
        try {
            coroutineScope {
                launch {
                    var at = 0f
                    for (letter in letters) {
                        val inside = letter.start + PaperPlaneDimens.LetterMs
                        delay((inside - at).toLong())
                        at = inside
                        arrived()
                    }
                }
                pour.clock.animateTo(total, tween(total.toInt(), easing = LinearEasing))
            }
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
                    val into = pour.into - stream.stage
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
                            clipRect(
                                origin.x + letter.box.left,
                                origin.y + letter.box.top,
                                origin.x + letter.box.right,
                                origin.y + letter.box.bottom,
                            ) {
                                drawText(pour.layout, topLeft = origin, alpha = fade)
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
    val into: Offset,
    val letters: List<Letter>,
) {
    /** Milliseconds since the first letter left. */
    val clock = Animatable(0f)
}

/** One letter: its box in the text, when it leaves, ms after the first, and how far it tumbles. */
internal class Letter(val box: Rect, val start: Float, val spin: Float)

/**
 * The box of every letter in [layout] with ink to it, spaces left behind; a character made of two
 * code units is one letter.
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
            if (box.width > 0f) boxes += box
        }
        i += if (pair) 2 else 1
    }
    return boxes
}

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
