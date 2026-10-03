package dev.dimvlachos.moodboard.android.nav

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.navigation3.runtime.NavKey

/**
 * One back stack per tab, so a tab keeps its place while you visit another, as in an iOS TabView.
 * NavDisplay sees the start tab's stack followed by the current tab's: back pops within the tab,
 * from a tab's root it returns to the start tab, and from the start tab's root the app closes.
 */
class TopLevelBackStack(private val start: NavKey) {
    private val stacks = linkedMapOf(start to mutableListOf(start))

    var currentTab: NavKey by mutableStateOf(start)
        private set

    val backStack = mutableStateListOf(start)

    fun selectTab(tab: NavKey) {
        stacks.getOrPut(tab) { mutableListOf(tab) }
        currentTab = tab
        rebuild()
    }

    /** A key already on top is ignored, so a double tap can't stack two copies of a screen. */
    fun push(key: NavKey) {
        val stack = stacks.getValue(currentTab)
        if (stack.last() == key) return
        stack.add(key)
        rebuild()
    }

    /** Returns false when there's nothing left to pop, so the system can close the app. */
    fun pop(): Boolean {
        val stack = stacks.getValue(currentTab)
        when {
            stack.size > 1 -> stack.removeAt(stack.lastIndex)
            currentTab != start -> currentTab = start
            else -> return false
        }
        rebuild()
        return true
    }

    private fun rebuild() {
        val visible =
            stacks.getValue(start) +
                if (currentTab == start) emptyList() else stacks.getValue(currentTab)
        backStack.clear()
        backStack.addAll(visible)
    }
}

/** Keeps the tabs' back stacks across configuration changes. */
class NavigationViewModel : ViewModel() {
    val stack = TopLevelBackStack(Gallery)
}
