package dev.dimvlachos.lab.core.presentation.components.typewriter

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.delete
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

/** What the machine does that is heard and felt: the composer plays each as a sound and haptics. */
internal interface TypewriterFeel {
    /** A type bar strikes the paper; [variant] picks one of its sounds, and a space is softer. */
    fun strike(variant: Int, space: Boolean) = Unit

    /** The margin bell, five letters before the margin. */
    fun bell() = Unit

    /** The lever is pushed: the paper feeds a line and the carriage is sent home. */
    fun carriageReturn() = Unit

    /** The carriage bangs into its stop. */
    fun bang() = Unit

    /** The correction tape lifts a letter off. */
    fun tape() = Unit

    /** The ratchet clicks as the paper moves; [pulling] when a finger is pulling the sheet. */
    fun ratchet(pulling: Boolean) = Unit

    /** The sheet is pulled free of the roller. */
    fun release() = Unit

    /** The sheet is full: a key that would type off it is refused. */
    fun jam() = Unit
}

/**
 * A sheet sent from the machine: what was on it, and where it was as it came free. The composer
 * hands it over with [bounds] in the root, for a thread to fly it into its place.
 */
@Immutable
public class SentSheet(
    /** The message, as typed, without the spaces round it. */
    public val text: String,
    /** The sheet's lines, as they were typed on it, blank ones at the foot left off. */
    public val lines: List<String>,
    /** The whole sheet, in px in the root (in units from the machine's top left, inside it). */
    public val bounds: Rect,
) {
    /** Its letters, each where it was struck. */
    internal val glyphs: List<Glyph> = glyphsOf(lines)
}

/** Where the sheet is in being sent. */
internal enum class SheetPhase {
    /** In the roller, to type on. */
    Idle,

    /** Pulled free, zipping up out of the roller. */
    Zipping,

    /** A fresh sheet feeding in. */
    Feeding,
}

/**
 * A typewriter used as a chat composer: its [field] is what is typed, and the machine follows it,
 * striking each letter onto its sheet as it is typed, ringing its bell near the margin, returning
 * its carriage at the margin and lifting letters off with its correction tape when they are
 * deleted. The sheet is sent by pulling it up out of the roller ([grab], [drag], [release]), by
 * [send], which pulls it out by itself, or by a scripted fingertip ([pullScripted]). Its physics is
 * stepped at a fixed 120 Hz by [advance], which the composer calls every frame while it is [awake],
 * and everything that moves is read while drawing, so only a change of [inked], [jammed] or [phase]
 * recomposes.
 */
@Stable
public class TypewriterState(text: String = "") {
    /**
     * What is typed: edited only by the machine's keys and a hardware keyboard, never the phone's.
     */
    public val field: TextFieldState = TextFieldState(text)

    /** What is typed, as the machine last saw it, and as it lies on the sheet. */
    private var typed = text
    internal var sheet = layOutSheet(text)
        private set

    /** Whether there is anything on the sheet to send. */
    public var inked: Boolean by mutableStateOf(sheet.inked)
        private set

    /**
     * What a screen reader should hear of the typing since it last heard: letters struck, letters
     * deleted, a refused key. The composer reads it out once the typing pauses.
     */
    internal var speech by mutableStateOf(Speech.Quiet)

    /** Whether a key was just refused for want of room: the hint says the sheet is full. */
    public var jammed: Boolean by mutableStateOf(false)
        private set

    internal var phase by mutableStateOf(SheetPhase.Idle)
        private set

    /**
     * How big a machine unit is, as the composer last laid the machine out: a thread its slips fly
     * into draws them in the same size of type.
     */
    public var unit: Dp by mutableStateOf(1.dp)
        internal set

    /** The keyboard the machine is typed on. */
    internal val keyboard = TypewriterKeyboard()

    /** Hears what is heard and felt. */
    internal var feel: TypewriterFeel = object : TypewriterFeel {}

    /** Hears each sheet as it comes free of the roller. */
    internal var onSent: ((SentSheet) -> Unit)? = null

    /** Bumped every step, so whatever draws the machine is drawn again. */
    internal var frame by mutableIntStateOf(0)
        private set

    /** Whether anything is moving: the frame loop runs while it is. */
    internal var awake by mutableStateOf(false)
        private set

    /** The machine's clock, in seconds since it was made. */
    internal var now = 0f
        private set

