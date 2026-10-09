package dev.dimvlachos.lab.fishingdemo

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import dev.dimvlachos.lab.core.presentation.components.fishing.FishingOutcome
import dev.dimvlachos.lab.core.presentation.components.fishing.FishingStatus
import dev.dimvlachos.lab.resources.Res
import dev.dimvlachos.lab.resources.photo_corfu
import dev.dimvlachos.lab.resources.photo_corfu_caption
import dev.dimvlachos.lab.resources.photo_corfu_title
import dev.dimvlachos.lab.resources.photo_crete
import dev.dimvlachos.lab.resources.photo_crete_caption
import dev.dimvlachos.lab.resources.photo_crete_title
import dev.dimvlachos.lab.resources.photo_folegandros
import dev.dimvlachos.lab.resources.photo_folegandros_caption
import dev.dimvlachos.lab.resources.photo_folegandros_title
import dev.dimvlachos.lab.resources.photo_hydra
import dev.dimvlachos.lab.resources.photo_hydra_caption
import dev.dimvlachos.lab.resources.photo_hydra_title
import dev.dimvlachos.lab.resources.photo_kefalonia
import dev.dimvlachos.lab.resources.photo_kefalonia_caption
import dev.dimvlachos.lab.resources.photo_kefalonia_title
import dev.dimvlachos.lab.resources.photo_milos
import dev.dimvlachos.lab.resources.photo_milos_caption
import dev.dimvlachos.lab.resources.photo_milos_title
import dev.dimvlachos.lab.resources.photo_mykonos
import dev.dimvlachos.lab.resources.photo_mykonos_caption
import dev.dimvlachos.lab.resources.photo_mykonos_title
import dev.dimvlachos.lab.resources.photo_naxos
import dev.dimvlachos.lab.resources.photo_naxos_caption
import dev.dimvlachos.lab.resources.photo_naxos_title
import dev.dimvlachos.lab.resources.photo_paxos
import dev.dimvlachos.lab.resources.photo_paxos_caption
import dev.dimvlachos.lab.resources.photo_paxos_title
import dev.dimvlachos.lab.resources.photo_rhodes
import dev.dimvlachos.lab.resources.photo_rhodes_caption
import dev.dimvlachos.lab.resources.photo_rhodes_title
import dev.dimvlachos.lab.resources.photo_santorini
import dev.dimvlachos.lab.resources.photo_santorini_caption
import dev.dimvlachos.lab.resources.photo_santorini_title
import dev.dimvlachos.lab.resources.photo_zakynthos
import dev.dimvlachos.lab.resources.photo_zakynthos_caption
import dev.dimvlachos.lab.resources.photo_zakynthos_title
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource

/** An island in the feed: its photo, its name and a line about it. [id] keys it in the list. */
internal class FeedIsland(
    val id: String,
    val photo: DrawableResource,
    val title: StringResource,
    val caption: StringResource,
)

/** How one refresh of the feed comes out, [after] how long. */
internal sealed interface FeedTurn {
    val after: Duration

    /** New [islands] come in, newest first. */
    class Catch(val islands: List<FeedIsland>, override val after: Duration) : FeedTurn

    /** Nothing new. */
    class Empty(override val after: Duration) : FeedTurn

    /** The refresh fails. */
    class Fail(override val after: Duration) : FeedTurn
}

/** The feed as it starts, and how its refreshes come out, in order. */
internal object IslandFeedTurns {
    private val corfu =
        FeedIsland(
            "corfu",
            Res.drawable.photo_corfu,
            Res.string.photo_corfu_title,
            Res.string.photo_corfu_caption,
        )
    private val paxos =
        FeedIsland(
            "paxos",
            Res.drawable.photo_paxos,
            Res.string.photo_paxos_title,
            Res.string.photo_paxos_caption,
        )
    private val santorini =
        FeedIsland(
            "santorini",
            Res.drawable.photo_santorini,
            Res.string.photo_santorini_title,
            Res.string.photo_santorini_caption,
        )
    private val milos =
        FeedIsland(
            "milos",
            Res.drawable.photo_milos,
            Res.string.photo_milos_title,
            Res.string.photo_milos_caption,
        )
    private val naxos =
        FeedIsland(
            "naxos",
            Res.drawable.photo_naxos,
            Res.string.photo_naxos_title,
            Res.string.photo_naxos_caption,
        )
    private val hydra =
        FeedIsland(
            "hydra",
            Res.drawable.photo_hydra,
            Res.string.photo_hydra_title,
            Res.string.photo_hydra_caption,
        )
    private val rhodes =
        FeedIsland(
            "rhodes",
            Res.drawable.photo_rhodes,
            Res.string.photo_rhodes_title,
            Res.string.photo_rhodes_caption,
        )
    private val zakynthos =
        FeedIsland(
            "zakynthos",
            Res.drawable.photo_zakynthos,
            Res.string.photo_zakynthos_title,
            Res.string.photo_zakynthos_caption,
        )
    private val kefalonia =
        FeedIsland(
            "kefalonia",
            Res.drawable.photo_kefalonia,
            Res.string.photo_kefalonia_title,
            Res.string.photo_kefalonia_caption,
        )
    private val mykonos =
        FeedIsland(
            "mykonos",
            Res.drawable.photo_mykonos,
            Res.string.photo_mykonos_title,
            Res.string.photo_mykonos_caption,
        )
    private val crete =
        FeedIsland(
            "crete",
            Res.drawable.photo_crete,
            Res.string.photo_crete_title,
            Res.string.photo_crete_caption,
        )
    private val folegandros =
        FeedIsland(
            "folegandros",
            Res.drawable.photo_folegandros,
            Res.string.photo_folegandros_title,
            Res.string.photo_folegandros_caption,
        )

