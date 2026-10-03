package dev.dimvlachos.moodboard.android.nav

import androidx.compose.runtime.MutableState
import androidx.navigation3.runtime.NavKey

/**
 * One back stack per tab, so a tab keeps its place while you visit another, as in an iOS TabView.
 * The stacks and the current tab are owned by the caller (saveable state); this holds the rules.
 * NavDisplay sees the start tab's stack followed by the current tab's: back pops within the tab,
 * from a tab's root it returns to the start tab, and from the start tab's root the app closes.
 */
class TopLevelBackStack(
    private val start: NavKey,
    private val stacks: Map<NavKey, MutableList<NavKey>>,
    private val current: MutableState<NavKey>,
) {
    val currentTab: NavKey
        get() = current.value

    val visible: List<NavKey>
        get() = stacks.getValue(start) + if (currentTab == start) emptyList() else currentStack

    private val currentStack
        get() = stacks.getValue(currentTab)

    fun selectTab(tab: NavKey) {
        current.value = tab
    }

    /** A key already on top is ignored, so a double tap can't stack two copies of a screen. */
    fun push(key: NavKey) {
        if (currentStack.last() != key) currentStack.add(key)
    }

    /** Returns false when there's nothing left to pop, so the system can close the app. */
    fun pop(): Boolean =
        when {
            currentStack.size > 1 -> {
                currentStack.removeAt(currentStack.lastIndex)
                true
            }
            currentTab != start -> {
                current.value = start
                true
            }
            else -> false
        }

    /**
     * Drops every screen whose photo or board no longer exists, in every tab. Screens never pop
     * themselves on a delete, so a hidden tab's stale screen can't navigate the visible one.
     */
    fun prune(photoIds: Set<String>, boardIds: Set<String>) {
        stacks.values.forEach { stack ->
            stack.removeAll { key ->
                when (key) {
                    is PhotoDetail -> key.photoId !in photoIds
                    is BoardDetail -> key.boardId !in boardIds
                    is BoardEditor -> key.boardId !in boardIds
                    else -> false
                }
            }
        }
    }
}
