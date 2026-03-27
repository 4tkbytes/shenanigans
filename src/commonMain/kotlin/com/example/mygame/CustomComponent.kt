package com.example.mygame

import com.dropbear.DropbearEngine
import com.dropbear.ecs.EcsComponent
import com.dropbear.ecs.NativeComponent
import com.dropbear.logging.Logger
import com.dropbear.ui.UIInstructionSet

@EcsComponent
class CustomComponent: NativeComponent(
    fullyQualifiedTypeName = "com.example.mygame.CustomComponent",
    typeName = "CustomComponent",
) {
    override fun inspect(engine: DropbearEngine): UIInstructionSet? {
        return null
    }

    override fun updateComponent(engine: DropbearEngine, deltaTime: Double) {
        Logger.info("CustomComponent update")
        Logger.info("Current entity: $currentEntity")
    }
}