package com.example.mygame.player

import com.dropbear.logging.Logger
import com.example.mygame.PlayerState
import kotlin.time.Clock
import kotlin.time.Instant

class PlayerStateController {

    var state: PlayerState = PlayerState.Solid
        private set
    var previousState: PlayerState = PlayerState.Solid
        private set

    private var gasStart: Instant? = null
    var currentGasTime: Double = 0.0

    fun update(input: PlayerInput, health: PlayerHealthController) {
        if (input.switchDown) trySwitch(down = true, health)
        if (input.switchUp)   trySwitch(down = false, health)
    }

    /**
     * Should be called every frame while in [PlayerState.Gas].
     * Drains energy and returns the vertical velocity to apply ([gasFloatSpeed]),
     * or forces a switch to [PlayerState.Solid] and returns 0.0 if energy is depleted.
     */
    fun applyGasPhysics(gasFloatSpeed: Double, health: PlayerHealthController): Double {
        val elapsed = gasStart?.let {
            (Clock.System.now() - it).inWholeMilliseconds / 1000.0
        } ?: 0.0
        currentGasTime = elapsed

        health.drainGasEnergy(drainRate = 35.0, elapsed = elapsed)

        if (!health.hasEnergy()) {
            Logger.info("Energy depleted — reverting to Solid")
            switchTo(PlayerState.Solid)
            return 0.0
        }

        return gasFloatSpeed
    }

    private fun trySwitch(down: Boolean, health: PlayerHealthController) {
        val next = if (down) {
            when (state) {
                PlayerState.Liquid -> PlayerState.Liquid
                PlayerState.Solid  -> PlayerState.Liquid
                PlayerState.Gas    -> PlayerState.Solid
            }
        } else {
            when (state) {
                PlayerState.Liquid -> PlayerState.Solid
                PlayerState.Solid  -> if (health.hasEnergy()) PlayerState.Gas else {
                    Logger.info("No energy available. Heal with `H` or `GamepadButton.North`")
                    PlayerState.Solid
                }
                PlayerState.Gas    -> PlayerState.Gas
            }
        }
        if (next != state) switchTo(next)
    }

    private fun switchTo(next: PlayerState) {
        Logger.info("playerState: $state -> $next")
        previousState = state
        state         = next
        gasStart      = if (next == PlayerState.Gas) Clock.System.now() else null
        currentGasTime = 0.0
        // TODO: switch models here
    }
}