    // The carriage: where the sheet's left edge is, in units, how fast it moves, and where it is
    // making for. It may be held where it is while the lever is pushed, and then sent home on the
    // loose return spring.
    internal var carriage = columnX(sheet.column)
        private set

    private var carriageSpeed = 0f
    private var target = carriage
    private var heldUntil = Float.NEGATIVE_INFINITY
    private var heldAt = carriage
    private var homing = false

    // The paper's feed: how far it has rolled up, in units, to show the line being typed on.
    internal var feed = sheet.line * TypewriterDimens.LineHeight
        private set

    private var feedSpeed = 0f

    /**
     * How far the paper has fed, as the layout sees it: it changes only as the paper feeds, so the
     * composer is laid out again only then, not as the carriage moves.
     */
    internal var fed by mutableFloatStateOf(feed)
        private set

    // The finger's pull on the sheet, and the sheet's own spring back once it lets go.
    private var pull = 0f
    private var pullSpeed = 0f
    private var held = false
    private var autoPullAt = Float.NEGATIVE_INFINITY
    private var autoPulling = false

    // Sending: the sheet zips up from where it was let go, and a fresh one feeds in.
    private var phaseAt = 0f
    private var zipFrom = 0f
    internal var zip = 0f
        private set

    internal var feedIn = 0f
        private set

    // Where the ratchet last clicked, as far as the paper had moved.
    private var ratchetAt = paperTravel()

    // A strike: when the type bar flew and from where in the basket, 0 at its left to 1 at its
    // right; when the ribbon was lifted, and whether it was the correction tape's half.
    internal var barAt = Float.NEGATIVE_INFINITY
        private set

    internal var barFrom = 0.5f
        private set

    internal var ribbonAt = Float.NEGATIVE_INFINITY
        private set

    internal var taping = false
        private set

    // The lever: when it was pushed, and how long it is held over before the carriage goes.
    internal var leverAt = Float.NEGATIVE_INFINITY
        private set

    private var leverHold = TypewriterDimens.LeverShortest

    // The lever by hand: whether a finger has it, how far over it is pushed (0 to 1), whether the
    // pawl has caught and fed a line, and where the carriage was as the push began. Let go, the
    // lever goes on, or springs back, from where the hand left it.
    private var handHeld = false
    private var handPush = 0f
    private var handFed = false
    private var handFrom = 0f
    private var leverFrom = 0f

    // While the hand feeds or takes back a line, the machine doesn't return the carriage or run
    // the tape by itself.
    private var byHand = false

    private var jamAt = Float.NEGATIVE_INFINITY

    // The whole machine shakes a little at each strike, harder at the carriage's bang; the bell's
    // hammer taps its dome; and the ribbon spools turn on a little with every letter.
    private var shudderAt = Float.NEGATIVE_INFINITY
    private var shudderPower = 1f

    internal var bellAt = Float.NEGATIVE_INFINITY
        private set

    internal var struck = 0
        private set

    // Letters struck but not yet on the paper, by place, with when each lands; and letters being
    // lifted off by the tape, with when the tape took each.
    private val landing = FloatArray(TypewriterDimens.MaxLines * TypewriterDimens.Columns)
    private var lastLanding = Float.NEGATIVE_INFINITY
    internal val lifting = mutableListOf<Lifted>()

    init {
        landing.fill(Float.NEGATIVE_INFINITY)
    }

    private var accumulated = 0f

    /**
     * A scripted fingertip on the sheet: where, in units from the machine's top left, and how far
     * down.
     */
    internal var touch = Offset.Zero
        private set

    internal var touchAlpha = 0f
        private set

    /** How far the sheet is out of the roller: the pull, as the paper gives to it. */
    internal val shown: Float
        get() = PullToSend.shown(pull, sheet.inked)

    /** Where the sheet's top is, in units from the machine's top. */
    internal val sheetTop: Float
        get() = TypewriterDimens.SheetTop - feed - shown - zip + feedIn

    /** How far the lever is pushed over, 0 at rest to 1 as far as it goes. */
    internal val leverPush: Float
        get() {
            if (handHeld) return handPush
            val t = now - leverAt
            if (t < 0f) return 0f
            if (t < leverHold) return leverFrom + (1f - leverFrom) * easeOut(t / leverHold)
            val from = if (leverHold > 0f) 1f else leverFrom
            return from * exp(-(t - leverHold) * LeverEase)
        }

