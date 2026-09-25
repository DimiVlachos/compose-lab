package dev.dimvlachos.lab.core.demo

class FakeController(private val now: () -> Long) : DemoController {
    override var selectedIndex: Int = 0
        private set

    var netScroll: Float = 0f
        private set

    val calls = mutableListOf<Pair<Long, String>>()

    override fun select(index: Int) {
        selectedIndex = index
        calls += now() to "select($index)"
    }

    override suspend fun scrollBy(px: Float) {
        netScroll += px
        calls += now() to "scrollBy($px)"
    }
}
