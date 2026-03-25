package com.example.mygame.player

import com.dropbear.components.EntityTransform
import com.dropbear.math.Quaterniond
import com.dropbear.math.Vector3d
import com.dropbear.physics.KinematicCharacterController
import com.example.mygame.PlayerState

class PlayerMovement {

    var verticalVelocity: Double = 0.0
    var isMoving: Boolean = false
    var direction: Vector3d = Vector3d.zero()

    fun update(
        input: PlayerInput,
        state: PlayerState,
        isGrounded: Boolean,
        speed: Double,
        jumpHeight: Double,
        gravity: Double,
        deltaTime: Double,
        kcc: KinematicCharacterController,
        transform: EntityTransform,
    ) {
        if (isGrounded && verticalVelocity < 0.0) verticalVelocity = 0.0

        val effectiveSpeed = if (input.isSprinting) speed * 2 else speed

        // jumping
        if (input.jumpPressed && isGrounded) {
            verticalVelocity = if (state == PlayerState.Gas) -jumpHeight else jumpHeight
        }

        // --- Gravity (overridden externally when in Gas state) ---
        if (state != PlayerState.Gas && !isGrounded) {
            verticalVelocity -= gravity * deltaTime
        }

        // apply to kcc
        val dir         = if (input.movement.lengthSquared() > 1e-9) input.movement.normalize() else Vector3d.zero()
        val velocity    = dir * effectiveSpeed
        val translation = Vector3d(velocity.x, verticalVelocity, velocity.z)
        kcc.move(deltaTime, translation)

        // rotation
        if (input.movement.lengthSquared() > 0.001) {
            val targetYaw      = kotlin.math.atan2(input.movement.x, input.movement.z)
            val targetRotation = Quaterniond.fromEulerAngles(0.0, targetYaw, 0.0)
            val t = (10.0 * deltaTime).coerceIn(0.0, 1.0)
            transform.world.rotation = transform.world.rotation.slerp(targetRotation, t)
        }

        isMoving  = input.movement.lengthSquared() > 0.001
        direction = input.movement
    }
}