    /** Where the lever's knob is, in units from the machine's top left. */
    internal val leverKnob: Offset
        get() {
            val angle =
                (TypewriterDimens.LeverAngle + TypewriterDimens.LeverSwing * leverPush) * PI / 180.0
            return leverPivot +
                Offset(
                    (cos(angle) * TypewriterDimens.LeverLength).toFloat(),
                    (sin(angle) * TypewriterDimens.LeverLength).toFloat(),
                )
        }

    /** Where the lever pivots, in units from the machine's top left. */
    internal val leverPivot: Offset
        get() = Offset(carriage - TypewriterDimens.LeverOffset, TypewriterDimens.LeverPivotY)

    /** Whether [glyph] has landed on the paper. */
    internal fun landed(glyph: Glyph): Boolean = now >= landing[place(glyph.line, glyph.column)]

    /** Whether the hint should show: the sheet is in, and not pulled out to read it. */
    internal val hintShown: Float
        get() = if (phase == SheetPhase.Idle && shown < HintFade) 1f else 0f

    /**
     * The machine sees [text], the field's text, now: what is new on the end is struck letter by
     * letter, what is gone from the end is lifted off by the tape, and the carriage and the paper
     * go where the text now ends.
     */
    internal fun typedNow(text: CharSequence) {
        val now = text.toString()
        if (now == typed) return
        val before = sheet
        val after = layOutSheet(now)
        val kept = typed.commonPrefixWith(now).length
        val gone = before.glyphs.filter { it.index >= kept }
        val added = after.glyphs.filter { it.index >= kept }
        val deleted = typed.substring(kept)
        speech = speech.said(typed = now.substring(kept), deleted = deleted)
        typed = now
        sheet = after
        inked = after.inked
        if (gone.isNotEmpty()) {
            for (glyph in gone) lifting += Lifted(glyph, this.now)
            ribbonAt = this.now
            taping = true
            feel.tape()
        } else if (deleted.isNotEmpty() && !byHand) {
            // Only a space or a line went, or a space the return took up: the tape still runs.
            feel.tape()
        }
        if (added.isNotEmpty()) {
            for (glyph in added) {
                landing[place(glyph.line, glyph.column)] = this.now + TypewriterDimens.LandSeconds
                lastLanding = this.now + TypewriterDimens.LandSeconds
                lifting.removeAll { it.glyph.line == glyph.line && it.glyph.column == glyph.column }
            }
            val last = added.last()
            barAt = this.now
            barFrom = basketPlace(last.char)
            ribbonAt = this.now
            taping = false
            struck += added.size
            shake(StrikeShudder)
            feel.strike(last.char.lowercaseChar().code % StrikeVariants, space = false)
        } else if (deleted.isEmpty() && after.line == before.line) {
            // Only a space came: the carriage steps without a letter.
            feel.strike(0, space = true)
        }
        if (
            after.line == before.line &&
                before.column < TypewriterDimens.BellColumn &&
                after.column >= TypewriterDimens.BellColumn
        ) {
            bellAt = this.now
            feel.bell()
        }
        target = columnX(after.column)
        when {
            after.line > before.line -> if (!byHand) returnCarriage(after)
            after.line < before.line -> {
                // Back up a line: the paper rolls back, and the carriage steps to the end of it.
                homing = false
                heldUntil = Float.NEGATIVE_INFINITY
            }
            homing || this.now < heldUntil -> {
                // A key while the carriage is on its way home: it stops and steps on from there.
                homing = false
                heldUntil = Float.NEGATIVE_INFINITY
            }
        }
        if (jammed && !after.full) jammed = false
        wake()
    }

    /**
     * Presses [key]: a letter (a capital after Shift), a space or a new line strikes, Backspace
     * runs the tape, Shift holds for the next letter, and the page key turns to the figures and
     * back.
     */
    internal fun press(key: TypeKey) {
        when (val action = key.action) {
            is KeyAction.Type -> {
                val typedIt =
                    type(if (keyboard.shifted) action.char.uppercaseChar() else action.char)
                if (typedIt) keyboard.shifted = false
            }
            KeyAction.Space -> type(' ')
            KeyAction.Return -> type('\n')
            KeyAction.Backspace -> backspace()
            KeyAction.Shift -> keyboard.shifted = !keyboard.shifted
            KeyAction.Page -> {
                keyboard.figures = !keyboard.figures
                keyboard.shifted = false
            }
        }
    }

