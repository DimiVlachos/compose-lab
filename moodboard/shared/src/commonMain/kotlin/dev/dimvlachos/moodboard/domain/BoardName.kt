package dev.dimvlachos.moodboard.domain

/**
 * The name a board is saved under: trimmed, or null when nothing is left. One rule, every caller.
 */
fun boardName(raw: String): String? = raw.trim().takeIf { it.isNotEmpty() }
