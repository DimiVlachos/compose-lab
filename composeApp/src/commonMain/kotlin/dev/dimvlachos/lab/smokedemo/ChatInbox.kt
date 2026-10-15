package dev.dimvlachos.lab.smokedemo

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import dev.dimvlachos.lab.resources.Res
import dev.dimvlachos.lab.resources.smoke_chat_cab_name
import dev.dimvlachos.lab.resources.smoke_chat_cab_preview
import dev.dimvlachos.lab.resources.smoke_chat_emma_name
import dev.dimvlachos.lab.resources.smoke_chat_emma_preview
import dev.dimvlachos.lab.resources.smoke_chat_ferry_name
import dev.dimvlachos.lab.resources.smoke_chat_ferry_preview
import dev.dimvlachos.lab.resources.smoke_chat_jake_name
import dev.dimvlachos.lab.resources.smoke_chat_jake_preview
import dev.dimvlachos.lab.resources.smoke_chat_kite_name
import dev.dimvlachos.lab.resources.smoke_chat_kite_preview
import dev.dimvlachos.lab.resources.smoke_chat_maddie_name
import dev.dimvlachos.lab.resources.smoke_chat_maddie_preview
import dev.dimvlachos.lab.resources.smoke_chat_patterson_name
import dev.dimvlachos.lab.resources.smoke_chat_patterson_preview
import dev.dimvlachos.lab.resources.smoke_chat_pottery_name
import dev.dimvlachos.lab.resources.smoke_chat_pottery_preview
import dev.dimvlachos.lab.resources.smoke_chat_surf_name
import dev.dimvlachos.lab.resources.smoke_chat_surf_preview
import dev.dimvlachos.lab.resources.smoke_chat_time_mon
import dev.dimvlachos.lab.resources.smoke_chat_time_sun
import org.jetbrains.compose.resources.StringResource

/** Which colour a chat's avatar is. */
internal enum class Avatar {
    Green,
    Blue,
    Amber,
    Red,
    Teal,
    Violet,
    Pink,
}

/** When a chat's last message came in: today, or earlier in the week. */
internal enum class ChatDay {
    Today,
    Earlier,
}

/**
 * A chat in the list: who it is with ([name], and [initials] for the avatar), the last message
 * ([preview]), when it came in ([time] today, or the [day] of the week for an earlier one), and
 * whether it is [unread].
 */
internal class Chat(
    val id: String,
    val name: StringResource,
    val initials: String,
    val preview: StringResource,
    val time: String = "",
    val day: StringResource? = null,
    val avatar: Avatar,
    val unread: Boolean = false,
) {
    /** Which day's group it is in: earlier in the week if it has a [day], else today. */
    val section: ChatDay
        get() = if (day == null) ChatDay.Today else ChatDay.Earlier
}

/** Where a chat is: in the inbox, archived, or deleted. */
internal enum class ChatPlace {
    Inbox,
    Archived,
    Deleted,
}

/** What the last action did, for the snackbar and its Undo. */
internal enum class ChatChange {
    Deleted,
    Archived,
    Restored,
    Cleared,
}

/** The last action: [change] to the chats [ids], which were in [from] before. */
internal class InboxAction(
    val change: ChatChange,
    val ids: List<String>,
    val from: ChatPlace,
    val serial: Int,
)

/** The chats, an inbox: five today and two earlier, and two already archived. */
internal object InboxChats {
    val all: List<Chat> =
        listOf(
            Chat(
                "emma",
                Res.string.smoke_chat_emma_name,
                "EC",
                Res.string.smoke_chat_emma_preview,
                time = "6:42 PM",
                avatar = Avatar.Green,
                unread = true,
            ),
            Chat(
                "ferry",
                Res.string.smoke_chat_ferry_name,
                "HF",
                Res.string.smoke_chat_ferry_preview,
                time = "5:05 PM",
                avatar = Avatar.Blue,
            ),
            Chat(
                "jake",
                Res.string.smoke_chat_jake_name,
                "J",
                Res.string.smoke_chat_jake_preview,
                time = "4:20 PM",
                avatar = Avatar.Amber,
            ),
            Chat(
                "patterson",
                Res.string.smoke_chat_patterson_name,
                "MP",
                Res.string.smoke_chat_patterson_preview,
                time = "12:10 PM",
                avatar = Avatar.Red,
            ),
            Chat(
                "cab",
                Res.string.smoke_chat_cab_name,
                "BC",
                Res.string.smoke_chat_cab_preview,
                time = "9:48 AM",
                avatar = Avatar.Teal,
                unread = true,
            ),
            Chat(
                "surf",
                Res.string.smoke_chat_surf_name,
                "DS",
                Res.string.smoke_chat_surf_preview,
                day = Res.string.smoke_chat_time_mon,
                avatar = Avatar.Violet,
            ),
            Chat(
                "maddie",
                Res.string.smoke_chat_maddie_name,
                "M",
                Res.string.smoke_chat_maddie_preview,
                day = Res.string.smoke_chat_time_mon,
                avatar = Avatar.Pink,
            ),
            Chat(
                "pottery",
                Res.string.smoke_chat_pottery_name,
                "RM",
                Res.string.smoke_chat_pottery_preview,
                day = Res.string.smoke_chat_time_sun,
                avatar = Avatar.Amber,
            ),
            Chat(
                "kite",
                Res.string.smoke_chat_kite_name,
                "CW",
                Res.string.smoke_chat_kite_preview,
                day = Res.string.smoke_chat_time_sun,
                avatar = Avatar.Blue,
            ),
        )

