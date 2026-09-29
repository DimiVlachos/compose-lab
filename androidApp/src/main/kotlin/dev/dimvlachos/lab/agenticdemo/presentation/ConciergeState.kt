package dev.dimvlachos.lab.agenticdemo.presentation

/**
 * [seenSurfaces] have been on screen at least once, so one that is gone was closed by the agent;
 * [failedSurfaces] were created by the agent but never made it onto the screen.
 */
data class ConciergeState(
    val transcript: List<TranscriptItem> = emptyList(),
    val thinking: Boolean = false,
    val hasApiKey: Boolean = true,
    val seenSurfaces: Set<String> = emptySet(),
    val failedSurfaces: Set<String> = emptySet(),
)
