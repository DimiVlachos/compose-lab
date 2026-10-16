package dev.dimvlachos.lab.core.presentation.components.snowglobe

/** The globe's physics and thresholds. Lengths are in globe units (the sphere's radius is 1). */
internal object SnowGlobeDimens {
    const val Flakes = 230
    /** The water's radius inside the glass. */
    const val InnerRadius = 0.92f

    /** Swirl energy: the most it can hold, and how fast it fades (per second, exponential). */
    const val MaxEnergy = 1.6f
    const val EnergyDecay = 0.9f
    /**
     * A shake fires the refresh above this; the globe counts as settled below [SettleThreshold].
     */
    const val ShakeThreshold = 0.6f
    const val SettleThreshold = 0.2f
    /** While a refresh is pending the water keeps at least this much swirl. */
    const val PendingFloor = 0.35f
    /** Resting flakes lift off above this energy. */
    const val LiftEnergy = 0.15f

    /** The glass's spring toward the finger, and how much its acceleration stirs the water. */
    const val SpringStiffness = 260f
    const val SpringDamping = 18f
    const val AccelToEnergy = 0.0016f
    /** How far the finger can pull the glass from its rest, so a long fling can't run it away. */
    const val MaxDrag = 0.6f
    /** A day change's rock: a sideways kick and the energy it leaves, under [ShakeThreshold]. */
    const val RockKick = 1.4f
    const val RockEnergy = 0.2f
    /** A keyboard or scripted shake. */
    const val ShakeEnergy = 1.2f

    /**
     * The water's flow at full swirl: how fast its vortex turns, how hard it lifts up the middle
     * (fading toward the top of the glass, so the glitter clouds the water instead of crowding the
     * glass), and how strong its slow eddies are.
     */
    const val VortexGain = 0.5f
    const val LiftGain = 1.3f
    const val EddyGain = 0.35f

    /** Sinking speed and how quickly a flake takes up the flow's velocity. */
    const val Sink = 0.4f
    const val Drag = 6f
    const val Flutter = 0.05f

    /** How long a change of scene crossfades. */
    const val SceneFadeSeconds = 0.6f
}