    /**
     * Types [char] at the end of the text, and says whether it did: not while the sheet is being
     * pulled out by itself or is coming free, nor while a finger has the lever, whose line it would
     * type over, and not off the foot of a full sheet, which jams instead.
     */
    public fun type(char: Char): Boolean {
        if (sending || (handHeld && !byHand)) return false
        val next = field.text.toString() + char
        if (layOutSheet(next).overflow) {
            jam()
            return false
        }
        field.edit { append(char) }
        typedNow(field.text)
        return true
    }

    /**
     * Deletes the last character typed: the carriage runs back and the tape lifts it off. Not while
     * the sheet is being pulled out by itself or is coming free, so what is sent is what was there,
     * nor while a finger has the lever.
     */
    public fun backspace() {
        if (sending || (handHeld && !byHand) || field.text.isEmpty()) return
        field.edit { delete(length - 1, length) }
        typedNow(field.text)
    }

    /**
     * Takes the carriage-return lever with a finger, and says whether it did: not while another
     * finger has it or the sheet, nor while the sheet is being sent.
     */
    public fun grabLever(): Boolean {
        if (handHeld || held || sending || phase != SheetPhase.Idle) return false
        handHeld = true
        handPush = 0f
        handFed = false
        handFrom = carriage
        wake()
        return true
    }

    /**
     * Pushes the held lever [push] of the way over (0 to 1). A third of the way, the line-space
     * pawl catches with a click and the paper feeds a line, or, on a full sheet, the lever jams;
     * further, the lever drags the carriage home.
     */
    public fun pushLever(push: Float) {
        if (!handHeld) return
        handPush = push.coerceIn(0f, 1f)
        if (!handFed && handPush >= TypewriterDimens.LeverPawl) catchPawl()
        if (handFed && feedTaken) {
            val drag =
                ((handPush - TypewriterDimens.LeverPawl) / (1f - TypewriterDimens.LeverPawl))
                    .coerceIn(0f, 1f)
            heldAt = handFrom + (TypewriterDimens.HomeX - handFrom) * drag
            heldUntil = Float.POSITIVE_INFINITY
            homing = false
        }
        wake()
    }

    /**
     * Lets go of the lever, the finger moving right at [speed] travels a second. Pushed past the
     * threshold, or flung, the return goes through: the carriage is let go to bang home as the
     * return's sound does. Short of it, the lever springs back and a line it fed is taken back.
     */
    public fun releaseLever(speed: Float) {
        if (!handHeld) return
        handHeld = false
        val goes =
            handPush >= TypewriterDimens.LeverCommit ||
                (speed > TypewriterDimens.LeverFling && handPush > 0f)
        leverFrom = handPush
        leverAt = now
        if (goes) {
            if (!handFed) catchPawl()
            if (feedTaken) {
                heldAt = min(carriage, TypewriterDimens.HomeX)
                val travel = travelTime(TypewriterDimens.HomeX - heldAt)
                leverHold =
                    max(TypewriterDimens.LeverShortest, TypewriterDimens.ReturnBang - travel)
                heldUntil = now + leverHold
                homing = true
                feel.carriageReturn()
            } else {
                leverHold = 0f
                heldUntil = Float.NEGATIVE_INFINITY
            }
        } else {
            leverHold = 0f
            heldUntil = Float.NEGATIVE_INFINITY
            homing = false
            // Only the line the pawl fed is taken back, if it is still the last thing typed.
            if (handFed && feedTaken && field.text.endsWith('\n')) {
                byHand = true
                backspace()
                byHand = false
            }
        }
        handFed = false
        wake()
    }

    // Whether the pawl's line feed went onto the sheet: a full sheet refuses it.
    private var feedTaken = false

    // The line-space pawl catches: a click under the finger, and a line fed.
    private fun catchPawl() {
        handFed = true
        feel.ratchet(pulling = true)
        byHand = true
        feedTaken = type('\n')
        byHand = false
    }

    /** Starts a new line, as the New line key does: the lever swings over by itself. */
    public fun newLine(): Boolean = type('\n')

    /**
     * A scripted fingertip takes the lever's knob, pushes it all the way over in [seconds] and lets
     * go. Returns once the fingertip has lifted.
     */
    internal suspend fun pushLeverScripted(seconds: Float) {
        try {
            touch = leverKnob
            fade(to = 1f, seconds = TouchDown)
            if (!grabLever()) return
            val start = withFrameNanos { it }
            while (true) {
                val t = withFrameNanos { ((it - start) / 1e9f / seconds).coerceIn(0f, 1f) }
                pushLever(ease(t))
                touch = leverKnob
                if (t >= 1f) break
            }
            releaseLever(0f)
        } finally {
            withContext(NonCancellable) {
                if (handHeld) releaseLever(0f)
                fade(to = 0f, seconds = TouchUp)
            }
        }
    }

