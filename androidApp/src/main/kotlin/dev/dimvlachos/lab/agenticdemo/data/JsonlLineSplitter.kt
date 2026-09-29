package dev.dimvlachos.lab.agenticdemo.data

/**
 * Turns streamed text deltas into whole lines, so each A2UI message reaches the parser the moment
 * its line closes: the UI builds up while the model is still writing. A line that opens with `{` is
 * an A2UI message; anything else is prose for a chat bubble.
 */
class JsonlLineSplitter {
    private val buffer = StringBuilder()

    fun push(delta: String): List<AgentChunk> {
        buffer.append(delta)
        val chunks = mutableListOf<AgentChunk>()
        while (true) {
            val newline = buffer.indexOf('\n')
            if (newline < 0) break
            val line = buffer.substring(0, newline)
            buffer.delete(0, newline + 1)
            classify(line)?.let(chunks::add)
        }
        return chunks
    }

    fun flush(): List<AgentChunk> {
        val line = buffer.toString()
        buffer.clear()
        return listOfNotNull(classify(line))
    }

    private fun classify(line: String): AgentChunk? {
        val trimmed = line.trim()
        return when {
            trimmed.isEmpty() || trimmed.startsWith("```") -> null
            trimmed.startsWith("{") -> AgentChunk.A2uiLine(trimmed)
            else -> AgentChunk.Prose(trimmed)
        }
    }
}
