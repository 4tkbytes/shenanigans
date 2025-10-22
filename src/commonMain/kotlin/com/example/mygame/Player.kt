package com.example.mygame

import com.dropbear.DropbearEngine
import com.dropbear.Runnable
import com.dropbear.System
import com.dropbear.input.KeyCode
import com.dropbear.logging.Logger
import com.dropbear.math.Vector3D
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Runnable(["player"])
class Player: System() {
    private var lastModelPosition = Vector3D.zero()
    private var isMoving = false
    private val rotationDefault = Vector3D.zero()

    private val thirdPersonDistance = 5.0
    private val cameraOffset = Vector3D(0.0, 2.0, 0.0)

    override fun load(engine: DropbearEngine) {
        Logger.info("Initialised Player")
    }

    override fun update(engine: DropbearEngine, deltaTime: Float) {
        val entity = engine.getEntity("Default Cube") ?: return
        val input = engine.getInputState()
        val speed = entity.getProperty<Float>("speed") ?: return
        val transform = entity.getTransform() ?: return
        val camera = entity.getAttachedCamera() ?: return

        val prevPos = transform.position.copy()
        isMoving = false

        if (input.isKeyPressed(KeyCode.KeyW)) {
            transform.position.z -= speed
            isMoving = true
        }

        if (input.isKeyPressed(KeyCode.KeyS)) {
            transform.position.z += speed
            isMoving = true
        }

        if (input.isKeyPressed(KeyCode.KeyA)) {
            transform.position.x += speed
            isMoving = true
        }

        if (input.isKeyPressed(KeyCode.KeyD)) {
            transform.position.x -= speed
            isMoving = true
        }

        if (input.isKeyPressed(KeyCode.Space)) {
            transform.position.y += speed
            isMoving = true
        }

        if (input.isKeyPressed(KeyCode.ShiftLeft)) {
            transform.position.y -= speed
            isMoving = true
        }

        entity.setTransform(transform)

        val delta = input.getMouseDelta()
        val xOffset = delta.x * camera.sensitivity
        val yOffset = delta.y * camera.sensitivity

        camera.yaw += xOffset
        camera.pitch += yOffset

        camera.pitch = camera.pitch.coerceIn(-89.0, 89.0)

        val front = Vector3D(
            cos(degreesToRadians(camera.yaw)) * cos(degreesToRadians(camera.pitch)),
            sin(degreesToRadians(camera.pitch)),
            sin(degreesToRadians(camera.yaw)) * cos(degreesToRadians(camera.pitch))
        ).normalize()

        camera.eye = transform.position - (front * thirdPersonDistance) + cameraOffset
        camera.target = transform.position + cameraOffset

//        if (transform.position != lastModelPosition && isMoving) {
//            val normalizedYaw = normalizeAngle(camera.yaw)
//            val targetYRotation = -degreesToRadians(normalizedYaw)
//
//            transform.rotation = Vector3D(
//                rotationDefault.x,
//                targetYRotation,
//                rotationDefault.z
//            )
//            entity.setTransform(transform)
//
//            lastModelPosition = transform.position.copy()
//        }

        if (prevPos != transform.position) {
            Logger.info("[Player] Player's position changed: $prevPos -> ${transform.position}")
        }


        camera.setCamera() // Push camera
    }
}

fun degreesToRadians(degrees: Double): Double = degrees * PI / 180

fun normalizeAngle(angle: Double): Double {
    var normalized = angle % 360.0
    if (normalized > 180.0) {
        normalized -= 360.0
    } else if (normalized < -180.0) {
        normalized += 360.0
    }
    return normalized
}

fun normalizeRadians(radians: Double): Double {
    var normalized = radians % (2 * PI)
    if (normalized > PI) {
        normalized -= 2 * PI
    } else if (normalized < -PI) {
        normalized += 2 * PI
    }
    return normalized
}

fun Vector3D.copy(): Vector3D = Vector3D(this.x, this.y, this.z)