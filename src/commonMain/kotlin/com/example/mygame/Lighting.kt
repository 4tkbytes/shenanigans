package com.example.mygame

import com.dropbear.DropbearEngine
import com.dropbear.Runnable
import com.dropbear.ecs.System
import com.dropbear.lighting.Light
import com.dropbear.physics.ColliderGroup
import com.dropbear.physics.CollisionEvent
import com.dropbear.physics.ContactForceEvent
import com.dropbear.utils.Colour
import kotlin.random.Random

@Runnable("lighting")
class Lighting: System() {
    override fun load(engine: DropbearEngine) {
       currentEntity ?: throw Exception("There should be a current entity that exists. huh?")
    }

    override fun collisionEvent(engine: DropbearEngine, collisionEvent: CollisionEvent) {
        val player = engine.getEntity("elgato") ?: throw Exception("No player entity found")
        val collider = player.getComponent(ColliderGroup)?.getColliders() ?: throw Exception("No collider group found")

        val targetLight = engine.getEntity("Default Light") ?: throw Exception("Cannot find light")

        if (collisionEvent.started() && collisionEvent.includes(collider)) {
            val light = targetLight.getComponent(Light) ?: throw Exception("Unable to find light")
            light.enabled = !light.enabled
        }

        if (collisionEvent.stopped() && collisionEvent.includes(collider)) {
            val light = targetLight.getComponent(Light) ?: throw Exception("Unable to find light")
            light.colour = Colour(
                r = 255u,
                g = 255u,
                b = 255u,
                a = 255u,
            )
        }
    }

    override fun collisionForceEvent(engine: DropbearEngine, collisionForceEvent: ContactForceEvent) {
        val player = engine.getEntity("elgato") ?: throw Exception("No player entity found")
        val collider = player.getComponent(ColliderGroup)?.getColliders() ?: throw Exception("No collider group found")

        val targetLight = engine.getEntity("Default Light") ?: throw Exception("Cannot find light")

        if (collisionForceEvent.includes(collider)) {
            val light = targetLight.getComponent(Light) ?: throw Exception("Unable to find light")
            light.colour = Colour(
                r = Random.nextInt().toUByte(),
                g = Random.nextInt().toUByte(),
                b = Random.nextInt().toUByte(),
                a = Random.nextInt().toUByte(),
            )
        }
    }
}