    /** A scripted fingertip taps [key]: it dips under the fingertip and is pressed. */
    internal fun tap(key: TypeKey) {
        keyboard.tapped(key.id)
        press(key)
    }

    /** A key that would have typed off a full sheet was refused. */
    internal fun jam() {
        speech = speech.jammed()
        jamAt = now
        jammed = true
        carriageSpeed -= JamKnock
        feel.jam()
        wake()
    }

    // The lever is pushed: the paper feeds a line at once, and the carriage, held a moment at the
    // margin of the line it left, is let go so that it bangs home as the return's sound does.
    private fun returnCarriage(after: SheetLayout) {
        val margin = columnX(after.lines.getOrNull(after.line - 1)?.length ?: 0)
        heldAt = min(carriage, margin)
        val travel = travelTime(TypewriterDimens.HomeX - heldAt)
        leverHold = max(TypewriterDimens.LeverShortest, TypewriterDimens.ReturnBang - travel)
        leverFrom = 0f
        leverAt = now
        heldUntil = now + leverHold
        homing = true
        feel.carriageReturn()
    }

    /**
     * Takes the sheet with a finger, and says whether it did: not while another finger has it, nor
     * while it is being sent.
     */
    public fun grab(): Boolean {
        if (held || handHeld || phase != SheetPhase.Idle || autoPulling) return false
        held = true
        pullSpeed = 0f
        wake()
        return true
    }

    /** Moves the finger holding the sheet [up] units up (negative is down). */
    public fun drag(up: Float) {
        if (!held) return
        pull = (pull + up).coerceAtLeast(0f)
        wake()
    }

    /**
     * Lets go of the sheet, the finger moving up at [speed] units a second: pulled out far enough,
     * or flung, it is sent; otherwise it springs back into the roller.
     */
    public fun release(speed: Float) {
        if (!held) return
        held = false
        if (PullToSend.sends(shown, speed, sheet.inked)) pullOut()
        wake()
    }

    /**
     * Lets go of whatever a finger holds, the sheet or the lever, as if it lifted without moving:
     * for a gesture cut off before it ended, so nothing stays held for good.
     */
    internal fun letGo() {
        if (handHeld) releaseLever(0f)
        if (held) release(0f)
    }

    /**
     * Sends what is typed without a finger: the sheet is pulled out by itself, as Ctrl or ⌘ and
     * Return, or a screen reader's Send, ask. Nothing for a blank sheet, one already on its way, or
     * while a finger has the lever.
     */
    public fun send(): Boolean {
        if (handHeld || phase != SheetPhase.Idle || autoPulling || !sheet.inked) return false
        held = false
        autoPulling = true
        autoPullAt = now
        wake()
        return true
    }

    /**
     * A scripted fingertip takes the sheet, pulls it [up] units up over [seconds] and lets go, as a
     * finger would. Returns once the fingertip has lifted.
     */
    internal suspend fun pullScripted(up: Float, seconds: Float) {
        try {
            touch = Offset(carriage + SheetGrabX, sheetTop + SheetGrabY)
            fade(to = 1f, seconds = TouchDown)
            if (!grab()) return
            var last = withFrameNanos { it }
            val start = last
            var moved = 0f
            var speed = 0f
            while (true) {
                val time = withFrameNanos { it }
                val t = ((time - start) / 1e9f / seconds).coerceIn(0f, 1f)
                val to = up * ease(t)
                val dt = ((time - last) / 1e9f).coerceAtLeast(1e-3f)
                speed = (to - moved) / dt
                drag(to - moved)
                touch = Offset(carriage + SheetGrabX, sheetTop + SheetGrabY)
                moved = to
                last = time
                if (t >= 1f) break
            }
            release(speed)
        } finally {
            withContext(NonCancellable) {
                if (held) release(0f)
                fade(to = 0f, seconds = TouchUp)
            }
        }
    }

