package com.example.mygame

import com.dropbear.DropbearEngine
import com.dropbear.Runnable
import com.dropbear.System
import com.dropbear.math.Vector3
import kotlinx.datetime.Clock
import kotlin.math.sin

@Runnable(["scan"])
class Scan: System() {
    override fun load(engine: DropbearEngine) {
    }

    override fun update(engine: DropbearEngine, deltaTime: Float) {
//        engine.callExceptionOnError(true)
        val entity = currentEntity ?: return
        val transform = entity.getTransform() ?: return
        val now = Clock.System.now()
        transform.position =
            Vector3(transform.position.x, sin(now.toEpochMilliseconds().toDouble()) * 100, transform.position.z)
        entity.setTransform(transform)
    }
}