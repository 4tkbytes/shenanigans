package com.example.mygame

import com.dropbear.DropbearEngine
import com.dropbear.Runnable
import com.dropbear.System
import com.dropbear.input.KeyCode

@Runnable(["scene"])
class SceneSwitcherScript: System() {
    override fun update(engine: DropbearEngine, deltaTime: Float) {
        val input = engine.getInputState()
        val scene = engine.getSceneManager()

        if (input.isKeyPressed(KeyCode.F3)) {
            println("F3 pressed!")
            scene.switchToSceneImmediate("map")
        }
    }
}