    private suspend fun fade(to: Float, seconds: Float) {
        val from = touchAlpha
        val start = withFrameNanos { it }
        while (true) {
            val t = withFrameNanos { ((it - start) / 1e9f / seconds).coerceIn(0f, 1f) }
            touchAlpha = from + (to - from) * t
            wake()
            if (t >= 1f) break
        }
    }

    // The sheet comes free: it zips up out of the roller.
    private fun pullOut() {
        autoPulling = false
        held = false
        // A blank sheet is never sent: it slides back in.
        if (!sheet.inked) return
        zipFrom = TypewriterDimens.SheetTop - feed - shown
        phase = SheetPhase.Zipping
        phaseAt = now
        feel.release()
    }

    /** Steps the machine on by [seconds], at a fixed 120 Hz; with nothing moving, it sleeps. */
    internal fun advance(seconds: Float) {
        accumulated += seconds.coerceIn(0f, TypewriterDimens.MostFrameSeconds)
        val step = TypewriterDimens.StepSeconds
        while (accumulated >= step) {
            accumulated -= step
            now += step
            stepCarriage(step)
            stepPaper(step)
        }
        // The ratchet clicks as the paper passes each tooth, either way; not as it zips out.
        val travel = paperTravel()
        if (phase == SheetPhase.Zipping) {
            ratchetAt = travel
        } else if (PullToSend.ticks(ratchetAt, travel) > 0) {
            feel.ratchet(pulling = held || autoPulling)
            ratchetAt = travel
        }
        lifting.removeAll { now - it.at >= TypewriterDimens.TapeSeconds }
        if (jammed && now - jamAt >= TypewriterDimens.JamSeconds) jammed = false
        if (abs(feed - fed) > FedStep) fed = feed
        frame++
        awake = moving()
    }

    private fun stepCarriage(dt: Float) {
        when {
            now < heldUntil -> {
                carriageSpeed +=
                    (TypewriterDimens.Stiffness * (heldAt - carriage) -
                        TypewriterDimens.Damping * carriageSpeed) * dt
            }
            homing -> {
                val aim = TypewriterDimens.HomeX + TypewriterDimens.ReturnOvershoot
                carriageSpeed +=
                    (TypewriterDimens.ReturnStiffness * (aim - carriage) -
                        TypewriterDimens.ReturnDamping * carriageSpeed) * dt
            }
            else -> {
                carriageSpeed +=
                    (TypewriterDimens.Stiffness * (target - carriage) -
                        TypewriterDimens.Damping * carriageSpeed) * dt
            }
        }
        carriage += carriageSpeed * dt
        if (carriage >= TypewriterDimens.HomeX) {
            val speed = carriageSpeed
            carriage = TypewriterDimens.HomeX
            if (homing && now >= heldUntil) {
                homing = false
                carriageSpeed = -speed * TypewriterDimens.Bounce
                if (speed > BangSpeed) {
                    shake(BangShudder)
                    feel.bang()
                }
            } else {
                carriageSpeed = min(0f, carriageSpeed)
            }
        }
    }

    private fun stepPaper(dt: Float) {
        val line = sheet.line * TypewriterDimens.LineHeight
        feedSpeed +=
            (TypewriterDimens.FeedStiffness * (line - feed) -
                TypewriterDimens.FeedDamping * feedSpeed) * dt
        feed += feedSpeed * dt
        if (autoPulling) {
            val t = (now - autoPullAt) / TypewriterDimens.AutoPullSeconds
            pull = TypewriterDimens.AutoPull * easeOut(t.coerceAtMost(1f))
            if (t >= 1f) pullOut()
        } else if (!held && phase == SheetPhase.Idle && pull > 0f) {
            pullSpeed +=
                (-TypewriterDimens.PullStiffness * pull -
                    TypewriterDimens.PullDamping * pullSpeed) * dt
            pull += pullSpeed * dt
            if (pull <= 0f) {
                pull = 0f
                pullSpeed = 0f
            }
        }
        when (phase) {
            SheetPhase.Zipping -> {
                val t = ((now - phaseAt) / TypewriterDimens.ZipSeconds).coerceAtMost(1f)
                val end = TypewriterDimens.PlatenTop - TypewriterDimens.SheetHeight - ZipClear
                zip = (TypewriterDimens.SheetTop - feed - shown - end) * t * t
                if (t >= 1f) sent()
            }
            SheetPhase.Feeding -> {
                val t = ((now - phaseAt) / TypewriterDimens.FeedInSeconds).coerceAtMost(1f)
                feedIn = TypewriterDimens.FeedIn * (1f - easeOut(t))
                if (t >= 1f) {
                    feedIn = 0f
                    phase = SheetPhase.Idle
                }
            }
            SheetPhase.Idle -> Unit
        }
    }

