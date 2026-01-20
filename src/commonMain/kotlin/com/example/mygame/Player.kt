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
import com.dropbear.math.Vector2d
import com.dropbear.math.Vector3d
import com.dropbear.physics.AxisLock
import com.dropbear.physics.CollisionEvent
import com.dropbear.physics.KinematicCharacterController
import com.dropbear.physics.Physics
import com.dropbear.physics.RigidBody
import com.dropbear.scene.SceneLoadHandle
import com.dropbear.ui.Ui
import com.dropbear.ui.primitive.Rectangle
import com.dropbear.utils.Colour
import com.dropbear.utils.ID
import com.dropbear.utils.asId
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.time.Clock
import kotlin.time.Instant

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

    private var gasStart: Instant? = null

    private var qKeyPressedLastFrame = false
    private var eKeyPressedLastFrame = false

    private val springCamera = SpringyCameraController()

    companion object {
        private var previousPlayerState: PlayerState = PlayerState.Solid

        var playerState: PlayerState = PlayerState.Solid
            private set(value) {
                previousPlayerState = playerState
                field = value
            }

        var health: PlayerHealth = PlayerHealth.full()

        var currentGasTime: Double = 0.0
    }

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
        var speed = (props.getProperty<Double>("speed") ?: 10.0) * deltaTime
        val jumpHeight = (props.getProperty<Double>("jumpHeight") ?: 2.0) * deltaTime
        val gasFloatSpeed = (props.getProperty<Double>("gasFloatSpeed") ?: 2.0) * deltaTime
        val input = engine.inputState
        val kcc = entity.getComponent(KinematicCharacterController) ?: throw Exception("Expected KCC component")

        val isGrounded = kcc.isOnFloor()

        if (isGrounded && verticalVelocity < 0.0) {
            verticalVelocity = 0.0
        }

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
        if (input.isKeyPressed(KeyCode.ShiftLeft) || input.isKeyPressed(KeyCode.ShiftRight) || (player1?.isButtonPressed(GamepadButton.West) == true)) {
            speed *= 2
        }

        player1?.let { gamepad ->
            val deadzone = 0.15
            if (abs(gamepad.leftStickPosition.x) > deadzone) movement -= right * gamepad.leftStickPosition.x
            if (abs(gamepad.leftStickPosition.y) > deadzone) movement += forward * gamepad.leftStickPosition.y
        }

        val gravity = abs(Physics.gravity.y * rigidbody.gravityScale)

        if ((input.isKeyPressed(KeyCode.Space) || player1?.isButtonPressed(GamepadButton.South) == true) && isGrounded) {
            verticalVelocity = if (playerState == PlayerState.Gas) {
                -jumpHeight
            } else {
                jumpHeight
            }
        }

        if (playerState == PlayerState.Gas) {
            applyGasPhysics(gasFloatSpeed)
        } else {
            if (!isGrounded) {
                verticalVelocity -= gravity * deltaTime
            }
        }

        val dir = if (movement.lengthSquared() > 1e-9) movement.normalize() else Vector3d.zero()
        val velocity = dir * speed

        val translation = Vector3d(
            velocity.x,
            verticalVelocity,
            velocity.z
        )

        kcc.move(deltaTime, translation)

        if (movement.lengthSquared() > 0.001) {
            val transform = entity.getComponent(EntityTransform) ?: return

            val targetYaw = kotlin.math.atan2(movement.x, movement.z)

            val targetRotation = Quaterniond.fromEulerAngles(0.0, targetYaw, 0.0)

            val rotationSpeed = 10.0
            val t = (rotationSpeed * deltaTime).coerceIn(0.0, 1.0)

            transform.world.rotation = transform.world.rotation.slerp(targetRotation, t)
        }

        this.isMoving = movement.lengthSquared() > 0.001
        this.movement = movement

        // switch down
        val qPressed = input.isKeyPressed(KeyCode.KeyQ) || player1?.isButtonPressed(GamepadButton.LeftTrigger2) == true
        if (qPressed && !qKeyPressedLastFrame) {
            val oldPlayerState = playerState
            playerState = when (playerState) {
                PlayerState.Liquid -> PlayerState.Liquid
                PlayerState.Solid -> PlayerState.Liquid
                PlayerState.Gas -> PlayerState.Solid
            }
            Logger.info("playerState changed: $oldPlayerState -> $playerState")
            switchForm()
        }
        qKeyPressedLastFrame = qPressed

        // switch up
        val ePressed = input.isKeyPressed(KeyCode.KeyE) || player1?.isButtonPressed(GamepadButton.RightTrigger2) == true
        if (ePressed && !eKeyPressedLastFrame) {
            val oldPlayerState = playerState
            playerState = when (playerState) {
                PlayerState.Liquid -> PlayerState.Solid
                PlayerState.Solid -> PlayerState.Gas
                PlayerState.Gas -> PlayerState.Gas
            }
            Logger.info("playerState changed: $oldPlayerState -> $playerState")
            switchForm()
        }
        eKeyPressedLastFrame = ePressed

        renderHUD(engine.ui)
    }

    fun switchForm() {
        // todo: make it so it switches models.

        if (playerState == PlayerState.Gas) {
            // start gas timer
            gasStart = Clock.System.now()
            currentGasTime = 0.0
        } else {
            // gas timer resets if not gas
            gasStart = null
            currentGasTime = 0.0
        }
    }

    fun renderHUD(ui: Ui) {
        when (playerState) {
            PlayerState.Liquid -> {
                if (ui.add(Rectangle(
                    id = ID.fromString("liquid"),
                    initial = Vector2d(10.0, 10.0),
                    width = 50.0,
                    height = 20.0,
                    fillColour = Colour(255u, 0u, 0u, 255u)
                )).clicked()) {
                    ui.add(Rectangle(
                        id = ID.fromString("liquid"),
                        initial = Vector2d(10.0, 10.0),
                        width = 50.0,
                        height = 20.0,
                        fillColour = Colour.WHITE
                    ))
                }
            }
            PlayerState.Solid -> {
                if (ui.add(Rectangle(
                    id = ID.fromString("solid"),
                    initial = Vector2d(10.0, 10.0),
                    width = 50.0,
                    height = 20.0,
                    fillColour = Colour(128u, 128u, 128u, 255u)
                )).clicked()) {
                    ui.add(Rectangle(
                        id = ID.fromString("solid"),
                        initial = Vector2d(10.0, 10.0),
                        width = 50.0,
                        height = 20.0,
                        fillColour = Colour.WHITE
                    ))
                }
            }
            PlayerState.Gas -> {
                if (ui.add(Rectangle(
                        id = ID.fromString("gas"),
                        initial = Vector2d(10.0, 10.0),
                        width = 50.0,
                        height = 20.0,
                        fillColour = Colour(0u, 0u, 255u, 255u)
                    )).clicked()) {
                    ui.add(Rectangle(
                        id = ID.fromString("gas"),
                        initial = Vector2d(10.0, 10.0),
                        width = 50.0,
                        height = 20.0,
                        fillColour = Colour.WHITE
                    ))
                }
            }
        }
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

        // camera stuff
        val delta = input.getMouseDelta()
        var xOffset = if (locked) delta.x * camera.sensitivity else 0.0
        var yOffset = - (if (locked) delta.y * camera.sensitivity else 0.0)

        player1?.let { gamepad ->
            val deadzone = 0.15
            val gamepadSensitivity = 3.0
            if (abs(gamepad.rightStickPosition.x) > deadzone) xOffset += gamepad.rightStickPosition.x * gamepadSensitivity * deltaTime
            if (abs(gamepad.rightStickPosition.y) > deadzone) yOffset -= gamepad.rightStickPosition.y * gamepadSensitivity * deltaTime
        }

        camera.yaw -= xOffset
        camera.pitch -= yOffset
        camera.pitch = camera.pitch.coerceIn(-1.5533, 1.5533)

        val front = Vector3d(
            cos(camera.yaw) * cos(camera.pitch),
            sin(camera.pitch),
            sin(camera.yaw) * cos(camera.pitch)
        ).normalize()

        val idealCameraPos = transform.world.position - (front * thirdPersonDistance) + cameraOffset
        val headPos = transform.world.position + cameraOffset

        camera.eye = springCamera.getSpringyPosition(headPos, idealCameraPos, deltaTime)
        camera.target = headPos

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

    fun applyGasPhysics(gasFloatSpeed: Double) {
        verticalVelocity = gasFloatSpeed

        val startTime = gasStart ?: return
        val maxGasDuration = 5.0 // 5 seconds

        val elapsed = (Clock.System.now() - startTime).inWholeMilliseconds / 1000.0
        currentGasTime = elapsed

        val energyPercentage = (1.0 - (currentGasTime / maxGasDuration)).coerceIn(0.0, 1.0)

        val maxEnergy = 100.0
        health.energy.current = maxEnergy * energyPercentage

        if (currentGasTime >= maxGasDuration || health.energy.current <= 0.0) {
            health.energy.current = 0.0
            playerState = PlayerState.Solid
            switchForm()
            Logger.info("timer expired")
        }
    }
}