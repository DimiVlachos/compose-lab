package dev.dimvlachos.lab.core.presentation.components.navbar

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Stable
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

@Stable
internal class IndicatorState(initialIndex: Int) {
    private val left = Animatable(initialIndex.toFloat())
    private val right = Animatable(initialIndex + 1f)
    private val calmCenter = Animatable(initialIndex + 0.5f)

    val leftSlot: Float
        get() = left.value

    val rightSlot: Float
        get() = right.value

    val centerSlot: Float
        get() = (left.value + right.value) / 2f

    val calmCenterSlot: Float
        get() = calmCenter.value

    suspend fun animateTo(index: Int, stretch: Boolean, itemCount: Int) {
        val movingRight = index + 0.5f >= centerSlot
        val lead = if (movingRight) right else left
        val trail = if (movingRight) left else right
        val leadTarget = if (movingRight) index + 1f else index.toFloat()
        val trailTarget = if (movingRight) index.toFloat() else index + 1f
        val trailSpring = if (stretch) TrailSpring else LeadSpring
        coroutineScope {
            launch { lead.animateTo(leadTarget, LeadSpring) }
            launch { trail.animateTo(trailTarget, trailSpring) }
            launch { calmCenter.animateTo(index + 0.5f, CalmCenterSpring) }
        }
    }

    suspend fun snapTo(index: Int) {
        left.snapTo(index.toFloat())
        right.snapTo(index + 1f)
        calmCenter.snapTo(index + 0.5f)
    }

    private companion object {
        val LeadSpring = spring<Float>(dampingRatio = 0.8f, stiffness = 700f)
        val TrailSpring = spring<Float>(dampingRatio = 0.6f, stiffness = 170f)
        val CalmCenterSpring =
            spring<Float>(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = 300f)
    }
}