    // Out of the roller: the sheet goes to whoever is listening, and a fresh one feeds in as the
    // carriage goes home.
    private fun sent() {
        if (!sheet.inked) {
            zip = 0f
            phase = SheetPhase.Idle
            return
        }
        val bounds =
            Rect(
                Offset(carriage, sheetTop),
                Size(TypewriterDimens.SheetWidth, TypewriterDimens.SheetHeight),
            )
        val sheet = SentSheet(typed.trim(), this.sheet.sent, bounds)
        // Cleared as the machine sees it first, so the field's clearing strikes and lifts nothing.
        typed = ""
        this.sheet = SheetLayout.Empty
        inked = false
        jammed = false
        lifting.clear()
        landing.fill(Float.NEGATIVE_INFINITY)
        lastLanding = Float.NEGATIVE_INFINITY
        feed = 0f
        feedSpeed = 0f
        pull = 0f
        pullSpeed = 0f
        zip = 0f
        feedIn = TypewriterDimens.FeedIn
        ratchetAt = paperTravel()
        field.clearText()
        phase = SheetPhase.Feeding
        phaseAt = now
        target = TypewriterDimens.HomeX
        if (TypewriterDimens.HomeX - carriage > HomeSlack) {
            heldAt = carriage
            heldUntil = now
            homing = true
            leverAt = now
            leverFrom = 0f
            leverHold = TypewriterDimens.LeverShortest
            feel.carriageReturn()
        }
        onSent?.invoke(sheet)
    }

    private fun paperTravel(): Float = feed + shown - feedIn

    private fun shake(power: Float) {
        shudderAt = now
        shudderPower = power
    }

    /**
     * How far the whole machine is shaken off its place, as a share of
     * [Shudder][TypewriterDimens.Shudder] down (negative is up): a quick, dying jolt.
     */
    internal val shudder: Float
        get() {
            val t = (now - shudderAt) / TypewriterDimens.ShudderSeconds
            if (t < 0f || t >= 1f) return 0f
            return shudderPower * sin(t * ShudderWaves * PI.toFloat()) * (1f - t)
        }

    /** How far the bell's hammer is swung onto the dome, 0 at rest to 1 striking it. */
    internal val hammer: Float
        get() {
            val t = (now - bellAt) / HammerSeconds
            if (t < 0f || t >= 1f) return 0f
            return if (t < HammerStrike) t / HammerStrike
            else 1f - ease((t - HammerStrike) / (1f - HammerStrike))
        }

    // Whether the sheet is on its way out, by itself or freed: no key changes it then.
    private val sending: Boolean
        get() = autoPulling || phase == SheetPhase.Zipping

    private fun moving(): Boolean =
        held ||
            handHeld ||
            autoPulling ||
            phase != SheetPhase.Idle ||
            homing ||
            now < heldUntil ||
            abs(target - carriage) > Still ||
            abs(carriageSpeed) > Still ||
            abs(sheet.line * TypewriterDimens.LineHeight - feed) > Still ||
            abs(feedSpeed) > Still ||
            pull > 0f ||
            now < lastLanding ||
            lifting.isNotEmpty() ||
            touchAlpha > 0f ||
            now - barAt < TypewriterDimens.TypebarSeconds ||
            now - ribbonAt < TypewriterDimens.RibbonSeconds ||
            now - leverAt < leverHold + LeverSettle ||
            now - shudderAt < TypewriterDimens.ShudderSeconds ||
            now - bellAt < HammerSeconds ||
            jammed

    private fun wake() {
        awake = true
        frame++
    }

