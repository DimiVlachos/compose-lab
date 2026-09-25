package dev.dimvlachos.lab.core.demo

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue

@Stable
class DemoState : DemoController {
    override var selectedIndex: Int by mutableIntStateOf(0)
        private set

    private var scrollHandler: (suspend (Float) -> Unit)? = null

    override fun select(index: Int) {
        selectedIndex = index
    }

    override suspend fun scrollBy(px: Float) {
        scrollHandler?.invoke(px)
    }

    fun setScrollHandler(handler: (suspend (Float) -> Unit)?) {
        scrollHandler = handler
    }
}
