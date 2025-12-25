package com.example.mygame

import com.dropbear.DropbearEngine
import com.dropbear.Runnable
import com.dropbear.System
import com.dropbear.input.Gamepad
import com.dropbear.input.GamepadButton
import com.dropbear.input.KeyCode
import com.dropbear.logging.Logger
import com.dropbear.math.Quaternion
import com.dropbear.math.Vector3D
import com.dropbear.scene.SceneLoadHandle
import kotlin.math.cos
import kotlin.math.sin

@Runnable(["player"])
class Player: System() {
    private var lastModelPosition = Vector3D.zero()
    private var isMoving = false
    private val rotationDefault = Vector3D.zero()
    private var locked = true
    private var sceneLoadingHandle: SceneLoadHandle? = null
    private var oldGamepads: List<Gamepad> = emptyList()

    private var player1: Gamepad? = null

    private var some_incrementing_variable: Int = 0

    override fun load(engine: DropbearEngine) {
        Logger.info("Initialised Player")
        Logger.info("variable at mod init: $some_incrementing_variable")
    }

    override fun update(engine: DropbearEngine, deltaTime: Float) {
        engine.callExceptionOnError(true)

        val entity = this.currentEntity ?: throw Exception("Player entity not found")
        val input = engine.getInputState()
        val scene = engine.getSceneManager()

        val speed = (entity.getProperty<Double>("speed") ?: throw Exception("Player speed not set")) * deltaTime
        val thirdPersonDistance = entity.getProperty<Double>("distance") ?: throw Exception("Failed to get third person distance")
        val heightOffset = entity.getProperty<Double>("heightOffset") ?: throw Exception("Failed to get heightOffset")
        val cameraOffset = Vector3D(0.0, heightOffset, 0.0)

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
            movement += up
        }
        if (input.isKeyPressed(KeyCode.ShiftLeft) || player1?.isButtonPressed(GamepadButton.LeftTrigger2) == true) {
            movement -= up
        }

        // gamepad movement
        player1?.let { gamepad ->
            val deadzone = 0.15

            if (kotlin.math.abs(gamepad.leftStickPosition.x) > deadzone) {
                movement -= right * gamepad.leftStickPosition.x
            }

            if (kotlin.math.abs(gamepad.leftStickPosition.y) > deadzone) {
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

    override fun physicsUpdate(engine: DropbearEngine, deltaTime: Float) {
        some_incrementing_variable+=1
    }

    override fun destroy(engine: DropbearEngine) {
        Logger.info("This class is being destroyed :(    Goodbye!")
        Logger.info("variable at mod destroy: $some_incrementing_variable")

        sceneLoadingHandle = null
    }
}