    /** Nine islands to begin with; the other three come in as catches. */
    val start: List<FeedIsland> =
        listOf(corfu, paxos, santorini, milos, naxos, hydra, rhodes, zakynthos, kefalonia)

    /**
     * The clip's refreshes: two islands caught, then nothing new, answered faster than the bobber's
     * shortest float so the wait shows anyway, then a failure, then one more island.
     */
    val scripted: List<FeedTurn> =
        listOf(
            FeedTurn.Catch(listOf(mykonos, crete), after = 1_400.milliseconds),
            FeedTurn.Empty(after = 300.milliseconds),
            FeedTurn.Fail(after = 2_200.milliseconds),
            FeedTurn.Catch(listOf(folegandros), after = 1_000.milliseconds),
        )

    /** Once every island is in, a refresh finds nothing new. */
    val afterScript: FeedTurn = FeedTurn.Empty(after = 800.milliseconds)

    /** The feed once [landed] of its refreshes have come in: each catch on top of the last. */
    fun itemsAfter(landed: Int): List<FeedIsland> =
        scripted.take(landed).filterIsInstance<FeedTurn.Catch>().fold(start) { items, turn ->
            turn.islands + items
        }
}

/**
 * The island feed and where its refresh stands, as a ViewModel would hold them: [refresh] fetches
 * from a scripted source whose answers come out as [IslandFeedTurns.scripted] says, in order. A
 * catch goes on top of [items] as the [status] lands, and [onCatch] hears it, to keep the list at
 * its top.
 */
@Stable
internal class IslandFeed(private val scope: CoroutineScope, landed: Int = 0) {
    var items: List<FeedIsland> by mutableStateOf(IslandFeedTurns.itemsAfter(landed))
        private set

    var status: FishingStatus by mutableStateOf(FishingStatus.Idle)
        private set

    var onCatch: () -> Unit = {}

    // How many refreshes have come in, and how many have been asked for.
    private var landed = landed
    private var asked = landed
    private var job: Job? = null

    /** Asks the source for anything new, unless a refresh is already under way. */
    fun refresh() {
        if (status == FishingStatus.Refreshing) return
        val turn = IslandFeedTurns.scripted.getOrElse(asked) { IslandFeedTurns.afterScript }
        asked++
        status = FishingStatus.Refreshing
        job = scope.launch {
            delay(turn.after)
            landed = asked
            status =
                when (turn) {
                    is FeedTurn.Catch -> {
                        items = turn.islands + items
                        onCatch()
                        FishingStatus.Landed(FishingOutcome.Caught(turn.islands.size))
                    }
                    is FeedTurn.Empty -> FishingStatus.Landed(FishingOutcome.NothingNew)
                    is FeedTurn.Fail ->
                        FishingStatus.Landed(
                            FishingOutcome.Failed(IllegalStateException(UnreachableMessage))
                        )
                }
        }
    }

    /** Puts the feed back as it began, with no refresh under way. */
    fun reset() {
        job?.cancel()
        items = IslandFeedTurns.start
        status = FishingStatus.Idle
        landed = 0
        asked = 0
    }

    internal companion object {
        // Only how many refreshes have come in outlives the screen: a refresh under way when it
        // goes is lost with it, and the feed comes back idle.
        fun saver(scope: CoroutineScope): Saver<IslandFeed, Int> =
            Saver(save = { it.landed }, restore = { IslandFeed(scope, it) })

        // The scripted source's failure, for logs: the screen only says that it failed.
        private const val UnreachableMessage = "The island feed is unreachable"
    }
}

/** An [IslandFeed]; how many of its refreshes have come in survives the screen being made again. */
@Composable
internal fun rememberIslandFeed(): IslandFeed {
    val scope = rememberCoroutineScope()
    return rememberSaveable(saver = IslandFeed.saver(scope)) { IslandFeed(scope) }
}
