package dev.dimvlachos.moodboard.android.nav

enum class NavStep {
    Push,
    Pop,
}

/**
 * How the visible stack [to] follows [from]: one screen pushed on top, one popped off, or neither.
 * A tab switch swaps whole stacks, and can grow or shrink the visible list by several screens;
 * NavDisplay alone would read that as a push or a pop of the screen on top.
 */
fun navStep(from: List<Any>, to: List<Any>): NavStep? =
    when {
        to.size == from.size + 1 && to.subList(0, from.size) == from -> NavStep.Push
        from.size == to.size + 1 && from.subList(0, to.size) == to -> NavStep.Pop
        else -> null
    }
