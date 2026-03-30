package com.example.mygame.player

import com.dropbear.components.Camera
import com.dropbear.components.EntityTransform
import com.dropbear.components.camera.SpringyCameraController
import com.dropbear.input.InputState
import com.dropbear.math.Quaterniond
import com.dropbear.math.Vector3d
import com.dropbear.physics.KinematicCharacterController
import com.example.mygame.CameraMode
import com.example.mygame.Global
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

class PlayerCamera {

    private val springCamera = SpringyCameraController()

    fun update(
        transform: EntityTransform,
        camera: Camera,
        inputState: InputState,
        playerInput: PlayerInput,
        deltaTime: Double,
        thirdPersonDistance: Double,
        cameraOffset: Vector3d,
        kcc: KinematicCharacterController,
        locked: Boolean,
        isMoving: Boolean,
        movementDir: Vector3d,
    ) {
        if (Global.cameraMode == CameraMode.OnRails) {

        } else if (Global.cameraMode == CameraMode.ThirdPerson) {
            val invert = false

            val delta   = inputState.getMouseDelta()
            var xOffset = if (locked) delta.x * camera.sensitivity else 0.0
            var yOffset = (if (invert) -1 else 1) * (if (locked) delta.y * camera.sensitivity else 0.0)

            playerInput.player1?.let { gamepad ->
                val deadzone            = 0.15
                val gamepadSensitivity  = 3.0
                if (abs(gamepad.rightStickPosition.x) > deadzone) xOffset += gamepad.rightStickPosition.x * gamepadSensitivity * deltaTime
                if (abs(gamepad.rightStickPosition.y) > deadzone) yOffset -= gamepad.rightStickPosition.y * gamepadSensitivity * deltaTime
            }

            camera.yaw   -= xOffset
            camera.pitch -= yOffset
            camera.pitch  = camera.pitch.coerceIn(-1.5533, 1.5533)

            val front = Vector3d(
                cos(camera.yaw) * cos(camera.pitch),
                sin(camera.pitch),
                sin(camera.yaw) * cos(camera.pitch)
            ).normalize()

            val idealCameraPos = transform.world.position - (front * thirdPersonDistance) + cameraOffset
            val headPos        = transform.world.position + cameraOffset

            camera.eye    = springCamera.getSpringyPosition(headPos, idealCameraPos, deltaTime)
            camera.target = headPos

            if (isMoving && movementDir.lengthSquared() > 0.001) {
                val targetRotation = Quaterniond.fromEulerAngles(0.0, -camera.yaw + PI / 2, 0.0)
                kcc.setRotation(targetRotation)
            }
        }
    }
}