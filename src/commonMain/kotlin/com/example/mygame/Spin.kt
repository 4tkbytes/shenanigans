package com.example.mygame

import com.dropbear.DropbearEngine
import com.dropbear.Runnable
import com.dropbear.System
import com.dropbear.math.Quaternion
import com.dropbear.math.degreesToRadians

@Runnable(["spin"])
class Spin: System() {
    override fun update(engine: DropbearEngine, deltaTime: Float) {
        val entity = this.currentEntity ?: return
        val prop = entity.getProperty<Boolean>("exists") ?: return
        val transform = entity.getTransform() ?: return
        val delta = Quaternion.rotateY(degreesToRadians(3.0))
        transform.rotation = delta * transform.rotation
        entity.setTransform(transform)
    }
}