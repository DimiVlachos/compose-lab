package dev.dimvlachos.lab.core.presentation.components.fishing

/**
 * How a refresh came out, as the caller tells the fishing refresh once its loading is done. Each
 * plays out on the water: a catch bites and is reeled in, nothing new reels in an empty hook, and a
 * failure snaps the line.
 */
public sealed interface FishingOutcome {
    /**
     * New items came in, [count] of them, now at the top of the list: the bobber dips, the line is
     * reeled in and the [count] newest items rise out of the water. A catch of nothing is
     * [NothingNew], so [count] must be at least 1.
     */
    public data class Caught(val count: Int) : FishingOutcome {
        init {
            require(count > 0) { "A catch holds at least one new item, not $count" }
        }
    }

    /** The refresh worked and found nothing new: the line is reeled in to an empty hook. */
    public data object NothingNew : FishingOutcome

    /**
     * The refresh failed, for [cause]: the line snaps, its slack falls and the bobber drifts off.
     * The cause is the caller's to log or show; the fishing refresh only says that it failed.
     * Pulling again retries.
     */
    public data class Failed(val cause: Throwable) : FishingOutcome
}

/**
 * Where a refresh stands, hoisted by the caller (from a ViewModel, say), which owns the loading:
 * the fishing refresh knows nothing about it.
 *
 * A refresh goes [Idle] → [Refreshing] → [Landed] and back to [Idle] whenever the caller likes. A
 * pull past the threshold asks for a refresh through `onRefresh`; the caller sets [Refreshing] and
 * the line is cast. Once its loading is done it sets [Landed] with the outcome, and that plays out.
 * Each change from [Refreshing] to [Landed] plays once, so the same outcome twice in a row plays
 * twice, as long as a [Refreshing] came between them.
 */
public sealed interface FishingStatus {
    /** No refresh is under way: the water is out of sight. */
    public data object Idle : FishingStatus

    /** A refresh is loading: the bobber floats on the water, however long it takes. */
    public data object Refreshing : FishingStatus

    /** The refresh is done, with [outcome]: it plays out, and the water closes behind it. */
    public data class Landed(val outcome: FishingOutcome) : FishingStatus
}
