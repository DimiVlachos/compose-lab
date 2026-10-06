package dev.dimvlachos.lab.core.demo

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Dp
import dev.dimvlachos.lab.core.presentation.components.pullcord.PullCordDimens
import kotlin.time.Duration
import org.jetbrains.compose.resources.StringResource

internal class FakeController(private val now: () -> Long) : DemoController {
    override var selectedIndex: Int = 0
        private set

    var netScroll: Float = 0f
        private set

    val wipes = mutableListOf<List<Offset>>()

    val drips = mutableListOf<Pair<Offset, Float>>()

    val mists = mutableListOf<Duration>()

    val sends = mutableListOf<StringResource>()

    /** Messages sent and not yet taken back out of the conversation. */
    var messagesOut = 0
        private set

    /** Whether the lamp is lit: each pull of the cord far enough down switches it. */
    var lampLit = false
        private set

    val calls = mutableListOf<Pair<Long, String>>()

    override fun select(index: Int) {
        selectedIndex = index
        calls += now() to "select($index)"
    }

    override suspend fun scrollBy(px: Float) {
        netScroll += px
        calls += now() to "scrollBy($px)"
    }

    override suspend fun dragPage(drag: PageDrag) {
        calls += now() to "dragPage(${drag.moves.joinToString { "${it.toFraction}" }})"
    }

    override suspend fun wipe(path: List<Offset>, duration: Duration) {
        wipes += path
        calls += now() to "wipe(${path.size} points, $duration)"
    }

    override suspend fun drip(at: Offset, length: Float) {
        drips += at to length
        calls += now() to "drip($at, $length)"
    }

    override suspend fun mist(duration: Duration) {
        mists += duration
        calls += now() to "mist($duration)"
    }

    override suspend fun sendMessage(text: StringResource, typing: Duration) {
        sends += text
        messagesOut++
        calls += now() to "sendMessage(${text.key}, $typing)"
    }

    override suspend fun clearMessages() {
        messagesOut = 0
        calls += now() to "clearMessages()"
    }

    override suspend fun pullCord(down: Dp, duration: Duration, across: Dp) {
        if (down >= PullCordDimens.ClickPull) lampLit = !lampLit
        calls += now() to "pullCord($down, $duration, $across)"
    }
}
