package dev.dimvlachos.lab.typewriterdemo.presentation.components

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest

class TypewriterThreadTest {
    @Test
    fun theReplyTakesTheIdOfTheBubbleThatShowedItBeingTyped() = runTest {
        val thread = TypewriterThread(listOf("hi" to false), listOf("hello"))
        thread.send("yo", listOf("yo"))
        launch { thread.answer() }
        advanceTimeBy(1_000)
        assertTrue(thread.typing)
        val typing = thread.typingId
        // A slip sent while the reply is typed never takes its id.
        val slip = thread.send("again", listOf("again"))
        assertTrue(slip.id != typing)
        advanceTimeBy(2_000)
        assertFalse(thread.typing)
        val reply = thread.messages.last()
        assertEquals("hello", reply.text)
        assertEquals(typing, reply.id)
        assertTrue(thread.isNew(reply.id))
        assertFalse(thread.isNew(thread.messages.first().id))
    }

    @Test
    fun aListHearsOfEachArrivalJustBeforeItComes() = runTest {
        val thread = TypewriterThread(listOf("hi" to false), listOf("hello"))
        val heard = mutableListOf<Pair<Int, Boolean>>()
        thread.onArrival = { heard += thread.messages.size to thread.typing }
        thread.send("yo", listOf("yo"))
        launch { thread.answer() }
        advanceTimeBy(3_000)
        // Before the slip, and before the typing bubble; the reply takes the bubble's place.
        assertEquals(listOf(1 to false, 2 to false), heard)
        assertEquals(3, thread.messages.size)
    }
}