    internal companion object {
        const val StrikeVariants = 3

        // The keys in the basket from left to right, so a letter's bar flies from its own place.
        private const val KeyOrder = "qwertyuiopasdfghjklzxcvbnm,."

        // Close enough to count as still, in units and units a second.
        private const val Still = 0.02f

        // The layout follows the paper's feed in steps no finer than this, in units.
        private const val FedStep = 0.05f

        // The carriage hitting its stop slower than this, in units a second, makes no bang.
        private const val BangSpeed = 80f

        // Further from home than this, a sent sheet's carriage goes home with the lever.
        private const val HomeSlack = 2f

        // How quickly the lever falls back once let go, and for how long after that it is awake.
        private const val LeverEase = 18f
        private const val LeverSettle = 0.35f

        // A strike shakes the machine this hard, and the carriage's bang harder, as shares of
        // the most it shakes, in this many half waves.
        private const val StrikeShudder = 0.6f
        private const val BangShudder = 1f
        private const val ShudderWaves = 3f

        // The bell's hammer swings onto the dome in this share of this long, and back.
        private const val HammerSeconds = 0.32f
        private const val HammerStrike = 0.2f

        // A refused key knocks the carriage this hard, in units a second.
        private const val JamKnock = 140f

        // The sheet zips out until its foot is this far over the platen.
        private const val ZipClear = 2f

        // The hint fades once the sheet is pulled out this far.
        private const val HintFade = 8f

        // The scripted fingertip takes the sheet this far in from its left and down from its top,
        // and comes down and lifts in this long.
        private const val SheetGrabX = 64f
        private const val SheetGrabY = 20f
        private const val TouchDown = 0.09f
        private const val TouchUp = 0.26f

        /** Where the carriage stands at [column]: the sheet's left edge, in units. */
        fun columnX(column: Int): Float =
            TypewriterDimens.HomeX - column * TypewriterDimens.CharWidth

        /**
         * How long the carriage takes to come home from [distance] units out on its loose return
         * spring, aiming past the stop: worked out by stepping it as [advance] does.
         */
        fun travelTime(distance: Float): Float {
            if (distance <= 0f) return 0f
            var x = -distance
            var v = 0f
            var t = 0f
            val dt = TypewriterDimens.StepSeconds
            while (x < 0f && t < 2f) {
                v +=
                    (TypewriterDimens.ReturnStiffness * (TypewriterDimens.ReturnOvershoot - x) -
                        TypewriterDimens.ReturnDamping * v) * dt
                x += v * dt
                t += dt
            }
            return t
        }

        // Where in the basket a letter's bar is, 0 at the left to 1 at the right; anything else
        // flies from its middle.
        fun basketPlace(char: Char): Float {
            val i = KeyOrder.indexOf(char.lowercaseChar())
            return if (i < 0) 0.5f else i / (KeyOrder.length - 1f)
        }

        private fun place(line: Int, column: Int): Int =
            (line * TypewriterDimens.Columns + column).coerceIn(
                0,
                TypewriterDimens.MaxLines * TypewriterDimens.Columns - 1,
            )

        /** Saves what is typed on the sheet, so it outlives the screen being made again. */
        val Saver: Saver<TypewriterState, String> =
            Saver(save = { it.field.text.toString() }, restore = { TypewriterState(it) })
    }
}

/** What has been typed and deleted since a screen reader last heard, and whether a key jammed. */
@Immutable
internal data class Speech(val typed: String, val deleted: String, val jam: Boolean) {
    val quiet: Boolean
        get() = typed.isEmpty() && deleted.isEmpty() && !jam

    // What was typed and deleted again before it was heard cancels out; the rest of what was
    // deleted was there before, and goes before what was deleted since, as it lay in the text.
    fun said(typed: String, deleted: String): Speech {
        val kept = this.typed.dropLast(deleted.length)
        val alsoDeleted = deleted.dropLast(this.typed.length)
        return copy(typed = kept + typed, deleted = alsoDeleted + this.deleted)
    }

    fun jammed(): Speech = copy(jam = true)

    companion object {
        val Quiet = Speech("", "", jam = false)
    }
}

/** A letter the tape is lifting off, and when the tape took it. */
@Immutable internal class Lifted(val glyph: Glyph, val at: Float)

/** Quick out, slow in: a cubic ease out. */
internal fun easeOut(t: Float): Float = 1f - (1f - t.coerceIn(0f, 1f)).pow(3)

/** Slow in and out: a quadratic ease in and out. */
internal fun ease(t: Float): Float {
    val p = t.coerceIn(0f, 1f)
    return if (p < 0.5f) 2f * p * p else 1f - (-2f * p + 2f).pow(2) / 2f
}

/**
 * Remembers a [TypewriterState], [text] typed to begin with. What is typed is saved, so it outlives
 * the screen being made again, as when a hardware keyboard is plugged in or the phone turns.
 */
@Composable
public fun rememberTypewriterState(text: String = ""): TypewriterState =
    rememberSaveable(saver = TypewriterState.Saver) { TypewriterState(text) }
