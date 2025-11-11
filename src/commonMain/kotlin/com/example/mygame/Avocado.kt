package com.example.mygame

import com.dropbear.Runnable
import com.dropbear.System

@Runnable(["avocado"])
class Avocado: System() {
    override fun update(engine: com.dropbear.DropbearEngine, deltaTime: Float) {
//        engine.callExceptionOnError(true)

        val entity = currentEntity ?: return
        val toggleModel = entity.getProperty<Boolean>("toggle_model") ?: return

        if (toggleModel) {
            val asset = engine.getAsset("euca://models/fish.glb") ?: return
            val modelAsset = asset.asModelHandle(engine) ?: return
            entity.setModel(modelAsset)

            val newAsset = entity.getModel() ?: return
            if (newAsset != asset) {
                throw Exception("Fish asset was not updated")
            }
        } else {
            val asset = engine.getAsset("euca://models/Avocado.glb") ?: return
            val modelAsset = asset.asModelHandle(engine) ?: return
            entity.setModel(modelAsset)

            val newAsset = entity.getModel() ?: return
            if (newAsset != asset) {
                throw Exception("Avocado asset was not updated")
            }
        }

        entity.getAllTextures().forEach { println(it) }

        val avocado = engine.getAsset("euca://models/Avocado.glb") ?: return
        if (entity.getModel() == avocado) {
            val tex = entity.getTexture("2256_Avocado_d") ?: return
            val ass = engine.getAsset("euca://models/Avocado.glb/2256_Avocado_d") ?: return
            val avocadoTex = ass.asTextureHandle(engine) ?: return
            if (tex.getName(engine) == "2256_Avocado_d" && tex == avocadoTex) {
                entity.setProperty("texture", true)
            }
        }
    }
}