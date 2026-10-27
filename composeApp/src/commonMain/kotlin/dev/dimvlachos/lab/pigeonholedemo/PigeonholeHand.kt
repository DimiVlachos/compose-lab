package dev.dimvlachos.lab.pigeonholedemo

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.geometry.Offset
import dev.dimvlachos.lab.core.presentation.components.pigeonhole.Mail
import dev.dimvlachos.lab.core.presentation.components.pigeonhole.PigeonholeState
import dev.dimvlachos.lab.core.presentation.components.pigeonhole.PigeonholeState.Companion.ease
import dev.dimvlachos.lab.core.presentation.components.pigeonhole.PigeonholeState.Companion.lerp
import dev.dimvlachos.lab.core.presentation.components.touch.ScriptedTouch

/**
 * The script's hand on the mail screen, as the prototype's demo moves its finger: it presses, drags
 * and lets go through the same calls a finger makes, so a scripted letter folds exactly as one
 * carried by hand, and it taps the bar's buttons where they are. Points are in the screen's dp;
 * [at], [shown] and [down] are for drawing the fingertip.
 */
@Stable
internal class PigeonholeHand(private val mail: PigeonholeState) {
    var at by mutableStateOf(Offset.Zero)
        private set

    var shown by mutableFloatStateOf(0f)
        private set

    var down by mutableStateOf(false)
        private set

    private val front = arrayOfNulls<Mail>(1)

    /** Where the app bar's rack icon and Back button are. */
    var folders = Offset.Zero
    var back = Offset.Zero

    /** Plays the scripted step [control], one of [PigeonholeDemos]'. */
    suspend fun play(control: String) {
        settle()
        when (control) {
            // 1. Carry the Corner Market receipt down to the Receipts cubby.
            PigeonholeDemos.Post -> {
                val (rx, ry) = mail.rowCenter("m1") ?: return
                val gx = rx + 30
                val gy = ry + 4
                if (shown <= 0f) at = Offset(mail.width * .62f, mail.height * .55f)
                move(Offset(gx, gy), 560, appear = true)
                press { mail.rowDown("m1", gx, gy, scripted = true) }
                wait(200)
                drag(Offset(gx - 4, gy - 16), 240)
                val cx = mail.cubCenterX(1)
                val cy = mail.cubCenterY(1)
                drag(Offset(lerp(gx, cx, .5f), lerp(gy, cy, .45f)), 650)
                drag(Offset(cx, cy), 650)
                wait(380)
                lift()
            }
            // 2. Open the rack; tap Receipts and its letters unfold into the folder.
            PigeonholeDemos.OpenRack -> tap(folders) { mail.toggleRack() }
            PigeonholeDemos.OpenReceipts -> {
                val c = Offset(mail.cubCenterX(1), mail.cubCenterY(1))
                move(c, 560)
                press { mail.cubDown(1, c.x, c.y, scripted = true) }
                wait(ScriptedTouch.TapHoldMs.toInt())
                lift()
            }
            // 3. Back: they fold up and go home.
            PigeonholeDemos.Back -> tap(back) { mail.back() }
            // 4. Pull the receipt back out into the inbox, where it was.
            PigeonholeDemos.PullBack -> {
                // Only the receipt the clip posted; a stack something else was put on top of is
                // left alone.
                if (mail.cubVisible(1, front) == 0 || front[0]?.id != "m1") return
                val sx = mail.slotX(1)
                val sy = mail.slotY(1, 0f, mail.upTop())
                move(Offset(sx, sy - 4), 520)
                press { mail.cubDown(1, sx, sy - 4, scripted = true) }
                wait(180)
                val ly = mail.listTop
                drag(Offset(sx - 20, lerp(sy, ly + 40, .5f)), 520)
                drag(Offset(mail.width * .45f, ly + 44), 520)
                wait(260)
                lift()
            }
        }
    }

    // The prototype's tapEl(): over to the button, down for 110 ms, and what the tap does.
    private suspend fun tap(to: Offset, then: () -> Unit) {
        move(to, 560)
        down = true
        wait(ScriptedTouch.TapHoldMs.toInt())
        down = false
        then()
    }

    private fun press(block: () -> Unit) {
        down = true
        block()
    }

    private fun lift() {
        down = false
        mail.ptrUp(at.x, at.y, scripted = true)
    }

    /** Fades the fingertip out; one held down lets go first. */
    suspend fun hide() {
        if (down) {
            down = false
            mail.releaseScripted()
        }
        val from = shown
        animate(ScriptedTouch.UpMs) { shown = from * (1 - it) }
    }

    /** Lets go and vanishes at once, when the script is stopped. */
    fun stop() {
        down = false
        mail.releaseScripted()
        shown = 0f
    }

    private suspend fun drag(to: Offset, ms: Int) {
        val from = at
        animate(ms) { t ->
            val e = ease(t)
            at = Offset(lerp(from.x, to.x, e), lerp(from.y, to.y, e))
            mail.ptrMove(at.x, at.y, scripted = true)
        }
    }

    // The prototype's fingerTo(), the fingertip fading in over its first 200 ms if [appear].
    private suspend fun move(to: Offset, ms: Int, appear: Boolean = false) {
        val from = at
        val a0 = shown
        animate(ms) { t ->
            val e = ease(t)
            at = Offset(lerp(from.x, to.x, e), lerp(from.y, to.y, e))
            if (appear) shown = (a0 + t * ms / 200f).coerceAtMost(1f)
        }
    }

    private suspend fun wait(ms: Int) = animate(ms) {}

    // Waits, for at most a few seconds, until what the last step set going has finished.
    private suspend fun settle() {
        val start = withFrameNanos { it }
        var now = start
        while (!mail.idle && now - start < SettleCapNs) now = withFrameNanos { it }
    }

    private suspend fun animate(ms: Int, frame: (Float) -> Unit) {
        val start = withFrameNanos { it }
        var p = 0f
        while (p < 1f) {
            withFrameNanos { t -> p = ((t - start) / 1e6f / ms).coerceIn(0f, 1f) }
            frame(p)
        }
    }
}

private const val SettleCapNs = 4_000_000_000L
