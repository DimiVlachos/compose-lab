package dev.dimvlachos.lab.core.demo

interface DemoController {
    val selectedIndex: Int

    fun select(index: Int)

    suspend fun scrollBy(px: Float)
}
