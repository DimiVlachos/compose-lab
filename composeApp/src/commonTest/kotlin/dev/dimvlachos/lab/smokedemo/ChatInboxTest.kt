package dev.dimvlachos.lab.smokedemo

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame

class ChatInboxTest {
    @Test
    fun itStartsWithSevenInTheInboxAndTwoArchived() {
        val inbox = ChatInbox()
        assertEquals(7, inbox.chatsIn(ChatPlace.Inbox).size)
        assertEquals(listOf("pottery", "kite"), inbox.chatsIn(ChatPlace.Archived).map { it.id })
        assertEquals(5, inbox.readInInbox().size)
    }

    @Test
    fun undoPutsADeletedChatBack() {
        val inbox = ChatInbox()
        inbox.deleted("patterson")
        assertEquals(ChatPlace.Deleted, inbox.placeOf("patterson"))
        assertEquals(ChatChange.Deleted, inbox.last!!.change)
        inbox.undo()
        assertEquals(ChatPlace.Inbox, inbox.placeOf("patterson"))
        assertNull(inbox.last)
    }

    @Test
    fun archivingAndRestoringMoveAChatAndUndoMovesItBack() {
        val inbox = ChatInbox()
        inbox.archived("jake")
        assertEquals(ChatPlace.Archived, inbox.placeOf("jake"))
        inbox.undo()
        assertEquals(ChatPlace.Inbox, inbox.placeOf("jake"))
        inbox.restored("kite")
        assertEquals(ChatPlace.Inbox, inbox.placeOf("kite"))
        inbox.undo()
        assertEquals(ChatPlace.Archived, inbox.placeOf("kite"))
    }

    @Test
    fun aClearIsOneActionOnceItsLastChatHasBurnt() {
        val inbox = ChatInbox()
        val read = inbox.readInInbox().map { it.id }
        inbox.clearing(read)
        read.dropLast(1).forEach(inbox::deleted)
        assertNull(inbox.last, "no snackbar until the wave is done")
        inbox.deleted(read.last())
        assertEquals(ChatChange.Cleared, inbox.last!!.change)
        assertEquals(read, inbox.last!!.ids)
        inbox.undo()
        assertEquals(5, inbox.readInInbox().size)
    }

    @Test
    fun aSnackbarThatHasGoneCantBeUndone() {
        val inbox = ChatInbox()
        inbox.deleted("maddie")
        inbox.settle(inbox.last!!.serial)
        assertNull(inbox.undo())
        assertEquals(ChatPlace.Deleted, inbox.placeOf("maddie"))
        inbox.reset()
        assertEquals(ChatPlace.Inbox, inbox.placeOf("maddie"))
    }

    @Test
    fun smokeCantTakeAChatTwice() {
        val inbox = ChatInbox()
        inbox.deleted("patterson")
        val first = inbox.last
        inbox.deleted("patterson")
        inbox.archived("patterson")
        inbox.restored("patterson")
        assertSame(first, inbox.last, "nothing happened the second time")
        inbox.archived("kite")
        assertSame(first, inbox.last, "kite was archived already")
    }

    @Test
    fun aSecondClearWhileOneBurnsIsIgnored() {
        val inbox = ChatInbox()
        val read = inbox.readInInbox().map { it.id }
        inbox.clearing(read)
        inbox.deleted(read[0])
        inbox.clearing(read.drop(1))
        read.drop(1).forEach(inbox::deleted)
        assertEquals(read, inbox.last!!.ids, "one clear of all five")
        assertFalse(inbox.isClearing)
    }

    @Test
    fun aChatDeletedOnItsOwnDuringAClearJoinsIt() {
        val inbox = ChatInbox()
        val read = inbox.readInInbox().map { it.id }
        inbox.clearing(read)
        inbox.deleted("emma")
        assertNull(inbox.last, "no snackbar of its own to be replaced")
        read.forEach(inbox::deleted)
        assertEquals(read.size + 1, inbox.last!!.ids.size)
        inbox.undo()
        assertEquals(ChatPlace.Inbox, inbox.placeOf("emma"))
        assertEquals(5, inbox.readInInbox().size)
    }

    @Test
    fun aClearChatArchivedBeforeItsTurnLeavesTheClear() {
        val inbox = ChatInbox()
        val read = inbox.readInInbox().map { it.id }
        inbox.clearing(read)
        inbox.archived(read.last())
        read.dropLast(1).forEach(inbox::deleted)
        assertEquals(ChatChange.Cleared, inbox.last!!.change)
        assertEquals(read.dropLast(1), inbox.last!!.ids)
    }

    @Test
    fun aClearChatLeftOutBeforeItsTurnLeavesTheClearAndTheRestSettleIt() {
        val inbox = ChatInbox()
        val read = inbox.readInInbox().map { it.id }
        inbox.clearing(read)
        read.take(2).forEach(inbox::deleted)
        // The rest are dropped: the screen they were on went before their turn.
        read.drop(2).forEach(inbox::leaveClear)
        assertFalse(inbox.isClearing, "the clear is over")
        assertEquals(ChatChange.Cleared, inbox.last!!.change)
        assertEquals(read.take(2), inbox.last!!.ids, "what did burn can be undone")
    }

    @Test
    fun aClearWithNothingBurntEndsWithNothingToUndo() {
        val inbox = ChatInbox()
        val read = inbox.readInInbox().map { it.id }
        inbox.clearing(read)
        read.forEach(inbox::leaveClear)
        assertFalse(inbox.isClearing)
        assertNull(inbox.last)
    }
}
