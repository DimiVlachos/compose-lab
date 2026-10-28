package dev.dimvlachos.lab.coinsorterdemo

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.geometry.Offset
import dev.dimvlachos.lab.core.presentation.components.coinsorter.CoinSort
import dev.dimvlachos.lab.core.presentation.components.coinsorter.CoinSorterState
import dev.dimvlachos.lab.core.presentation.components.touch.ScriptedTouch
import kotlinx.coroutines.flow.first

/**
 * The script's hand on the screen, as the prototype's demo moves its finger: it taps the chip
 * through the same press a finger makes, then taps an option, and fades out while the sort runs so
 * it never covers the prices. Points are in the screen's dp; [at], [shown] and [down] are for
 * drawing the fingertip.
 */
@Stable
internal class CoinSorterHand(private val sorter: CoinSorterState) {
    var at by mutableStateOf(Offset.Zero)
        private set

    var shown by mutableFloatStateOf(0f)
        private set

    var down by mutableStateOf(false)
        private set

    /** Plays the scripted step [control], one of [CoinSorterDemos]'. */
    suspend fun play(control: String) {
        val sort =
            when (control) {
                CoinSorterDemos.SortLow -> CoinSort.Low
                CoinSorterDemos.SortHigh -> CoinSort.High
                CoinSorterDemos.SortTop -> CoinSort.Top
                else -> CoinSort.Best
            }
        val slide = control == CoinSorterDemos.SortHigh || control == CoinSorterDemos.SortBest
        // As the prototype's demo awaits idle(): a sort still landing finishes first.
        idle()
        // From the lower middle of the screen, fading in as it moves to the chip.
        at = Offset(sorter.width * .62f, sorter.height * .7f)
        val chip =
            Offset(sorter.chipX + sorter.chipW * .45f, sorter.chipY + CoinSorterState.ChipH / 2)
        move(chip, 560, fadeIn = true)
        down = true
        sorter.pressDown(at.x, at.y)
        val i = sort.ordinal
        val optX =
            sorter.menuX +
                CoinSorterState.MenuPad +
                (CoinSorterState.MenuW - 2 * CoinSorterState.MenuPad) * .4f
        if (slide) {
            // Press the chip and slide onto the option: the menu opens once the finger moves, and
            // letting go on the option picks it.
            wait(400)
            val from = at
            animate(820) { t ->
                val e = CoinSorterState.ease(t)
                // The option's place is read each frame: the menu is laid out once it opens.
                val to = Offset(optX, sorter.optY(i) + CoinSorterState.OptH / 2)
                at = Offset(from.x + (to.x - from.x) * e, from.y + (to.y - from.y) * e)
                sorter.pressMove(at.x, at.y)
            }
            wait(350)
            down = false
            sorter.pressUp(at.x, at.y)
            hide()
            return
        }
        wait(ScriptedTouch.TapHoldMs.toInt())
        down = false
        sorter.pressUp(at.x, at.y)
        wait(650)
        val opt = Offset(optX, sorter.optY(i) + CoinSorterState.OptH / 2)
        move(opt, 460)
        down = true
        sorter.hover(i)
        wait(ScriptedTouch.TapHoldMs.toInt())
        down = false
        sorter.hover(-1)
        sorter.pick(i)
        // Off the screen while it sorts: anywhere on the list it would cover the prices.
        hide()
    }

    /** Waits for a sort that is running to land. */
    suspend fun idle() {
        snapshotFlow { sorter.running }.first { !it }
    }

    /** Fades the fingertip out. */
    suspend fun hide() {
        down = false
        val from = shown
        animate(200) { shown = from * (1 - it) }
    }

    /** Vanishes at once, when the script is stopped. */
    fun stop() {
        down = false
        shown = 0f
    }

    private suspend fun move(to: Offset, ms: Int, fadeIn: Boolean = false) {
        val from = at
        val a0 = shown
        animate(ms) { t ->
            val p = CoinSorterState.ease(t)
            at = Offset(from.x + (to.x - from.x) * p, from.y + (to.y - from.y) * p)
            // The prototype's .2 s opacity transition.
            if (fadeIn) shown = a0 + (1 - a0) * (t * ms / 200f).coerceAtMost(1f)
        }
    }

    private suspend fun wait(ms: Int) = animate(ms) {}

    private suspend fun animate(ms: Int, frame: (Float) -> Unit) {
        val start = withFrameNanos { it }
        var p = 0f
        while (p < 1f) {
            withFrameNanos { t -> p = ((t - start) / 1e6f / ms).coerceIn(0f, 1f) }
            frame(p)
        }
    }
}