    private val startArchived = setOf("pottery", "kite")

    /** Where each chat starts: all in the inbox but the two archived. */
    val start: Map<String, ChatPlace> = all.associate {
        it.id to if (it.id in startArchived) ChatPlace.Archived else ChatPlace.Inbox
    }
}

/**
 * The inbox as a ViewModel would hold it: where each chat is, and the [last] action, which Undo
 * puts back. Deleting, archiving and restoring take effect as each chat's smoke has gone; a clear
 * of the read chats is one action, made once the last of them has burnt.
 */
@Stable
internal class ChatInbox {
    private val places = mutableStateMapOf<String, ChatPlace>().apply { putAll(InboxChats.start) }

    /** The last action, until it is undone or the next one replaces it. */
    var last: InboxAction? by mutableStateOf(null)
        private set

    private var serial by mutableIntStateOf(0)

    // The read chats a clear is burning, and those of them burnt so far.
    private var clearing: List<String> = emptyList()
    private val cleared = mutableListOf<String>()

    fun placeOf(id: String): ChatPlace = places[id] ?: ChatPlace.Deleted

    /** The chats in [place], in the list's order. */
    fun chatsIn(place: ChatPlace): List<Chat> = InboxChats.all.filter { placeOf(it.id) == place }

    /** The read chats in the inbox: what Clear read burns. */
    fun readInInbox(): List<Chat> = chatsIn(ChatPlace.Inbox).filter { !it.unread }

    /** Whether a Clear read is burning its wave: another can't start until it is done. */
    val isClearing: Boolean
        get() = clearingNow

    private var clearingNow by mutableStateOf(false)

    /**
     * Chat [id] has gone up in smoke: it is deleted, on its own or as part of a clear. A chat
     * already deleted stays as it is: smoke can't take a chat away twice.
     */
    fun deleted(id: String) {
        val from = placeOf(id)
        if (from == ChatPlace.Deleted) return
        places[id] = ChatPlace.Deleted
        if (clearingNow && id !in clearing && from == ChatPlace.Inbox) {
            // Deleted on its own while a clear burns: it joins the clear, so the clear's Undo,
            // which takes the place of its own, brings it back too.
            clearing = clearing + id
        }
        if (id in clearing) {
            cleared += id
            settleClear()
        } else record(ChatChange.Deleted, listOf(id), from)
    }

    /** Chat [id] has gone in mist from the inbox: it is archived, unless it already was. */
    fun archived(id: String) {
        if (placeOf(id) != ChatPlace.Inbox) return
        places[id] = ChatPlace.Archived
        leaveClear(id)
        record(ChatChange.Archived, listOf(id), ChatPlace.Inbox)
    }

    /**
     * Chat [id] has gone in mist from the archive: it is back in the inbox, unless it already was.
     */
    fun restored(id: String) {
        if (placeOf(id) != ChatPlace.Archived) return
        places[id] = ChatPlace.Inbox
        record(ChatChange.Restored, listOf(id), ChatPlace.Archived)
    }

    /**
     * A clear of [ids] has begun: they are one action once the last has burnt. Nothing happens if
     * there are none, or a clear is already going.
     */
    fun clearing(ids: List<String>) {
        if (ids.isEmpty() || clearingNow) return
        clearing = ids
        cleared.clear()
        clearingNow = true
    }

    /**
     * Chat [id] of a clear won't burn with it: it went another way, to the archive by a screen
     * reader's action before its turn, or its turn found it off the screen, in the archive. The
     * clear is the rest of them.
     */
    fun leaveClear(id: String) {
        if (id !in clearing) return
        clearing = clearing - id
        settleClear()
    }

    private fun settleClear() {
        if (!clearingNow || cleared.size < clearing.size) return
        if (cleared.isNotEmpty()) record(ChatChange.Cleared, cleared.toList(), ChatPlace.Inbox)
        clearing = emptyList()
        cleared.clear()
        clearingNow = false
    }

    /** Puts the last action back, and returns it, if there is one. */
    fun undo(): InboxAction? {
        val action = last ?: return null
        for (id in action.ids) places[id] = action.from
        last = null
        return action
    }

    /** The snackbar for action [serial] has gone: it can't be undone any more. */
    fun settle(serial: Int) {
        if (last?.serial == serial) last = null
    }

    /** Puts every chat back where it started, with nothing to undo. */
    fun reset() {
        places.clear()
        places.putAll(InboxChats.start)
        last = null
        clearing = emptyList()
        cleared.clear()
        clearingNow = false
    }

    private fun record(change: ChatChange, ids: List<String>, from: ChatPlace) {
        serial++
        last = InboxAction(change, ids, from, serial)
    }
}

/** A [ChatInbox], as it starts. */
@Composable internal fun rememberChatInbox(): ChatInbox = remember { ChatInbox() }
