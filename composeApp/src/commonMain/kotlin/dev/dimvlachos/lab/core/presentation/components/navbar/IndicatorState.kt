package dev.dimvlachos.lab.core.presentation.components.navbar

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Stable
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sign
import kotlin.math.sqrt
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

@Stable
internal class IndicatorState(initialIndex: Int) {
    private val left = Animatable(initialIndex.toFloat())
    private val right = Animatable(initialIndex + 1f)
    private val calmCenter = Animatable(initialIndex + 0.5f)
    private val heading = Animatable(1f)

    val leftSlot: Float
        get() = left.value

    val rightSlot: Float
        get() = right.value

    val centerSlot: Float
        get() = (left.value + right.value) / 2f

    val calmCenterSlot: Float
        get() = calmCenter.value

    val lean: Float
        get() {
            val stretch = right.value - left.value - 1f
            val travel = max(0f, stretch)
            val landing = max(0f, -stretch)
            return heading.value * (travel + NavBarDimens.BubbleLandingGain * landing)
        }

    suspend fun animateTo(index: Int, stretch: Boolean) {
        val movingRight = index + 0.5f >= centerSlot
        val lead = if (movingRight) right else left
        val trail = if (movingRight) left else right
        val leadTarget = if (movingRight) index + 1f else index.toFloat()
        val trailTarget = if (movingRight) index.toFloat() else index + 1f
        val trailSpring = if (stretch) TrailSpring else LeadSpring
        val calmTarget = index + 0.5f
        val calmVelocity =
            nonOvershootingVelocity(calmCenter.value - calmTarget, calmCenter.velocity)
        val newHeading = if (calmTarget >= calmCenter.value) 1f else -1f
        if (abs(right.value - left.value - 1f) < RestingStretch) heading.snapTo(newHeading)
        coroutineScope {
            launch { heading.animateTo(newHeading, HeadingSpring) }
            launch { lead.animateTo(leadTarget, LeadSpring) }
            launch { trail.animateTo(trailTarget, trailSpring) }
            launch { calmCenter.animateTo(calmTarget, CalmCenterSpring, calmVelocity) }
        }
    }

    suspend fun snapTo(index: Int) {
        left.snapTo(index.toFloat())
        right.snapTo(index + 1f)
        calmCenter.snapTo(index + 0.5f)
    }

    private companion object {
        const val CalmCenterStiffness = 300f
        const val RestingStretch = 0.02f
        val LeadSpring = spring<Float>(dampingRatio = 0.8f, stiffness = 700f)
        val TrailSpring = spring<Float>(dampingRatio = 0.6f, stiffness = 170f)
        val HeadingSpring =
            spring<Float>(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = 200f)
        val CalmCenterSpring =
            spring<Float>(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = CalmCenterStiffness,
            )

        fun nonOvershootingVelocity(offset: Float, velocity: Float): Float {
            val towardsTarget = velocity.sign == -offset.sign
            val limit = sqrt(CalmCenterStiffness) * abs(offset)
            return if (towardsTarget && abs(velocity) > limit) -offset.sign * limit else velocity
        }
    }
}
