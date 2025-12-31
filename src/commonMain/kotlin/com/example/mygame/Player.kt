package com.example.mygame

import com.dropbear.DropbearEngine
import com.dropbear.Runnable
import com.dropbear.components.Camera
import com.dropbear.components.CustomProperties
import com.dropbear.components.EntityTransform
import com.dropbear.ecs.System
import com.dropbear.input.Gamepad
import com.dropbear.input.GamepadButton
import com.dropbear.input.KeyCode
import com.dropbear.logging.Logger
import com.dropbear.math.Quaternion
import com.dropbear.math.Quaterniond
import com.dropbear.math.Vector3d
import com.dropbear.physics.AxisLock
import com.dropbear.physics.Collider
import com.dropbear.physics.ColliderGroup
import com.dropbear.physics.ColliderShape
import com.dropbear.physics.RigidBody
import com.dropbear.scene.SceneLoadHandle
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

@Runnable(["player"])
class Player: System() {
    private var lastModelPosition = Vector3d.zero()
    private var isMoving = false
    private val rotationDefault = Vector3d.zero()
    private var locked = true
    private var sceneLoadingHandle: SceneLoadHandle? = null
    private var oldGamepads: List<Gamepad> = emptyList()

    private var player1: Gamepad? = null

    private var someIncrementingVariable: Int = 0
    private var jumpForce: Double = 150.0

    override fun load(engine: DropbearEngine) {
        Logger.info("Initialised Player")
        Logger.info("variable at mod init: $someIncrementingVariable")
    }

