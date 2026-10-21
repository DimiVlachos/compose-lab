package dev.dimvlachos.lab.core.presentation.components.mailbox

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.min

/** A sound the mailbox makes, with how hard it was struck. */
internal enum class MailboxCue {
    /** Tin struck: the door clapping shut. */
    Tunk,
    /** Tin struck low: the flag dropping onto its stop, the door onto its open stop. */
    LowTunk,
    /** The flag's spring arm twanging as it swings up. */
    Twang,
    /** The door's hinge. */
    Creak,
    /** An envelope slipping in. */
    Slip,
    /** The count on the flag ticking. */
    Tick,
}

/**
 * The mailbox's moving parts, in simulated time stepped at 240 Hz as the prototype steps them: the
 * flag on its pivot, the door on its bottom hinge, the letters leaning out of the opening, the
 * number painted on the flag, and envelopes in flight. [advance] runs it; the rest is what happens
 * to it: unread mail ([setUnread]), a delivery, the inbox opened or closed, the door pulled.
 */
@Stable
internal class MailboxState(reducedMotion: Boolean = false) {
    var reducedMotion: Boolean = reducedMotion

    /** Called for each sound, with a volume from 0 to 1. */
    var cue: (MailboxCue, Float) -> Unit = { _, _ -> }

    /** Simulated time, in milliseconds. */
    var now: Double = 0.0
        private set

    /** The flag's angle: 0 lying along the side, π/2 standing up. */
    var flagAngle = 0f
        private set

    /** The door's angle on its hinge: 0 shut, 1.9 hanging open. */
    var doorAngle = 0f
        private set

    /** How far the letters lean out of the opening, 0 to 1. */
    var peek = 0f
        private set

    /** How much of the number on the flag shows, 0 to 1. */
    var numberAlpha = 0f
        private set

    /** The number painted on the flag: the last unread count above none. */
    var shown = 0
        private set

    var unread = 0
        private set

    /**
     * The inbox is open, or opening: the door hangs down and the letters lean out. It is state, so
     * Back can close an inbox whose door is still dropping.
     */
    var isOpen by mutableStateOf(false)
        private set

    /** A finger has the door. */
    var dragging = false
        private set

    /** When each envelope in flight set off, in [now]'s milliseconds. */
    val flights: List<Double>
        get() = flightStarts

    // Bumped on every step and every change, so a drawing that reads it is drawn again.
    var frame by mutableIntStateOf(0)
        private set

    // Bumped when something sets the parts moving, to wake a frame loop that has gone to sleep.
    var wake by mutableIntStateOf(0)
        private set

    private var flagV = 0f
    private var flagTarget = 0f
    private var flagSoft = false
    private var doorV = 0f
    private var doorTarget = 0f
    private var doorStiff = false
    private var peekTarget = 0f
    private val flightStarts = mutableListOf<Double>()
    private val queue = mutableListOf<Pair<Double, () -> Unit>>()
    private var carry = 0f

    // Bumped by every open and close, so what one of them left queued is dropped once the other
    // has come: a quick tap and tap again must not shut the door on an open inbox, or slide in an
    // inbox already closed.
    private var turn = 0

    /** Nothing is moving and nothing is waiting to happen: a frame loop can sleep. */
    val isSettled: Boolean
        get() =
            queue.isEmpty() &&
                flightStarts.isEmpty() &&
                abs(flagV) < Rest &&
                abs(flagTarget - flagAngle) < Rest &&
                abs(doorV) < Rest &&
                abs(doorTarget - doorAngle) < Rest &&
                abs(peekTarget - peek) < Rest &&
                abs(wantNumber() - numberAlpha) < Rest

