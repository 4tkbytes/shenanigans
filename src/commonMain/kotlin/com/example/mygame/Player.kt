package com.example.mygame

import com.dropbear.DropbearEngine
import com.dropbear.Runnable
import com.dropbear.System
import com.dropbear.input.KeyCode
import com.dropbear.logging.Logger
import com.dropbear.math.Vector3
import com.dropbear.math.Vector3D
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Runnable(["player"])
class Player: System() {

    override fun load(engine: DropbearEngine) {
        Logger.info("Initialised Player")
    }

    override fun update(engine: DropbearEngine, deltaTime: Float) {
        val entity = engine.getEntity("Default Cube") ?: return
        val input = engine.getInputState()
        val speed = entity.getProperty<Float>("speed") ?: return

        if (input.isKeyPressed(KeyCode.KeyW)) {
            val transform = entity.getTransform() ?: return
            transform.position.z -= speed
            entity.setTransform(transform)
        }

        if (input.isKeyPressed(KeyCode.KeyS)) {
            val transform = entity.getTransform() ?: return
            transform.position.z += speed
            entity.setTransform(transform)
        }

        if (input.isKeyPressed(KeyCode.KeyA)) {
            val transform = entity.getTransform() ?: return
            transform.position.x += speed
            entity.setTransform(transform)
        }

        if (input.isKeyPressed(KeyCode.KeyD)) {
            val transform = entity.getTransform() ?: return
            transform.position.x -= speed
            entity.setTransform(transform)
        }

        if (input.isKeyPressed(KeyCode.Space)) {
            val transform = entity.getTransform() ?: return
            transform.position.y += speed
            entity.setTransform(transform)
        }

        if (input.isKeyPressed(KeyCode.ShiftLeft)) {
            val transform = entity.getTransform() ?: return
            transform.position.y -= speed
            entity.setTransform(transform)
        }

        // camera stuff
        val camera = entity.getAttachedCamera() ?: return
        val distance = entity.getProperty<Float>("distance") ?: return //distance from camera

        val delta = input.getMouseDelta()
        camera.yaw += delta.x * camera.sensitivity
        camera.pitch += delta.y * camera.sensitivity

        val minPitch = -PI / 2.0 + 0.1
        val maxPitch = PI / 2.0 - 0.1
        camera.pitch = camera.pitch.coerceIn(minPitch, maxPitch)

        val transform = entity.getTransform() ?: return
        val position = transform.position

        val horizontalDistance = distance * cos(camera.pitch)
        val heightOffset = entity.getProperty<Float>("heightOffset") ?: 0.0f
        val cameraOffset = Vector3(
            horizontalDistance * sin(camera.yaw),
            (distance * sin(camera.pitch)) + heightOffset,
            horizontalDistance * sin(camera.yaw)
        )

        camera.eye = camera.target + cameraOffset

        camera.target = position

        camera.setCamera() //pushing camera
    }
}