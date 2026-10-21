package dev.dimvlachos.lab.mailboxdemo

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.setValue

/** A message in the neighborhood inbox. */
internal data class Letter(
    val id: Int,
    val from: String,
    val subject: String,
    val body: String,
    val time: String,
    val read: Boolean,
    val expanded: Boolean = false,
)

private class Draft(val from: String, val subject: String, val body: String)

// The mail that arrives, in turn, round and round: invented neighbors and businesses.
private val pool =
    listOf(
        Draft(
            "Dana Whitfield",
            "Porch swap this Saturday?",
            "I have two tomato cages and a box of mason jars if anyone wants them. Bring yours by " +
                "9:00 AM!",
        ),
        Draft(
            "Hollow Oak Hardware",
            "Your order is ready for pickup",
            "Order #4471 (2 gal. barn red, two brushes) is at the front counter until 6:00 PM.",
        ),
        Draft(
            "Marcus Lee",
            "Found Biscuit's frisbee",
            "It landed in our hydrangeas again. I'll leave it on your front steps.",
        ),
        Draft(
            "Elm Street Garden Club",
            "Seed swap moved to the library",
            "Same time, 10:00 AM Sunday, in the Maple Grove Library community room.",
        ),
        Draft(
            "Grandma June",
            "The pie recipe you asked for",
            "Two cups tart cherries, and don't skimp on the butter. Call me Sunday.",
        ),
        Draft(
            "Rosa Jimenez",
            "Carpool to the game?",
            "We have two seats free for Friday night. Leaving at 6:15 PM from the corner.",
        ),
    )

// What the inbox holds before any new mail: read, from earlier in the week.
private val earlier =
    listOf(
        Letter(
            -1,
            "Maple Grove Library",
            "Your hold is ready",
            "Pick it up at the front desk within 7 days.",
            "Thu",
            read = true,
        ),
        Letter(
            -2,
            "Ted Okafor",
            "Thanks for the ladder",
            "Gutters are clean. Ladder is back in your side yard.",
            "Wed",
            read = true,
        ),
        Letter(
            -3,
            "Cedar Park Rec",
            "Fall soccer schedule",
            "Games start October 4. The schedule is posted at the field house.",
            "Mon",
            read = true,
        ),
    )

/**
 * The inbox of a neighborhood app: last week's read messages, and new ones that arrive, newest
 * first, each stamped with a morning clock that starts at 8:41 AM and moves on a few minutes with
 * each.
 */
@Stable
internal class NeighborMail {
    val letters = mutableStateListOf<Letter>().apply { addAll(earlier) }

    var unread by mutableIntStateOf(0)
        private set

    private var delivered = 0
    private var clockMinutes = 0

    /** The next message from the pool, unread, at the top. */
    fun arrive(): Letter {
        val draft = pool[delivered % pool.size]
        delivered++
        clockMinutes += 4 + (delivered % 3) * 3
        val letter = Letter(delivered, draft.from, draft.subject, draft.body, clock(), read = false)
        letters.add(0, letter)
        count()
        return letter
    }

    /** Opens or closes message [id], marking it read. */
    fun toggle(id: Int) {
        val i = letters.indexOfFirst { it.id == id }
        if (i < 0) return
        val letter = letters[i]
        letters[i] = letter.copy(read = true, expanded = !letter.expanded)
        count()
    }

    fun readAll() {
        for (i in letters.indices) if (!letters[i].read) letters[i] = letters[i].copy(read = true)
        count()
    }

    /** Closes every opened message, as leaving the inbox does. */
    fun collapseAll() {
        for (i in letters.indices) {
            if (letters[i].expanded) letters[i] = letters[i].copy(expanded = false)
        }
    }

    fun reset() {
        letters.clear()
        letters.addAll(earlier)
        delivered = 0
        clockMinutes = 0
        count()
    }

    private fun count() {
        unread = letters.count { !it.read }
    }

    private fun clock(): String {
        val minutes = 8 * 60 + 41 + clockMinutes
        val hour = minutes / 60
        val minute = minutes % 60
        val shown = if (hour > 12) hour - 12 else hour
        val suffix = if (hour >= 12) "PM" else "AM"
        return "$shown:${minute.toString().padStart(2, '0')} $suffix"
    }
}