    override fun update(engine: DropbearEngine, deltaTime: Float) {
        someIncrementingVariable+=1

        val entity = this.currentEntity ?: throw Exception("Player entity not found")
        val input = engine.inputState
        val scene = engine.sceneManager

        val props = entity.getComponent(CustomProperties) ?: throw Exception("Player is required to have a CustomProperties type")

        val speed = (props.getProperty<Double>("speed") ?: throw Exception("Player speed not set"))
        val thirdPersonDistance = props.getProperty<Double>("distance") ?: throw Exception("Failed to get third person distance")
        val heightOffset = props.getProperty<Double>("heightOffset") ?: throw Exception("Failed to get heightOffset")
        val cameraOffset = Vector3d(0.0, heightOffset, 0.0)

        val gamepads = input.getConnectedGamepads()

        val oldIds = oldGamepads.map { it.id }.toSet()
        val newIds = gamepads.map { it.id }.toSet()

        val connected = newIds - oldIds
        val disconnected = oldIds - newIds

        connected.forEach { id ->
            Logger.info("Gamepad connected: ID=$id")
        }

        disconnected.forEach { id ->
            Logger.info("Gamepad disconnected: ID=$id")
            if (player1?.id == id) {
                player1 = null // unset player1
            }
        }

        oldGamepads = gamepads

        if (player1 == null) {
            player1 = gamepads.getOrNull(0) // set player1 to first controller
        } else {
            gamepads.find { it.id == player1?.id }?.let { player1 = it } // update state (for positioning)
        }

        val transform = entity.getComponent(EntityTransform) ?: return
        val camera = entity.getComponent(Camera) ?: return
        val rigidbody = entity.getComponent(RigidBody) ?: return

        if (locked) {
            input.setCursorLocked(true)
            input.setCursorHidden(true)
        } else {
            input.setCursorLocked(false)
            input.setCursorHidden(false)
        }

        isMoving = false

        val forward = Vector3d(cos(camera.yaw), 0.0, sin(camera.yaw))
        val right = Vector3d(-sin(camera.yaw), 0.0, cos(camera.yaw))
        var movement = Vector3d.zero()

        // Keyboard movement
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
        if (input.isKeyPressed(KeyCode.Space) || player1?.isButtonPressed(GamepadButton.South) == true) {
            rigidbody.applyImpulse(Vector3d(0.0, jumpForce, 0.0))
        }

        // gamepad movement
        player1?.let { gamepad ->
            val deadzone = 0.15

            if (abs(gamepad.leftStickPosition.x) > deadzone) {
                movement -= right * gamepad.leftStickPosition.x
            }

            if (abs(gamepad.leftStickPosition.y) > deadzone) {
                movement += forward * gamepad.leftStickPosition.y
            }
        }

        if (input.isKeyPressed(KeyCode.Escape)) {
            engine.quit()
        }
        if (input.isKeyPressed(KeyCode.F1)) {
            locked = !locked
        }

        if (input.isKeyPressed(KeyCode.F2) || player1?.isButtonPressed(GamepadButton.Start) == true) {
            Logger.info("Scene switch requested")
            sceneLoadingHandle = scene.loadSceneAsync("Default")
        }

        sceneLoadingHandle?.let { load ->
            val progress = load.progress()
            Logger.info("\"Default\" scene loading progress: ${progress.percentage()}%, current message: ${progress.message}")
            if (load.isComplete()) {
                load.switchTo()
            } else if (load.hasFailed()) {
                Logger.info("Error while loading \"Default\": ${progress.message}")
                sceneLoadingHandle = null
            }
        }

        val targetVelocity = if (movement.length() > 0.0) {
            movement.normalize()
            movement * speed
        } else {
            Vector3d.zero()
        }

        val currentVel = rigidbody.linearVelocity

        rigidbody.linearVelocity = Vector3d(
            targetVelocity.x,
            currentVel.y,
            targetVelocity.z
        )

        isMoving = movement.length() > 0.0

        val floor = engine.getEntity("floor")

        floor?.let { floor ->
            val group = floor.getComponent(ColliderGroup)
            group?.getColliders()?.forEach { col ->
                when (val shape = col.colliderShape) {
                    is ColliderShape.Box -> Logger.info("Hitbox of box floor: ${shape.halfExtents}")
                    is ColliderShape.Capsule -> Logger.info("Hitbox of capsule of floor: ${shape.halfHeight}, r=${shape.radius}")
                    is ColliderShape.Cone -> Logger.info("Hitbox of cone of floor: ${shape.halfHeight}, r=${shape.radius}")
                    is ColliderShape.Cylinder -> Logger.info("Hitbox of cylinder of floor: ${shape.halfHeight}, r=${shape.radius}")
                    is ColliderShape.Sphere -> Logger.info("Hitbox of sphere: r=${shape.radius}")
                }
            }
        }

        val delta = input.getMouseDelta()
        var xOffset = if (locked) delta.x * camera.sensitivity else 0.0
        var yOffset = if (locked) delta.y * camera.sensitivity else 0.0

        // gamepad camera movement
        player1?.let { gamepad ->
            val deadzone = 0.15
            val gamepadSensitivity = 3.0

            if (kotlin.math.abs(gamepad.rightStickPosition.x) > deadzone) {
                xOffset += gamepad.rightStickPosition.x * gamepadSensitivity * deltaTime
            }

            if (kotlin.math.abs(gamepad.rightStickPosition.y) > deadzone) {
                yOffset -= gamepad.rightStickPosition.y * gamepadSensitivity * deltaTime
            }
        }

        camera.yaw += xOffset
        camera.pitch += yOffset

        camera.pitch = camera.pitch.coerceIn(-1.5533, 1.5533)

        val front = Vector3d(
            cos(camera.yaw) * cos(camera.pitch),
            sin(camera.pitch),
            sin(camera.yaw) * cos(camera.pitch)
        ).normalize()

        camera.eye = transform.world.position - (front * thirdPersonDistance) + cameraOffset
        camera.target = transform.world.position + cameraOffset

        if (transform.world.position != lastModelPosition && isMoving) {
            transform.world.rotation = Quaterniond.fromEulerAngles(
                rotationDefault.x,
                -camera.yaw,
                rotationDefault.z
            )

            lastModelPosition = transform.world.position
        }

        props.setProperty("locked", locked)
    }

    override fun destroy(engine: DropbearEngine) {
        Logger.info("This class is being destroyed :(    Goodbye!")
        Logger.info("variable at mod destroy: $someIncrementingVariable")

        sceneLoadingHandle = null
    }
}