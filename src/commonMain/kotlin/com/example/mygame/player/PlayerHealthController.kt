package com.example.mygame.player

import com.example.mygame.PlayerHealth

class PlayerHealthController {

    val health: PlayerHealth = PlayerHealth.full()

    fun hasEnergy(): Boolean = health.energy.current > 0.0

    fun canSiphon(): Boolean =
        health.energy.current < health.energy.total && health.health.current > 5

    fun drainSprintEnergy(amount: Double = 2.0) {
        health.energy.current -= amount
    }

    fun drainGasEnergy(drainRate: Double, elapsed: Double) {
        health.energy.current = (health.energy.total - drainRate * elapsed)
            .coerceIn(0.0, health.energy.total)
    }

    fun siphon(healthAmount: Double = 0.25) = health.siphon(healthAmount)
}