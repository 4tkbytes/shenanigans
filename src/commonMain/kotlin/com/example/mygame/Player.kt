package com.example.mygame

import com.dropbear.DropbearEngine
import com.dropbear.Runnable
import com.dropbear.System
import com.dropbear.input.KeyCode
import com.dropbear.logging.Logger
import com.dropbear.math.Quaternion
import com.dropbear.math.Vector3D
import com.dropbear.math.degreesToRadians
import com.dropbear.math.normalizeAngle
import kotlin.math.cos
import kotlin.math.sin

@Runnable(["player"])
class Player: System() {
    private var lastModelPosition = Vector3D.zero()
    private var isMoving = false
    private val rotationDefault = Vector3D.zero()
    private var locked = true

    override fun load(engine: DropbearEngine) {
        Logger.info("Initialised Player")
    }

    override fun update(engine: DropbearEngine, deltaTime: Float) {
        engine.callExceptionOnError(true)

        val entity = this.currentEntity ?: throw Exception("Player entity not found")
        val input = engine.getInputState()
        val scene = engine.getSceneManager()

        val speed = (entity.getProperty<Double>("speed") ?: throw Exception("Player speed not set")) * deltaTime
//        val speed = 100.0 * deltaTime
        val thirdPersonDistance = entity.getProperty<Double>("distance") ?: throw Exception("Failed to get third person distance")
        val heightOffset = entity.getProperty<Double>("heightOffset") ?: throw Exception("Failed to get heightOffset")
        val cameraOffset = Vector3D(0.0, heightOffset, 0.0)

        val transform = entity.getTransform() ?: return
        val camera = entity.getAttachedCamera() ?: return

        if (locked) {
            input.setCursorLocked(true)
            input.setCursorHidden(true)
        } else {
            input.setCursorLocked(false)
            input.setCursorHidden(false)
        }

        isMoving = false

        val forward = Vector3D(cos(camera.yaw), 0.0, sin(camera.yaw))
        val right = Vector3D(-sin(camera.yaw), 0.0, cos(camera.yaw))
        val up = Vector3D(0.0, 1.0, 0.0)
        var movement = Vector3D.zero()

        if (input.isKeyPressed(KeyCode.KeyW)) {
            movement += forward
        }
        if (input.isKeyPressed(KeyCode.KeyS)) {
            movement -= forward
        }
        if (input.isKeyPressed(KeyCode.KeyA)) {
            movement += right
        }
        if (input.isKeyPressed(KeyCode.KeyD)) {
            movement -= right
        }
        if (input.isKeyPressed(KeyCode.Space)) {
            movement += up
        }
        if (input.isKeyPressed(KeyCode.ShiftLeft)) {
            movement -= up
        }
        if (input.isKeyPressed(KeyCode.Escape)) {
            engine.quit()
        }
        if (input.isKeyPressed(KeyCode.F1)) {
            locked = !locked
        }

        if (input.isKeyPressed(KeyCode.F2)) {
            println("F2 pressed")
            scene.switchToSceneImmediate("Default")
        }

        if (movement.length() > 0.0) {
            movement.normalize()
            val displacement = movement * speed
            transform.world.position += displacement
            isMoving = true
        } else {
            isMoving = false
        }

        entity.setTransform(transform)

        val delta = input.getMouseDelta()
        val xOffset = if (locked) delta.x * camera.sensitivity else 0.0
        val yOffset = if (locked) delta.y * camera.sensitivity else 0.0

        camera.yaw += xOffset
        camera.pitch += yOffset

        camera.pitch = camera.pitch.coerceIn(-1.5533, 1.5533)

        val front = Vector3D(
            cos(camera.yaw) * cos(camera.pitch),
            sin(camera.pitch),
            sin(camera.yaw) * cos(camera.pitch)
        ).normalize()

        camera.eye = transform.world.position - (front * thirdPersonDistance) + cameraOffset
        camera.target = transform.world.position + cameraOffset

        if (transform.world.position != lastModelPosition && isMoving) {
            transform.world.rotation = Quaternion.fromEulerAngles(
                rotationDefault.x,
                -camera.yaw,
                rotationDefault.z
            )

            entity.setTransform(transform)
            lastModelPosition = transform.world.position.copy()
        }

        camera.setCamera()

        entity.setProperty("locked", locked)
    }
}