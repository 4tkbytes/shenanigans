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
import com.dropbear.math.Quaterniond
import com.dropbear.math.Vector3d
import com.dropbear.physics.AxisLock
import com.dropbear.physics.CollisionEvent
import com.dropbear.physics.KinematicCharacterController
import com.dropbear.physics.Physics
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
    private var movement: Vector3d = Vector3d.zero()

    private var verticalVelocity = 0.0

    override fun load(engine: DropbearEngine) {
        Logger.info("Initialised Player")
        Logger.info("variable at mod init: $someIncrementingVariable")

        val entity = currentEntity.let {
            Logger.info("Current entity is $it")
            it
        }

        val rb = entity?.getComponent(RigidBody)
        rb?.gravityScale.let { gs ->
            Logger.info("Gravity scale (for entity) is $gs")
        }

        Logger.info("Current global gravity is ${Physics.gravity}")
    }

    override fun physicsUpdate(engine: DropbearEngine, deltaTime: Double) {
        val entity = this.currentEntity ?: throw Exception("Player entity not found")
        val camera = entity.getComponent(Camera) ?: return
        val rigidbody = entity.getComponent(RigidBody) ?: return
        val props = entity.getComponent(CustomProperties) ?: throw Exception("Props missing")
        val speed = (props.getProperty<Double>("speed") ?: 10.0)
        val jumpHeight = (props.getProperty<Double>("jumpHeight") ?: 2.0)
        val input = engine.inputState
        val kcc = entity.getComponent(KinematicCharacterController) ?: throw Exception("Expected KCC component")

        val isGrounded = kcc.isOnFloor()

        val forward = Vector3d(cos(camera.yaw), 0.0, sin(camera.yaw))
        val right = Vector3d(-sin(camera.yaw), 0.0, cos(camera.yaw))
        var movement = Vector3d.zero()

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

        player1?.let { gamepad ->
            val deadzone = 0.15
            if (abs(gamepad.leftStickPosition.x) > deadzone) movement -= right * gamepad.leftStickPosition.x
            if (abs(gamepad.leftStickPosition.y) > deadzone) movement += forward * gamepad.leftStickPosition.y
        }

        val gravity = abs(Physics.gravity.y * rigidbody.gravityScale)

        if ((input.isKeyPressed(KeyCode.Space) || player1?.isButtonPressed(GamepadButton.South) == true) && isGrounded) {
            verticalVelocity = jumpHeight
        }

        verticalVelocity -= gravity * deltaTime

        val dir = if (movement.lengthSquared() > 1e-9) movement.normalize() else Vector3d.zero()
        val velocity = dir * speed

        val translation = Vector3d(
            velocity.x,
            verticalVelocity,
            velocity.z
        )

        entity.getComponent(KinematicCharacterController)?.move(deltaTime, translation)

        isMoving = movement.length() > 0.0
        this.movement = movement
    }

    override fun update(engine: DropbearEngine, deltaTime: Double) {
        someIncrementingVariable += 1

        val entity = this.currentEntity ?: throw Exception("Player entity not found")
        val input = engine.inputState
        val scene = engine.sceneManager
        val props = entity.getComponent(CustomProperties) ?: throw Exception("Props missing")
        val transform = entity.getComponent(EntityTransform) ?: return
        val camera = entity.getComponent(Camera) ?: return

        val thirdPersonDistance = props.getProperty<Double>("distance") ?: 5.0
        val heightOffset = props.getProperty<Double>("heightOffset") ?: 1.0
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
            player1 = gamepads.getOrNull(0)
        } else {
            gamepads.find { it.id == player1?.id }?.let { player1 = it }
        }

        if (locked) {
            input.setCursorLocked(true)
            input.setCursorHidden(true)
        } else {
            input.setCursorLocked(false)
            input.setCursorHidden(false)
        }

        // movement

        // camera stuff
        val delta = input.getMouseDelta()
        var xOffset = if (locked) delta.x * camera.sensitivity else 0.0
        var yOffset = if (locked) delta.y * camera.sensitivity else 0.0

        player1?.let { gamepad ->
            val deadzone = 0.15
            val gamepadSensitivity = 3.0
            if (abs(gamepad.rightStickPosition.x) > deadzone) xOffset += gamepad.rightStickPosition.x * gamepadSensitivity * deltaTime
            if (abs(gamepad.rightStickPosition.y) > deadzone) yOffset -= gamepad.rightStickPosition.y * gamepadSensitivity * deltaTime
        }

        camera.yaw -= xOffset
        camera.pitch += yOffset
        camera.pitch = camera.pitch.coerceIn(-1.5533, 1.5533)

        val front = Vector3d(
            cos(camera.yaw) * cos(camera.pitch),
            sin(camera.pitch),
            sin(camera.yaw) * cos(camera.pitch)
        ).normalize()

        camera.eye = transform.world.position - (front * thirdPersonDistance) + cameraOffset
        camera.target = transform.world.position + cameraOffset

        if (isMoving && movement.lengthSquared() > 0.001) {
            val targetYaw = kotlin.math.atan2(movement.x, movement.z)
            val targetRotation = Quaterniond.fromEulerAngles(rotationDefault.x, targetYaw, rotationDefault.z)
            val rotationSpeed = 10.0
            val t = (rotationSpeed * deltaTime).coerceIn(0.0, 1.0)
            transform.world.rotation = transform.world.rotation.slerp(targetRotation, t)
        }

        lastModelPosition = transform.world.position

        if (input.isKeyPressed(KeyCode.F2) || player1?.isButtonPressed(GamepadButton.Start) == true) {
            Logger.info("Scene switch requested")
            sceneLoadingHandle = scene.loadSceneAsync("Default")
        }

        sceneLoadingHandle?.let { load ->
            if (load.isComplete()) load.switchTo()
            else if (load.hasFailed()) sceneLoadingHandle = null
        }

        if (input.isKeyPressed(KeyCode.Escape)) engine.quit()
        if (input.isKeyPressed(KeyCode.F1)) locked = !locked
        props.setProperty("locked", locked)
    }

    override fun destroy(engine: DropbearEngine) { sceneLoadingHandle = null }
}