    /** Runs the parts on by [seconds] of real time, at most 0.15 s at once. */
    fun advance(seconds: Float) {
        carry += seconds.coerceIn(0f, MaxFrame)
        while (carry >= Step) {
            step(Step)
            now += Step * 1000.0
            carry -= Step
        }
        while (true) {
            val next = queue.minByOrNull { it.first } ?: break
            if (next.first > now) break
            queue.remove(next)
            next.second()
        }
        flightStarts.removeAll { now - it >= FlightMs }
        if (isSettled) settle()
        frame++
    }

    /**
     * The unread count is now [count]. More mail raises the flag with a twang, or, already up,
     * jiggles it; reading some jiggles it and ticks; reading the last lowers it, slowly, a moment
     * later.
     */
    fun setUnread(count: Int) {
        val target = count.coerceAtLeast(0)
        val previous = unread
        if (target == previous) return
        unread = target
        if (target > 0) shown = target
        when {
            target > previous -> raiseFlag()
            target > 0 -> {
                if (!reducedMotion) flagV -= 2.5f
                cue(MailboxCue.Tick, 1f)
            }
            else -> {
                if (previous > 1) cue(MailboxCue.Tick, 1f)
                at(if (reducedMotion) 0.0 else LowerDelayMs) {
                    if (unread == 0) {
                        flagTarget = 0f
                        flagSoft = true
                    }
                }
            }
        }
        touched()
    }

    /**
     * Delivers an envelope: the door cracks open, the envelope slips in and the door claps shut;
     * then [onLanded], which is where the unread count goes up.
     */
    fun deliver(onLanded: () -> Unit) {
        if (reducedMotion) {
            onLanded()
            touched()
            return
        }
        // The door is cracked for the envelope unless the inbox has it open or a finger has it.
        if (!isOpen && !dragging) {
            doorTarget = CrackedDoor
            doorStiff = false
            cue(MailboxCue.Creak, 1f)
        }
        at(160.0) {
            flightStarts += now
            cue(MailboxCue.Slip, 1f)
        }
        at(760.0) { if (!isOpen && !dragging) doorTarget = 0f }
        at(900.0) { onLanded() }
        touched()
    }

    /**
     * Opens the inbox: the door drops open and the letters lean out, and [onOpened] runs once the
     * door is down, which is where the inbox slides in.
     */
    fun open(onOpened: () -> Unit) {
        if (isOpen) return
        isOpen = true
        val mine = ++turn
        doorTarget = OpenDoor
        doorStiff = false
        if (!reducedMotion) doorV += 5f
        cue(MailboxCue.Creak, 1f)
        at(if (reducedMotion) 0.0 else 130.0) { if (turn == mine) peekTarget = 1f }
        at(if (reducedMotion) 0.0 else 280.0) { if (turn == mine) onOpened() }
        touched()
    }

    /** Closes the inbox: the letters slide back in and the door claps shut. */
    fun close() {
        if (!isOpen) return
        isOpen = false
        val mine = ++turn
        at(if (reducedMotion) 0.0 else 140.0) { if (turn == mine) peekTarget = 0f }
        at(if (reducedMotion) 0.0 else 260.0) { if (turn == mine) doorTarget = 0f }
        touched()
    }

    /** A finger pulls the door down by [dy] dp: it follows, all the way open at [DragOpenDp]. */
    fun dragDoor(dy: Float) {
        if (isOpen) return
        dragging = true
        doorStiff = true
        doorTarget = (dy / DragOpenDp).coerceIn(0f, 1f) * OpenDoor
        touched()
    }

    /**
     * The finger lets go of the door. Past halfway it is true, and the caller opens the inbox;
     * short of that the door claps shut.
     */
    fun releaseDoor(): Boolean {
        val was = dragging
        dragging = false
        doorStiff = false
        if (!was || isOpen) return false
        if (doorAngle > OpenEnough) return true
        doorTarget = 0f
        touched()
        return false
    }

    /** The gesture on the door was taken away: it goes back to where the inbox has it. */
    fun cancelDrag() {
        dragging = false
        doorStiff = false
        doorTarget = if (isOpen) OpenDoor else 0f
        touched()
    }

