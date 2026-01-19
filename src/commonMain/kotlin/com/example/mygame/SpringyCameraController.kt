package com.example.mygame

import com.dropbear.math.Vector3d
import com.dropbear.physics.ColliderShape
import com.dropbear.physics.Physics

// doiiiinnnnnggggg
class SpringyCameraController {
    private var currentDistance: Double = 5.0
    private val margin = 0.3 // Increased slightly for lens clearance
    private val sphereRadius = 0.2 // The "thickness" of your camera

    fun getSpringyPosition(
        playerPos: Vector3d,
        targetPos: Vector3d,
        deltaTime: Double
    ): Vector3d {
        val vectorToCam = targetPos - playerPos
        val maxDist = vectorToCam.length()
        val dir = vectorToCam.normalize()

        val hit = Physics.shapeCast(
            origin = playerPos,
            shape = ColliderShape.Sphere(sphereRadius.toFloat()),
            direction = dir,
            maxDistance = maxDist,
            solid = false
        )

        val targetDist = if (hit != null) {
            (hit.distance - margin).coerceAtLeast(0.1)
        } else {
            maxDist
        }

        if (targetDist < currentDistance) {
            currentDistance = targetDist
        } else {
            // Smoothly glide OUT
            val returnSpeed = 5.0
            currentDistance += (targetDist - currentDistance) * (returnSpeed * deltaTime).coerceIn(0.0, 1.0)
        }

        return playerPos + (dir * currentDistance)
    }
}