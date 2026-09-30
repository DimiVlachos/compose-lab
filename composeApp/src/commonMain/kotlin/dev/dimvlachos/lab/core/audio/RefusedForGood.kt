package dev.dimvlachos.lab.core.audio

/**
 * Whether a refused microphone request means the system will not ask again, judged from its only
 * clue, whether it would show a rationale, before the request and after it. A first dialog refused
 * or dismissed is not for good; a refusal after a rationale is. Already refused for good before
 * this visit, the system answers at once with no dialog, and neither clue changes, so a second such
 * refusal counts too.
 */
internal fun refusedForGood(
    rationaleBefore: Boolean,
    rationaleAfter: Boolean,
    refusals: Int,
): Boolean = !rationaleAfter && (rationaleBefore || refusals >= 2)
