package dev.dimvlachos.lab.agenticdemo.data

import kotlin.test.Test
import kotlin.test.assertEquals

class JsonlLineSplitterTest {
    @Test
    fun linesSplitAcrossDeltasComeOutWhole() {
        val splitter = JsonlLineSplitter()

        val first = splitter.push("Here you go.\n{\"version\":")
        val second = splitter.push("\"v0.9\"}\n{\"a\"")
        val rest = splitter.flush()

        assertEquals(listOf(AgentChunk.Prose("Here you go.")), first)
        assertEquals(listOf(AgentChunk.A2uiLine("{\"version\":\"v0.9\"}")), second)
        assertEquals(listOf(AgentChunk.A2uiLine("{\"a\"")), rest)
    }

    @Test
    fun blankLinesAndCodeFencesAreDropped() {
        val splitter = JsonlLineSplitter()

        val chunks = splitter.push("```jsonl\n\n   \n{}\n```\n")

        assertEquals(listOf(AgentChunk.A2uiLine("{}")), chunks)
    }

    @Test
    fun flushOnAnEmptyBufferIsEmpty() {
        assertEquals(emptyList(), JsonlLineSplitter().flush())
    }
}
