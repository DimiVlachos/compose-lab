package dev.dimvlachos.lab.core.presentation.components.zipper

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/** Something the zipper does that is heard or felt. */
internal enum class ZipperEvent {
    /** The tab falls flat on the tape hard; its strength is how hard, 0 to 1. */
    Clack,
    /** A softer knock of the tab on the tape, as it bounces. */
    Tick,
    /** The pull reached a new letter while held. */
    Letter,
    /** Teeth passed the pull: its strength is how many, 1 to 4, since the last rasp. */
    Teeth,
    /** The pull was taken and its tab lifts off the tape. */
    Lift,
}

/**
 * The zipper's springs and gestures, ported from the prototype's `st`, `step()` and pointer
 * handlers. [pos] is how far down the list is, 0 to 1; the pull's top sits at [sliderTopFor] of it
 * on a track the height of the list area ([layout], in dp). Held, the tab ([flip], radians) springs
 * up off the tape and the tag ([tag], 0 to 1) swings out, swaying ([sway]) with the pull's speed;
 * let go, the tab falls under a constant pull and bounces on the tape.
 *
 * The list is the truth for [pos] while nothing here drives it: each frame [follow] hands it in. A
 * drag or a glide moves [pos] itself and leaves [scrollRequest] for the list to follow.
 */
@Stable
internal class ZipperState {
    var reducedMotion = false

    /** Hears what the zipper does, with the event's strength. */
    var onEvent: ((ZipperEvent, Float) -> Unit)? = null

    /** Bumped on every [step], so a drawing that reads it redraws each frame. */
    var frame by mutableIntStateOf(0)
        private set

    /** Bumped whenever something sets the zipper moving, so a resting frame loop wakes. */
    var wake by mutableIntStateOf(0)
        private set

    // ---- Geometry, in dp of the list area ----
    var height = 0f
        private set

    val trackTop = 8f
    var trackBot = 0f
        private set

    var range = 1f
        private set

    /** Lays the track along a list area [height] dp tall. */
    fun layout(height: Float) {
        if (height == this.height) return
        this.height = height
        trackBot = height - 8f
        range = max(1f, trackBot - trackTop - SliderHeight - Tab - 6f)
        sy = sliderTopFor(pos)
        wake++
    }

    fun sliderTopFor(p: Float) = trackTop + p * range

    fun posForY(y: Float) = ((y - trackTop - SliderHeight / 2f) / range).coerceIn(0f, 1f)

    /** Where the middle of the pull's tab is, which the script's finger takes. */
    fun pullCenter() = sliderTopFor(pos) + SliderHeight + 12f

    // ---- State ----
    var pos = 0f
        private set

    /** Whether a finger holds the pull: the tab stands and the tag shows. */
    var held by mutableStateOf(false)
        private set

    var flip = 0f
        private set

    var flipV = 0f
        private set

    var tag = 0f
        private set

    var sway = 0f
        private set

    /** The letter at the top of the list, which the tag shows. */
    var letter by mutableStateOf("A")
        private set

    /** How many times the tab has clacked down. */
    var clacks = 0
        private set

    /** Where a glide is taking the pull, or null. */
    var glide: Float? = null
        private set

    /** Where [pos] wants the list to be, once the list follows it; null while the list leads. */
    var scrollRequest: Float? = null
        private set

    private var tagV = 0f
    private var swayV = 0f
    private var glideV = 0f
    private var sy = sliderTopFor(0f)
    private var syV = 0f
    private var teethAcc = 0f
    private var time = 0f
    private var lastZip = -1000f
    private var drag: Drag? = null

    private class Drag(val y0: Float, var moved: Boolean, var grab: Boolean, var off: Float)

    /** The list's own position, while nothing here is moving it. */
    fun follow(listPos: Float) {
        if (scrollRequest != null) return
        pos = listPos.coerceIn(0f, 1f)
    }

    /** The list has followed [scrollRequest]. */
    fun takeScrollRequest(): Float? = scrollRequest.also { scrollRequest = null }

    private fun setPos(p: Float) {
        pos = p.coerceIn(0f, 1f)
        scrollRequest = pos
    }

    /** The list's letter is now [l]; held, a new letter ticks. */
    fun showLetter(l: String) {
        if (l == letter) return
        letter = l
        if (held) emit(ZipperEvent.Letter, .5f)
    }

    // ---- Input, as the prototype's down/move/up ----

    /** A finger down on the strip at [y]: on or near the pull it takes it. */
    fun down(y: Float) {
        wake++
        glide = null
        glideV = 0f
        val top = sliderTopFor(pos)
        val near = y > top - 14f && y < top + SliderHeight + Tab + 10f
        drag = Drag(y, false, near, if (near) y - (top + SliderHeight / 2f) else 0f)
        if (near) hold(true)
    }

    fun move(y: Float) {
        val d = drag ?: return
        wake++
        val dy = y - d.y0
        // Crossing the slop keeps the origin, so a fast first move still counts in full.
        if (!d.moved && abs(dy) > Slop) {
            d.moved = true
            if (!d.grab) {
                d.grab = true
                d.off = 0f
                hold(true)
            }
        }
        if (!d.moved) return
        setPos(posForY(y - d.off))
    }

    /** The finger lifts at [y]: a tap on the tape glides the pull there. */
    fun up(y: Float) {
        val d = drag ?: return
        wake++
        drag = null
        if (!d.moved && !d.grab) {
            glide = posForY(y)
            glideV = 0f
            return
        }
        hold(false)
    }

