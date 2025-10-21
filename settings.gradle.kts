pluginManagement {
    repositories {
        mavenLocal()
        gradlePluginPortal()
        maven { url = uri("https://4tkbytes.github.io/dropbear/") }
    }
    resolutionStrategy {
        eachPlugin {
            if (requested.id.id == "magna-carta") {
                useModule("com.dropbear:magna-carta-plugin:1.0-SNAPSHOT")
            }
        }
    }
}

rootProject.name = "project"

