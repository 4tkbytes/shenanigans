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
    private var isLocked = false

    override fun load(engine: DropbearEngine) {
        Logger.info("Initialised Player")
    }

    override fun update(engine: DropbearEngine, deltaTime: Float) {
        val entity = engine.getEntity("fish") ?: return
        val input = engine.getInputState()
        val speed = entity.getProperty<Float>("speed") ?: return
        val transform = entity.getTransform() ?: return
        val camera = entity.getAttachedCamera() ?: return

        if (input.isKeyPressed(KeyCode.KeyF)) {
            isLocked = !isLocked
        }

        if (isLocked) {
            input.setCursorLocked(true)
            input.setCursorHidden(true)
        } else {
            input.setCursorLocked(false)
            input.setCursorHidden(false)
        }

        isMoving = false

        val yawRadians = degreesToRadians(camera.yaw)
        val forward = Vector3D(cos(yawRadians), 0.0, sin(yawRadians))
        val right = Vector3D(-sin(yawRadians), 0.0, cos(yawRadians))
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

        if (movement.length() > 0.0) {
            movement.normalize()
            val displacement = movement * speed.toDouble()
            transform.position += displacement
            isMoving = true
        } else {
            isMoving = false
        }

        entity.setTransform(transform)

        val delta = input.getMouseDelta()

        val xOffset = delta.x * camera.sensitivity
        val yOffset = delta.y * camera.sensitivity

        camera.yaw += xOffset
        camera.pitch += yOffset

        camera.pitch = camera.pitch.coerceIn(-89.0, 89.0)

        if (isLocked) {
            val front = Vector3D(
                cos(degreesToRadians(camera.yaw)) * cos(degreesToRadians(camera.pitch)),
                sin(degreesToRadians(camera.pitch)),
                sin(degreesToRadians(camera.yaw)) * cos(degreesToRadians(camera.pitch))
            ).normalize()

            val thirdPersonDistance = entity.getProperty<Double>("distance") ?: 0.0
            val cameraOffset = Vector3D(0.0, entity.getProperty("heightOffset") ?: 2.0, 0.0)

            camera.eye = transform.position - (front * thirdPersonDistance) + cameraOffset
            camera.target = transform.position + cameraOffset
        }

        if (transform.position != lastModelPosition && isMoving) {
            val normalizedYaw = normalizeAngle(camera.yaw)
            val targetYRotation = -degreesToRadians(normalizedYaw)

            transform.rotation = Quaternion.fromEulerAngles(
                rotationDefault.x,
                targetYRotation,
                rotationDefault.z
            )
            entity.setTransform(transform)

            lastModelPosition = transform.position.copy()
        }

        camera.setCamera()
    }
}