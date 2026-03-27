package com.example.mygame

import com.dropbear.DropbearEngine
import com.dropbear.Runnable
import com.dropbear.animation.AnimationComponent
import com.dropbear.components.Camera
import com.dropbear.components.CustomProperties
import com.dropbear.components.EntityTransform
import com.dropbear.ecs.System
import com.dropbear.logging.Logger
import com.dropbear.math.Vector3d
import com.dropbear.physics.KinematicCharacterController
import com.dropbear.physics.Physics
import com.dropbear.physics.RigidBody
import com.dropbear.scene.SceneLoadHandle
import com.example.mygame.player.PlayerCamera
import com.example.mygame.player.PlayerHealthController
import com.example.mygame.player.PlayerInput
import com.example.mygame.player.PlayerMovement
import com.example.mygame.player.PlayerStateController
import kotlin.math.abs

@Runnable("player")
class Player : System() {

    private val playerInput = PlayerInput()
    private val movement    = PlayerMovement()
    private val stateCtrl   = PlayerStateController()
    private val camera      = PlayerCamera()
    private val healthCtrl  = PlayerHealthController()

    private var locked            = true
    private var toggleDebug       = false
    private var currentAnimation: String? = null
    private var lastModelPosition = Vector3d.zero()
    private var sceneLoadingHandle: SceneLoadHandle? = null

    // required entity properties:
    // - speed: Double
    // - jumpHeight: Double
    // - gasFloatSpeed: Double
    // - distance: Double (third-person camera distance)
    // - heightOffset: Double

    override fun load(engine: DropbearEngine) {
        Logger.info("Initialised Player")

        val entity = currentEntity ?: return
        Logger.info("Current entity: $entity")

        entity.getComponent(RigidBody)?.let { rb ->
            Logger.info("Gravity scale: ${rb.gravityScale}")
        }

        Logger.info("Global gravity: ${Physics.gravity}")

        val animation = entity.getComponent(AnimationComponent) ?: throw Exception("AnimationComponent missing")
        animation.setAnimation("Idle")
    }

    override fun physicsUpdate(engine: DropbearEngine, deltaTime: Double) {
        val entity    = currentEntity                                            ?: throw Exception("Player entity not found")
        val playerCam = entity.getChildByLabel("player camera")                  ?: throw Exception("Player camera child missing")
        val cam       = playerCam.getComponent(Camera)                    ?: throw Exception("Camera missing")
        val kcc       = entity.getComponent(KinematicCharacterController) ?: throw Exception("KCC component missing")
        val rigidbody = entity.getComponent(RigidBody)                    ?: throw Exception("Rigidbody missing")
        val props     = entity.getComponent(CustomProperties)             ?: throw Exception("CustomProperties missing")
        val animation = entity.getComponent(AnimationComponent)           ?: throw Exception("AnimationComponent missing")
        val transform = entity.getComponent(EntityTransform)              ?: throw Exception("EntityTransform missing")

        val speed               = (props.getProperty<Double>("speed")            ?: 10.0) * deltaTime
        val jumpHeight          = (props.getProperty<Double>("jumpHeight")       ?: 2.0)  * deltaTime
        val gasFloatSpeed       = (props.getProperty<Double>("gasFloatSpeed")    ?: 2.0)  * deltaTime
        val thirdPersonDistance =  props.getProperty<Double>("distance")         ?: 5.0
        val heightOffset        =  props.getProperty<Double>("heightOffset")     ?: 1.0

        val inputState = engine.inputState
        val gravity    = abs(Physics.gravity.y * rigidbody.gravityScale)

        playerInput.update(inputState, cam.yaw)

        stateCtrl.update(playerInput, healthCtrl)

        if (playerInput.isSprinting) healthCtrl.drainSprintEnergy()

        if (stateCtrl.state == PlayerState.Gas) {
            movement.verticalVelocity = stateCtrl.applyGasPhysics(gasFloatSpeed, healthCtrl)
        }

        movement.update(
            input      = playerInput,
            state      = stateCtrl.state,
            isGrounded = kcc.isGrounded(),
            speed      = speed,
            jumpHeight = jumpHeight,
            gravity    = gravity,
            deltaTime  = deltaTime,
            kcc        = kcc,
            transform  = transform,
        )

        if (playerInput.healPressed && healthCtrl.canSiphon()) {
            healthCtrl.siphon()
        }

        val isGrounded       = kcc.isGrounded()
        val desiredAnimation = when {
            !isGrounded       -> PlayerAnimationState.Jumping
            movement.isMoving -> PlayerAnimationState.Walking
            else              -> PlayerAnimationState.Idle
        }
        if (currentAnimation != desiredAnimation.animationName) {
            animation.reset()
            animation.setAnimation(desiredAnimation.animationName)
            animation.play()
            currentAnimation = desiredAnimation.animationName
        }

        val cameraOffset = Vector3d(0.0, heightOffset, 0.0)
        inputState.setCursorLocked(locked)
        inputState.setCursorHidden(locked)

        camera.update(
            transform           = transform,
            camera              = cam,
            inputState          = inputState,
            playerInput         = playerInput,
            deltaTime           = deltaTime,
            thirdPersonDistance = thirdPersonDistance,
            cameraOffset        = cameraOffset,
            kcc                 = kcc,
            locked              = locked,
            isMoving            = movement.isMoving,
            movementDir         = movement.direction,
        )

        lastModelPosition = transform.world.position

        if (playerInput.sceneSwitch) {
            Logger.info("Scene switch requested")
            sceneLoadingHandle = engine.sceneManager.loadSceneAsync("Default")
        }
        sceneLoadingHandle?.let { handle ->
            when {
                handle.isComplete() -> handle.switchTo()
                handle.hasFailed()  -> sceneLoadingHandle = null
            }
        }

        if (playerInput.toggleDebug) toggleDebug = !toggleDebug
        if (playerInput.toggleLock)  locked      = !locked
        if (playerInput.quit)        engine.quit()
    }

    override fun destroy(engine: DropbearEngine) {
        sceneLoadingHandle = null
    }
}