    /** Everything back as it began: shut, flag down, nothing unread, nothing pending. */
    fun reset() {
        turn++
        queue.clear()
        flightStarts.clear()
        flagAngle = 0f
        flagV = 0f
        flagTarget = 0f
        flagSoft = false
        doorAngle = 0f
        doorV = 0f
        doorTarget = 0f
        doorStiff = false
        peek = 0f
        peekTarget = 0f
        numberAlpha = 0f
        unread = 0
        shown = 0
        isOpen = false
        dragging = false
        touched()
    }

    private fun raiseFlag() {
        if (flagTarget < 1f) {
            flagTarget = UpAngle
            flagSoft = false
            if (!reducedMotion) flagV += 3f
            cue(MailboxCue.Twang, 1f)
        } else {
            if (!reducedMotion) flagV -= 4.5f
            cue(MailboxCue.Tick, 1f)
        }
    }

    private fun at(delayMs: Double, action: () -> Unit) {
        queue += (now + delayMs) to action
    }

    private fun touched() {
        wake++
        frame++
    }

    private fun wantNumber() = if (unread > 0 && flagAngle > NumberFrom) 1f else 0f

    // Lands everything exactly on its target once it is close enough, so a settled state is
    // still.
    private fun settle() {
        flagAngle = flagTarget
        flagV = 0f
        doorAngle = doorTarget
        doorV = 0f
        peek = peekTarget
        numberAlpha = wantNumber()
    }

    private fun step(dt: Float) {
        // The flag: a bouncy spring up, a slow, fully damped one down.
        val fk = if (flagSoft) 26f else 210f
        val fc = if (flagSoft) 10.5f else 7.5f
        flagV += (fk * (flagTarget - flagAngle) - fc * flagV) * dt
        flagAngle += flagV * dt
        val top = UpAngle + 0.16f
        if (flagAngle > top) {
            flagAngle = top
            if (flagV > 0f) flagV *= -0.35f
        }
        if (flagAngle < 0f) {
            if (flagV < -0.35f) cue(MailboxCue.LowTunk, (-flagV / 3f).coerceIn(0.2f, 0.45f))
            flagAngle = 0f
            flagV = if (flagV < 0f) -flagV * 0.25f else flagV
        }
        // The door on its bottom hinge, with a stop at each end.
        val dk = if (doorStiff) 900f else 240f
        val dc = if (doorStiff) 55f else 15f
        doorV += (dk * (doorTarget - doorAngle) - dc * doorV) * dt
        doorAngle += doorV * dt
        if (doorAngle > DoorStop) {
            if (doorV > 2.5f) cue(MailboxCue.LowTunk, 0.3f)
            doorAngle = DoorStop
            if (doorV > 0f) doorV *= -0.35f
        }
        if (doorAngle < 0f) {
            if (doorV < -1.6f) cue(MailboxCue.Tunk, (-doorV / 9f).coerceIn(0.3f, 1f))
            doorAngle = 0f
            if (doorV < 0f) doorV *= -0.18f
        }
        peek += (peekTarget - peek) * min(1f, dt * 7f)
        val want = wantNumber()
        numberAlpha += (want - numberAlpha) * min(1f, dt * if (want > 0f) 6f else 10f)
        if (reducedMotion) {
            flagAngle = flagTarget
            flagV = 0f
            doorAngle = doorTarget
            doorV = 0f
            peek = peekTarget
            numberAlpha = want
        }
    }

    internal companion object {
        const val Step = 1f / 240f
        const val MaxFrame = 0.15f
        const val UpAngle = (PI / 2).toFloat()
        const val OpenDoor = 1.9f
        const val CrackedDoor = 0.75f
        const val DoorStop = 1.95f
        const val OpenEnough = 0.95f
        const val DragOpenDp = 38f
        const val FlightMs = 560.0
        const val LowerDelayMs = 700.0
        const val NumberFrom = 0.7f
        private const val Rest = 0.002f
    }
}
