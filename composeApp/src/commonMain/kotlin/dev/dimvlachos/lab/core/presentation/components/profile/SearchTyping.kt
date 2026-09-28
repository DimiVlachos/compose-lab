package dev.dimvlachos.lab.core.presentation.components.profile

// The demo cannot type, so the query types itself: one character per interval, counted from the
// moment the bar has landed. A pure function of elapsed time, so a second open starts from "".
internal const val SearchPerCharMs = 120L

internal fun searchQueryAt(elapsedMs: Long, full: String, perCharMs: Long): String {
    if (elapsedMs < 0) return ""
    val count = (elapsedMs / perCharMs).toInt().coerceIn(0, full.length)
    return full.take(count)
}

internal fun searchResults(query: String, candidates: List<String>): List<String> =
    if (query.isBlank()) emptyList()
    else candidates.filter { it.contains(query.trim(), ignoreCase = true) }

// The grid keeps a photo while its title contains the query; a blank query keeps them all.
internal fun matchesQuery(title: String, query: String): Boolean =
    query.isBlank() || title.contains(query.trim(), ignoreCase = true)
