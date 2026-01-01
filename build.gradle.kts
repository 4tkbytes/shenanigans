/// dropbear-engine template for gradle. its recommended to not touch it unless you
/// know what you're doing

import org.gradle.api.GradleException
import org.gradle.internal.os.OperatingSystem
import kotlin.concurrent.thread

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinxSerialization)
}

group = "com.example.mygame"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    mavenLocal()
//    maven {url = uri("https://tirbofish.github.io/dropbear/") }
}

val hostOs = providers.systemProperty("os.name").get()
val isArm64 = providers.systemProperty("os.arch").map { it == "aarch64" }.get()
val isMingwX64 = hostOs.startsWith("Windows")
val isLinux = hostOs == "Linux"
val isMacOs = hostOs == "Mac OS X"

val libName = when {
    isMacOs -> "libeucalyptus_core.dylib"
    isLinux -> "libeucalyptus_core.so"
    isMingwX64 -> "eucalyptus_core.dll"
    else -> throw GradleException("Host OS is not supported in Kotlin/Native.")
}

val libPathProvider = provider {
    val candidates = listOf(
        layout.projectDirectory.file("target/debug/$libName").asFile,
        layout.projectDirectory.file("target/release/$libName").asFile,
        layout.projectDirectory.file("libs/$libName").asFile
    )

    val foundLib = candidates.firstOrNull { it.exists() }

    if (foundLib == null) {
        println(
            "The required library [$libName] does not exist. \n" +
                    "\n" +
                    "Here is how to fix it:\n" +
                    "============================================================================\n" +
                    "You have two options. You can either build it yourself or download a prebuilt one. I would assume that you are just a standard game dev, so you would most likely want a prebuilt one. \n" +
                    "\n" +
                    "a. You can download the eucalyptus_core library from https://github.com/4tkbytes/dropbear   in the releases tab. \n" +
                    "Once you have the library, you can put it in the libs folder in the root of this project.\n" +
                    "\n" +
                    "In the case that there is no release, or you just want the cutting edge, you can build it yourself. \n" +
                    "\n" +
                    "b. Build instructions can be found here: https://github.com/4tkbytes/dropbear/blob/main/README.md   but here it is anyways: \n" +
                    "\n" +
                    "\t1. Clone the dropbear repository. \n" +
                    "\t2. Run cargo build --release\n" +
                    "\t3. The library should be in the target/debug or target/release folder depending on how you built it (most likely the release). Copy that library into the ${project.rootDir}/libs folder. \n" +
                    "\t4. Profit!\n" +
                    "\t\n" +
                    "If there is still a further issue, please open an issue on the dropbear repository.\n" +
                    "\n" +
                    "Anyhow, glhf ꉂ(˵˃ ᗜ ˂˵)\n" +
                    "============================================================================"
        )
        ""  // Return empty string instead of throwing or returning "ERROR"
    } else {
        foundLib.absolutePath
    }
}

kotlin {
    jvm()

    val nativeTarget = when {
        isMacOs && isArm64 -> macosArm64("nativeLib")
        isMacOs && !isArm64 -> macosX64("nativeLib")
        isLinux && isArm64 -> linuxArm64("nativeLib")
        isLinux && !isArm64 -> linuxX64("nativeLib")
        isMingwX64 -> mingwX64("nativeLib")
        else -> throw GradleException("Host OS is not supported in Kotlin/Native.")
    }

    nativeTarget.apply {
        binaries {
            sharedLib {
                baseName = "MyGame"

                val nativeLibPath = libPathProvider.get()
                if (nativeLibPath.isNotEmpty()) {
                    if (isLinux || isMacOs) {
                        linkerOpts(
                            nativeLibPath,
                            "-Wl,-rpath,\\\$ORIGIN"
                        )
                    } else if (isMingwX64) {
                        linkerOpts(
                            "$nativeLibPath.lib"
                        )
                    }
                } else {
                    throw GradleException("Native library not found. Please check the error message above.")
                }
            }
        }
    }

    sourceSets {
        commonMain {
            dependencies {
		        // TODO: change this when there is a proper release
                api("com.dropbear:dropbear:1.0-SNAPSHOT")
            }
        }

        // -----------------------------------------------------------------------------------------------
        //               ENSURE THIS IS KEPT OTHERWISE MAGNA-CARTA WON'T BE ABLE TO RUN
        // -----------------------------------------------------------------------------------------------
        val jvmMain by getting {
            kotlin.srcDir(layout.buildDirectory.dir("magna-carta/jvmMain"))
        }

        val nativeLibMain by getting {
            kotlin.srcDir(layout.buildDirectory.dir("magna-carta/nativeLibMain"))
        }
        // -----------------------------------------------------------------------------------------------
    }
}

tasks.register<Jar>("fatJar") {
    archiveClassifier.set("all")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    from(kotlin.jvm().compilations["main"].output)

    configurations.named("jvmRuntimeClasspath").get().forEach { file ->
        if (file.name.endsWith(".jar")) {
            from(zipTree(file))
        } else {
            from(file)
        }
    }

    manifest {}
}

tasks.register<Exec>("play") {
    group = "run"
    description = "Builds fatJar, then launches the editor in play mode with JDWP enabled for IntelliJ attach."
    dependsOn("fatJar")

    doFirst {
        val editorPath = System.getenv("EUCALYPTUS_EDITOR")?.trim().orEmpty()
        if (editorPath.isEmpty()) {
            throw GradleException(
                "The EUCALYPTUS_EDITOR variable is not set. Please ensure it is set."
            )
        }

        val projectRoot = project.rootDir.absolutePath

        thread(start = true) {
            Thread.sleep(3000)
            try {
                val attachScript = if (OperatingSystem.current().isWindows) {
                    listOf("cmd", "/c", "jdb", "-attach", "localhost:6751")
                } else {
                    listOf("jdb", "-attach", "localhost:6751")
                }

                ProcessBuilder()
                    .command(attachScript)
                    .inheritIO()
                    .start()

                println("Debugger attachment initiated on port 6751")
            } catch (e: Exception) {
                println("Note: Auto-attach failed. Manually attach debugger to localhost:6751")
            }
        }

        val isWindows = OperatingSystem.current().isWindows
        val lower = editorPath.lowercase()
        val isCmdScript = lower.endsWith(".cmd") || lower.endsWith(".bat")

        commandLine(
            if (isWindows && isCmdScript) listOf("cmd", "/c", editorPath, "play", projectRoot, "--await-jdb", "true")
            else listOf(editorPath, "play", projectRoot, "--await-jdb", "true")
        )

        workingDir = project.rootDir
    }
}