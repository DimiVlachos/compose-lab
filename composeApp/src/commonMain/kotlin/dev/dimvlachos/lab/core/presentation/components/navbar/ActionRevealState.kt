package dev.dimvlachos.lab.core.presentation.components.navbar

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Stable
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

@Stable
internal class ActionRevealState(initialAction: NavAction?) {
    private val reveal = Animatable(if (initialAction != null) 1f else 0f)
    private val scaleX = Animatable(1f)
    private val scaleY = Animatable(1f)
    private var current = initialAction

    val revealValue: Float
        get() = reveal.value

    val scaleXValue: Float
        get() = scaleX.value

    val scaleYValue: Float
        get() = scaleY.value

    suspend fun animateTo(target: NavAction?) {
        val previous = current
        current = target
        when {
            target != null && previous == null ->
                coroutineScope {
                    launch { reveal.animateTo(1f, tween(200)) }
                    jellyIn(scaleX, scaleY)
                }
            target != null && previous != target ->
                coroutineScope {
                    launch { reveal.animateTo(1f, tween(200)) }
                    squash(scaleX, scaleY)
                }
            target != null ->
                coroutineScope {
                    launch { reveal.animateTo(1f, tween(200)) }
                    launch { scaleX.animateTo(1f, tween(160)) }
                    scaleY.animateTo(1f, tween(160))
                }
            else ->
                coroutineScope {
                    launch { reveal.animateTo(0f, tween(150)) }
                    launch { scaleX.animateTo(0.6f, tween(150)) }
                    scaleY.animateTo(0.6f, tween(150))
                }
        }
    }

    suspend fun squashTap() {
        squash(scaleX, scaleY)
    }
}
