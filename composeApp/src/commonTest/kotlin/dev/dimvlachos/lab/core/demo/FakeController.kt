package dev.dimvlachos.lab.core.demo

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Dp
import dev.dimvlachos.lab.core.presentation.components.pullcord.PullCordDimens
import kotlin.math.abs
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

    /** Whether the lamp was lit after each pull, in order. */
    val litAfterPulls = mutableListOf<Boolean>()

    /** The magnets out on the table, and the one whose photos are fanned out, if any. */
    val magnetsOut = linkedSetOf<String>()

    var fannedOut: String? = null
        private set

    val magnetPaths = mutableListOf<List<Offset>>()

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
        // As the lamp's own rig does: far enough down, and more down than along.
        if (down >= PullCordDimens.ClickPull && down.value >= 2f * abs(across.value)) {
            lampLit = !lampLit
        }
        litAfterPulls += lampLit
        calls += now() to "pullCord($down, $duration, $across)"
    }

    override suspend fun dragMagnet(tag: String, path: List<Offset>, duration: Duration) {
        magnetPaths += path
        // As the table does: a third magnet stays in the strip.
        if (tag in magnetsOut || magnetsOut.size < 2) magnetsOut += tag
        calls += now() to "dragMagnet($tag, ${path.size} points, $duration)"
    }

    override suspend fun releaseMagnet(tag: String, duration: Duration) {
        magnetsOut -= tag
        if (fannedOut == tag) fannedOut = null
        calls += now() to "releaseMagnet($tag, $duration)"
    }

    override suspend fun tapMagnet(tag: String) {
        if (tag in magnetsOut) fannedOut = if (fannedOut == tag) null else tag
        calls += now() to "tapMagnet($tag)"
    }
}
