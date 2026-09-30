package dev.dimvlachos.lab.core.demo

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import kotlin.time.Duration

@Stable
class DemoState : DemoController {
    override var selectedIndex: Int by mutableIntStateOf(0)
        private set

    private var scrollHandler: (suspend (Float) -> Unit)? = null
    private var wipeHandler: (suspend (List<Offset>, Duration) -> Unit)? = null

    override fun select(index: Int) {
        selectedIndex = index
    }

    override suspend fun scrollBy(px: Float) {
        scrollHandler?.invoke(px)
    }

    override suspend fun wipe(path: List<Offset>, duration: Duration) {
        wipeHandler?.invoke(path, duration)
    }

    fun setScrollHandler(handler: (suspend (Float) -> Unit)?) {
        scrollHandler = handler
    }

    fun setWipeHandler(handler: (suspend (List<Offset>, Duration) -> Unit)?) {
        wipeHandler = handler
    }
}
