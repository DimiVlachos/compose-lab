package dev.dimvlachos.lab.core.presentation.components.profile

import kotlin.test.Test
import kotlin.test.assertEquals

class SearchTypingTest {
    @Test
    fun nothingIsTypedBeforeTheFirstInterval() {
        assertEquals("", searchQueryAt(elapsedMs = -5, full = "Cor", perCharMs = 120))
        assertEquals("", searchQueryAt(elapsedMs = 119, full = "Cor", perCharMs = 120))
    }

    @Test
    fun oneCharacterAppearsPerInterval() {
        assertEquals("C", searchQueryAt(elapsedMs = 120, full = "Cor", perCharMs = 120))
        assertEquals("Co", searchQueryAt(elapsedMs = 240, full = "Cor", perCharMs = 120))
        assertEquals("Cor", searchQueryAt(elapsedMs = 360, full = "Cor", perCharMs = 120))
    }

    @Test
    fun typingStopsAtTheFullQuery() {
        assertEquals("Cor", searchQueryAt(elapsedMs = 10_000, full = "Cor", perCharMs = 120))
    }

    @Test
    fun aTitleMatchesWhenItContainsTheQueryIgnoringCase() {
        assertEquals(true, matchesQuery("Naxos", "xos"))
        assertEquals(true, matchesQuery("Paxos", "XOS "))
        assertEquals(false, matchesQuery("Corfu", "xos"))
    }

    @Test
    fun aBlankQueryMatchesEverything() {
        assertEquals(true, matchesQuery("Corfu", ""))
        assertEquals(true, matchesQuery("Corfu", "  "))
    }
}
