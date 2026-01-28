package com.example.mygame

/**
 * Describes the health of the player.
 *
 * @property health The health bar of the player. Once the player loses all of their health, the player dies and restarts
 * the level.
 * @property energy Cat Slime has an energy bar as well as a health bar. The energy bar decreases for each
 * time the player changes state (in [PlayerState.Liquid] or [PlayerState.Solid]), or makes a constant
 * decrease for when the player is in [PlayerState.Gas] (with a max time of 5s).
 *
 * When the energy bar is 0, the player is unable to change states, which will lock them out of the puzzle. As a last-ditch
 * effort, the energy bar can be refilled up by transferring health at a ratio of `1:5`.
 */
data class PlayerHealth(
    var health: Percentage,
    var energy: Percentage,
) {
    companion object {
        /**
         * Creates a new [PlayerHealth] object with everything full and no upgrades (100%, 100%).
         */
        fun full(): PlayerHealth {
            return PlayerHealth(
                health = Percentage(
                    current = 100.0,
                    total = 100.0,
                ),
                energy = Percentage(
                    current = 100.0,
                    total = 100.0,
                ),
            )
        }
    }

    /**
     * Siphons health into energy as a `1:5` ratio for each frame this is run on (6 frames of the `H` key being pressed
     * leads to 30 points of energy being replenished, and 6 hp being reduced).
     */
    fun siphon(healthAmount: Double = 1.0) {
        val actualHealthToTake = minOf(healthAmount, health.current)
        val energyToGain = actualHealthToTake * 5.0

        health.current -= actualHealthToTake
        energy.current = minOf(energy.total, energy.current + energyToGain)
    }
}
