package com.example.mygame.player

import com.dropbear.input.Gamepad
import com.dropbear.input.GamepadButton
import com.dropbear.input.InputState
import com.dropbear.input.KeyCode
import com.dropbear.logging.Logger
import com.dropbear.math.Vector3d
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

class PlayerInput {

    private var oldGamepads: List<Gamepad> = emptyList()
    var player1: Gamepad? = null
        private set

    var movement: Vector3d = Vector3d.zero()
        private set
    var isSprinting: Boolean = false
        private set
    var jumpPressed: Boolean = false
        private set
    var switchDown: Boolean = false
        private set
    var switchUp: Boolean = false
        private set
    var healPressed: Boolean = false
        private set
    var toggleDebug: Boolean = false
        private set
    var toggleLock: Boolean = false
        private set
    var quit: Boolean = false
        private set
    var sceneSwitch: Boolean = false
        private set

    private var qPressedLastFrame = false
    private var ePressedLastFrame = false

    fun update(inputState: InputState, cameraYaw: Double) {
        updateGamepads(inputState)

        val forward = Vector3d(cos(cameraYaw), 0.0, sin(cameraYaw))
        val right   = Vector3d(-sin(cameraYaw), 0.0, cos(cameraYaw))
        var move    = Vector3d.zero()

        if (inputState.isKeyPressed(KeyCode.KeyW)) move += forward
        if (inputState.isKeyPressed(KeyCode.KeyS)) move -= forward
        if (inputState.isKeyPressed(KeyCode.KeyA)) move += right
        if (inputState.isKeyPressed(KeyCode.KeyD)) move -= right

        player1?.let { gamepad ->
            val deadzone = 0.15
            if (abs(gamepad.leftStickPosition.x) > deadzone) move -= right   * gamepad.leftStickPosition.x
            if (abs(gamepad.leftStickPosition.y) > deadzone) move += forward * gamepad.leftStickPosition.y
        }

        movement    = move
        isSprinting = inputState.isKeyPressed(KeyCode.ShiftLeft)
                   || inputState.isKeyPressed(KeyCode.ShiftRight)
                   || player1?.isButtonPressed(GamepadButton.West) == true
        jumpPressed = inputState.isKeyPressed(KeyCode.Space)  || player1?.isButtonPressed(GamepadButton.South) == true
        healPressed = inputState.isKeyPressed(KeyCode.KeyH)   || player1?.isButtonPressed(GamepadButton.North) == true
        toggleDebug = inputState.isKeyPressed(KeyCode.Backquote)
        toggleLock  = inputState.isKeyPressed(KeyCode.F1)
        quit        = inputState.isKeyPressed(KeyCode.Escape)
        sceneSwitch = inputState.isKeyPressed(KeyCode.F2) || player1?.isButtonPressed(GamepadButton.Start) == true

        val qPressed = inputState.isKeyPressed(KeyCode.KeyQ) || player1?.isButtonPressed(GamepadButton.LeftTrigger2) == true
        switchDown        = qPressed && !qPressedLastFrame
        qPressedLastFrame = qPressed

        val ePressed = inputState.isKeyPressed(KeyCode.KeyE) || player1?.isButtonPressed(GamepadButton.RightTrigger2) == true
        switchUp          = ePressed && !ePressedLastFrame
        ePressedLastFrame = ePressed
    }

    private fun updateGamepads(inputState: InputState) {
        val gamepads = inputState.getConnectedGamepads()
        val oldIds   = oldGamepads.map { it.id }.toSet()
        val newIds   = gamepads.map { it.id }.toSet()

        (newIds - oldIds).forEach { id -> Logger.info("Gamepad connected: ID=$id") }
        (oldIds - newIds).forEach { id ->
            Logger.info("Gamepad disconnected: ID=$id")
            if (player1?.id == id) player1 = null
        }

        oldGamepads = gamepads
        player1 = if (player1 == null) gamepads.getOrNull(0)
                  else gamepads.find { it.id == player1?.id }
    }
}