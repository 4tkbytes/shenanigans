/// dropbear-engine template for gradle. its recommended to not touch it unless you
/// know what you're doing

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinxSerialization)
    id("magna-carta") version "1.0-SNAPSHOT"
}

group = "com.example.mygame"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    mavenLocal()
//    maven {url = uri("https://4tkbytes.github.io/dropbear/") }
}

kotlin {
    jvm()

    val hostOs = System.getProperty("os.name")
    val isArm64 = System.getProperty("os.arch") == "aarch64"
    val isMingwX64 = hostOs.startsWith("Windows")
    val nativeTarget = when {
        hostOs == "Mac OS X" && isArm64 -> macosArm64("nativeLib")
        hostOs == "Mac OS X" && !isArm64 -> macosX64("nativeLib")
        hostOs == "Linux" && isArm64 -> linuxArm64("nativeLib")
        hostOs == "Linux" && !isArm64 -> linuxX64("nativeLib")
        isMingwX64 -> mingwX64("nativeLib")
        else -> throw GradleException("Host OS is not supported in Kotlin/Native.")
    }

    val libName = when {
        hostOs == "Mac OS X" -> "libeucalyptus_core.dylib"
        hostOs == "Linux" -> "libeucalyptus_core.so"
        isMingwX64 -> "eucalyptus_core.dll.lib"
        else -> throw GradleException("Host OS is not supported in Kotlin/Native.")
    }

    val libPath = if (file("${project.rootDir}/target/debug/$libName").exists()) {
        println("Debug library exists")
        "${project.rootDir}/target/debug/$libName"
    } else if (file("${project.rootDir}/target/release/$libName").exists()) {
        println("Release library exists")
        "${project.rootDir}/target/release/$libName"
    } else if (file("${project.rootDir}/libs/$libName").exists()) {
        println("Local library exists")
        "${project.rootDir}/libs/$libName"
    } else {
        throw GradleException(
            "The required library [$libName] does not exist. \n" +
                    "\n" +
                    "Here is how to fix it:\n" +
                    "============================================================================\n" +
                    "You have two options. You can either build it yourself or download a prebuilt one. I would assume that you are just a standard game dev, so you would most likely want a prebuilt one. \n" +
                    "\n" +
                    "a. You can download the eucalyptus_core library from https://github.com/4tkbytes/dropbear in the releases tab. \n" +
                    "Once you have the library, you can put it in the libs folder in the root of this project.\n" +
                    "\n" +
                    "In the case that there is no release, or you just want the cutting edge, you can build it yourself. \n" +
                    "\n" +
                    "b. Build instructions can be found here: https://github.com/4tkbytes/dropbear/blob/main/README.md but here it is anyways: \n" +
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
    }

    nativeTarget.apply {
        binaries {
            sharedLib {
                baseName = "project"
                export("com.dropbear:dropbear:1.0-SNAPSHOT")
                linkerOpts(
                    libPath,
                    "-Wl,-rpath,\\\$ORIGIN"
                )
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