    /** The gesture was taken away. */
    fun cancel() {
        if (drag == null) return
        drag = null
        wake++
        hold(false)
    }

    /** A finger on the list itself: whatever the zipper was doing stops. */
    fun listTouched() {
        glide = null
        glideV = 0f
        if (drag != null) {
            drag = null
            hold(false)
        }
    }

    /** Glides the pull to [p] by itself, as a key or a tap does. */
    fun glideTo(p: Float) {
        wake++
        drag = null
        glide = p.coerceIn(0f, 1f)
        glideV = 0f
    }

    private fun hold(on: Boolean) {
        if (held == on) return
        held = on
        if (on) {
            flipV = max(flipV, 0f)
            emit(ZipperEvent.Lift)
        }
    }

    /** Back at the top, zipped shut, at rest. */
    fun reset() {
        drag = null
        held = false
        glide = null
        glideV = 0f
        pos = 0f
        scrollRequest = 0f
        flip = 0f
        flipV = 0f
        tag = 0f
        tagV = 0f
        sway = 0f
        swayV = 0f
        sy = sliderTopFor(0f)
        syV = 0f
        clacks = 0
        teethAcc = 0f
        letter = "A"
        wake++
        frame++
    }

    /** Nothing would move on the next frame, so the frame loop can stop until [wake] changes. */
    val settled: Boolean
        get() =
            !held &&
                drag == null &&
                glide == null &&
                scrollRequest == null &&
                flip == 0f &&
                flipV == 0f &&
                abs(tag) < Rest &&
                abs(tagV) < Rest &&
                abs(sway) < Rest &&
                abs(swayV) < Rest &&
                abs(syV) < .5f

    /** Advances [seconds] in the prototype's 1/240 s substeps, then rasps for the teeth passed. */
    fun step(seconds: Float) {
        time += seconds * 1000f
        var left = seconds
        while (left > 0f) {
            val h = min(left, Substep)
            substep(h)
            left -= h
        }
        val n = floor(teethAcc)
        if (n >= 1f) {
            teethAcc -= n
            if (time - lastZip > 28f) {
                lastZip = time
                emit(ZipperEvent.Teeth, min(4f, n))
            }
        }
        frame++
    }

    private fun substep(dt: Float) {
        glide?.let { g ->
            if (reducedMotion) {
                setPos(g)
                glide = null
            } else {
                val p = pos
                glideV += (240f * (g - p) - 2f * sqrt(240f) * .95f * glideV) * dt
                val np = p + glideV * dt
                setPos(np)
                if (abs(g - np) < .002f && abs(glideV) < .02f) {
                    setPos(g)
                    glide = null
                    glideV = 0f
                }
            }
        }
        val nsy = sliderTopFor(pos)
        val v = (nsy - sy) / max(dt, 1e-4f)
        val acc = (v - syV) / max(dt, 1e-4f)
        // Teeth passing the pull: one rasp grain per tooth.
        teethAcc += abs(nsy - sy) / Pitch
        sy = nsy
        syV += (v - syV) * .35f
        if (reducedMotion) {
            flip = if (held) 1.2f else 0f
            flipV = 0f
            tag = if (held) 1f else 0f
            tagV = 0f
            sway = 0f
            swayV = 0f
            return
        }
        if (held) {
            val k = 260f
            val c = 2f * sqrt(k) * .7f
            flipV += (k * (1.2f - flip) - c * flipV) * dt
            flip += flipV * dt
        } else {
            // A hard stop of a fling flips the tab up off the tape.
            if (abs(acc) > 9000f && abs(syV) > 120f) flipV += min(3f, abs(acc) * .00006f)
            flipV -= 40f * dt
            flip += flipV * dt
            if (flip < 0f) {
                val hit = -flipV
                if (hit > 3.5f) {
                    clacks++
                    emit(ZipperEvent.Clack, min(1f, hit / 9f))
                } else if (hit > 1.4f) {
                    emit(ZipperEvent.Tick, hit / 9f)
                }
                flip = 0f
                flipV = if (hit > 1.4f) hit * .26f else 0f
            }
            if (flip > 1.35f) {
                flip = 1.35f
                flipV = min(0f, flipV)
            }
        }
        run {
            val k = 150f
            val c = 2f * sqrt(k) * .8f
            tagV += (k * ((if (held) 1f else 0f) - tag) - c * tagV) * dt
            tag = (tag + tagV * dt).coerceIn(0f, 1.1f)
        }
        run {
            val k = 55f
            val c = 2f * sqrt(k) * .3f
            val target = (-syV * .0009f).coerceIn(-.4f, .4f)
            swayV += (k * (target - sway) - c * swayV) * dt
            sway += swayV * dt
        }
        // The prototype's springs ring down forever; below these they are drawn as at rest.
        if (!held && abs(tag) < Rest && abs(tagV) < Rest) {
            tag = 0f
            tagV = 0f
        }
        if (abs(sway) < Rest && abs(swayV) < Rest) {
            sway = 0f
            swayV = 0f
        }
    }

    private fun emit(event: ZipperEvent, strength: Float = 1f) {
        onEvent?.invoke(event, strength)
    }

    internal companion object {
        /** The pull's body, its tab, and the teeth's pitch, in dp. */
        const val SliderHeight = 30f
        const val Tab = 34f
        const val Pitch = 5.2f

        /** How far a finger moves before a press on the strip is a drag. */
        const val Slop = 5f

        private const val Substep = 1f / 240f
        private const val Rest = 1e-3f
    }
}
