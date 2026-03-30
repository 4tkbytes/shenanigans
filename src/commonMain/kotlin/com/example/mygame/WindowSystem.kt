package com.example.mygame

import com.dropbear.DropbearEngine
import com.dropbear.Runnable
import com.dropbear.components.EntityTransform
import com.dropbear.ecs.System
import com.dropbear.logging.Logger
import com.dropbear.math.Transform
import com.dropbear.math.Vector3d
import com.dropbear.physics.CollisionEvent

/**
 * A system that handles objects/colliders that are treated like "windows".
 */
@Runnable("windowSystem")
class WindowSystem : System() {
    var previousPosition = Vector3d.zero()

    override fun collisionEvent(engine: DropbearEngine, collisionEvent: CollisionEvent) {
        if (!collisionEvent.sensor()) return

        val playerEntity = engine.getEntity("player").orLogAndReturn("Player entity not found") { return }
        val playerTransform = playerEntity.getComponent(EntityTransform) ?: return
        val windowTransform = currentEntity?.getComponent(EntityTransform).orLogAndReturn("Window entity not found") { return }

        val playerPos = playerTransform.sync().position
        val windowPos = windowTransform.sync().position

        if (collisionEvent.started()) {
            previousPosition = playerPos
        }

        if (collisionEvent.stopped()) {
            // player position.z is less than window position.z if player is outside
            if (playerPos.z < windowPos.z) {
                Logger.info("Player outside -> room2")
            } else { // otherwise it is inside
                Logger.info("Player inside -> room1")
            }
        }
    }
}

inline fun <T> T?.orLogAndReturn(msg: String, returnBlock: () -> Nothing): T {
    return this ?: run {
        Logger.warn(msg)
        returnBlock()
    }
}