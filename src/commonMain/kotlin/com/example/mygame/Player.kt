package com.example.mygame

import com.dropbear.DropbearEngine
import com.dropbear.EntityRef
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
import com.dropbear.physics.Collider
import com.dropbear.physics.ColliderGroup
import com.dropbear.physics.CollisionEvent
import com.dropbear.physics.ContactForceEvent
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
    private var jumpForce: Double = 50.0
    private var fallMultiplier: Double = 2.5

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

    override fun physicsUpdate(engine: DropbearEngine, deltaTime: Float) {
//        val entity = this.currentEntity ?: throw Exception("Player entity not found")
//
//        val player = getColliders(entity)
//        val triggerEntity = engine.getEntity("trigger") ?: throw Exception("trigger entity should exist")
//        val triggers = getColliders(triggerEntity)
//
//        if (Physics.triggering(player[0], triggers[0])) {
//            Logger.info("Triggering!")
//        }
//
//        if (Physics.overlapping(player[0], triggers[0])) {
//            Logger.info("Overlapping!")
//        }
//
//        if (Physics.touching(entity, triggerEntity)) {
//            Logger.info("Touching!")
//        }
    }

    fun getColliders(entity: EntityRef): List<Collider> {
        return (entity.getComponent(ColliderGroup) ?: throw Exception("huh?")).getColliders()
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
            gamepads.find { it.id == player1?.id }?.let { player1 = it }
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

        // player movement
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

        val rotLock = AxisLock(
            x = true,
            y = true,
            z = true,
        )

        rigidbody.lockRotation = rotLock

        player1?.let { gamepad ->
            val deadzone = 0.15

            if (abs(gamepad.leftStickPosition.x) > deadzone) {
                movement -= right * gamepad.leftStickPosition.x
            }

            if (abs(gamepad.leftStickPosition.y) > deadzone) {
                movement += forward * gamepad.leftStickPosition.y
            }
        }

        val targetVelocity = if (movement.length() > 0.0) {
            movement.normalize()
            movement * speed
        } else {
            Vector3d.zero()
        }

        val currentVel = rigidbody.linearVelocity
        var newY = currentVel.y

        val groundHit = Physics.raycast(
            origin = transform.sync().position,
            direction = Vector3d(0.0, -1.0, 0.0),
            maxDistance = 10.0,
            solid = false
        )

        val isGrounded = groundHit != null || abs(currentVel.y) < 0.1

        val isJumpReq = input.isKeyPressed(KeyCode.Space) || player1?.isButtonPressed(GamepadButton.South) ?: false

        if (isJumpReq && isGrounded) {
            newY = jumpForce
        }

        if (newY < 0) {
            val extraGravity = Physics.gravity.y * (fallMultiplier - 1.0) * deltaTime
            newY += extraGravity
        }
        else if (newY > 0 && !isJumpReq) {
            val cutJumpGravity = Physics.gravity.y * (2.0 - 1.0) * deltaTime
            newY += cutJumpGravity
        }

        rigidbody.linearVelocity = Vector3d(
            targetVelocity.x,
            newY,
            targetVelocity.z
        )

        isMoving = movement.length() > 0.0

        // camera->rotation
        val delta = input.getMouseDelta()
        var xOffset = if (locked) delta.x * camera.sensitivity else 0.0
        var yOffset = if (locked) delta.y * camera.sensitivity else 0.0

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

            val targetRotation = Quaterniond.fromEulerAngles(
                rotationDefault.x,
                targetYaw,
                rotationDefault.z
            )

            val rotationSpeed = 10.0
            val t = (rotationSpeed * deltaTime).coerceIn(0.0, 1.0)

            transform.world.rotation = transform.world.rotation.slerp(targetRotation, t)
        }

        lastModelPosition = transform.world.position

        // scene loading
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

        // misc
        if (input.isKeyPressed(KeyCode.Escape)) {
            engine.quit()
        }
        if (input.isKeyPressed(KeyCode.F1)) {
            locked = !locked
        }

        props.setProperty("locked", locked)
    }

    override fun destroy(engine: DropbearEngine) {
        Logger.info("This class is being destroyed :(    Goodbye!")
        Logger.info("variable at mod destroy: $someIncrementingVariable")

        sceneLoadingHandle = null
    }

    override fun collisionEvent(engine: DropbearEngine, collisionEvent: CollisionEvent) {
        Logger.info("Collision event triggered: $collisionEvent")
    }

    override fun collisionForceEvent(engine: DropbearEngine, collisionForceEvent: ContactForceEvent) {
        Logger.info("Collision force event triggered: $collisionForceEvent")
    }
}