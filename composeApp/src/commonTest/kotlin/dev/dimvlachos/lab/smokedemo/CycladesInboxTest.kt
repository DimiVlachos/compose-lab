package dev.dimvlachos.lab.smokedemo

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame

class CycladesInboxTest {
    @Test
    fun itStartsWithSevenInTheInboxAndTwoArchived() {
        val inbox = CycladesInbox()
        assertEquals(7, inbox.chatsIn(ChatPlace.Inbox).size)
        assertEquals(listOf("pottery", "kite"), inbox.chatsIn(ChatPlace.Archived).map { it.id })
        assertEquals(5, inbox.readInInbox().size)
    }

    @Test
    fun undoPutsADeletedChatBack() {
        val inbox = CycladesInbox()
        inbox.deleted("maria")
        assertEquals(ChatPlace.Deleted, inbox.placeOf("maria"))
        assertEquals(ChatChange.Deleted, inbox.last!!.change)
        inbox.undo()
        assertEquals(ChatPlace.Inbox, inbox.placeOf("maria"))
        assertNull(inbox.last)
    }

    @Test
    fun archivingAndRestoringMoveAChatAndUndoMovesItBack() {
        val inbox = CycladesInbox()
        inbox.archived("nikos")
        assertEquals(ChatPlace.Archived, inbox.placeOf("nikos"))
        inbox.undo()
        assertEquals(ChatPlace.Inbox, inbox.placeOf("nikos"))
        inbox.restored("kite")
        assertEquals(ChatPlace.Inbox, inbox.placeOf("kite"))
        inbox.undo()
        assertEquals(ChatPlace.Archived, inbox.placeOf("kite"))
    }

    @Test
    fun aClearIsOneActionOnceItsLastChatHasBurnt() {
        val inbox = CycladesInbox()
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
        val inbox = CycladesInbox()
        inbox.deleted("anna")
        inbox.settle(inbox.last!!.serial)
        assertNull(inbox.undo())
        assertEquals(ChatPlace.Deleted, inbox.placeOf("anna"))
        inbox.reset()
        assertEquals(ChatPlace.Inbox, inbox.placeOf("anna"))
    }

    @Test
    fun smokeCantTakeAChatTwice() {
        val inbox = CycladesInbox()
        inbox.deleted("maria")
        val first = inbox.last
        inbox.deleted("maria")
        inbox.archived("maria")
        inbox.restored("maria")
        assertSame(first, inbox.last, "nothing happened the second time")
        inbox.archived("kite")
        assertSame(first, inbox.last, "kite was archived already")
    }

    @Test
    fun aSecondClearWhileOneBurnsIsIgnored() {
        val inbox = CycladesInbox()
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
        val inbox = CycladesInbox()
        val read = inbox.readInInbox().map { it.id }
        inbox.clearing(read)
        inbox.deleted("eleni")
        assertNull(inbox.last, "no snackbar of its own to be replaced")
        read.forEach(inbox::deleted)
        assertEquals(read.size + 1, inbox.last!!.ids.size)
        inbox.undo()
        assertEquals(ChatPlace.Inbox, inbox.placeOf("eleni"))
        assertEquals(5, inbox.readInInbox().size)
    }

    @Test
    fun aClearChatArchivedBeforeItsTurnLeavesTheClear() {
        val inbox = CycladesInbox()
        val read = inbox.readInInbox().map { it.id }
        inbox.clearing(read)
        inbox.archived(read.last())
        read.dropLast(1).forEach(inbox::deleted)
        assertEquals(ChatChange.Cleared, inbox.last!!.change)
        assertEquals(read.dropLast(1), inbox.last!!.ids)
    }

    @Test
    fun aClearChatLeftOutBeforeItsTurnLeavesTheClearAndTheRestSettleIt() {
        val inbox = CycladesInbox()
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
        val inbox = CycladesInbox()
        val read = inbox.readInInbox().map { it.id }
        inbox.clearing(read)
        read.forEach(inbox::leaveClear)
        assertFalse(inbox.isClearing)
        assertNull(inbox.last)
    }
}
