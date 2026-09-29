package dev.dimvlachos.lab.agenticdemo.data

sealed interface AgentChunk {
    data class Prose(val text: String) : AgentChunk

    data class A2uiLine(val json: String) : AgentChunk

    data object Refused : AgentChunk
}
