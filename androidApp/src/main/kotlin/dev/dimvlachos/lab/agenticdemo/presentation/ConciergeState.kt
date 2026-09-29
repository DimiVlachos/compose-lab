package dev.dimvlachos.lab.agenticdemo.presentation

data class ConciergeState(
    val transcript: List<TranscriptItem> = emptyList(),
    val thinking: Boolean = false,
    val hasApiKey: Boolean = true,
)
