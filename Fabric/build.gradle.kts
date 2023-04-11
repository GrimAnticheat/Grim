plugins {
    id("fabric-loom") version "1.1-SNAPSHOT"
}

apply(plugin = "fabric-loom")

val minecraftVersion = "1.19.4"
val mappingsVersion = "1.19.4+build.1"

val loaderVersion = "0.14.17"
val apiVersion = "0.76.0+1.19.4"
val kotlinVersion = "1.9.2+kotlin.1.8.10"

dependencies {
    minecraft("com.mojang", "minecraft", minecraftVersion)
    mappings("net.fabricmc", "yarn", mappingsVersion, null, "v2")

    modImplementation("net.fabricmc", "fabric-loader", loaderVersion)
    modImplementation("net.fabricmc.fabric-api", "fabric-api", apiVersion)
    modImplementation("net.fabricmc", "fabric-language-kotlin", kotlinVersion)
}

tasks.processResources {
    filesMatching("fabric.mod.json") {
        expand(mutableMapOf(
            "version" to project.version as String,
            "fabricloader" to loaderVersion,
            "fabric_api" to apiVersion,
            "fabric_language_kotlin" to kotlinVersion,
            "minecraft" to minecraftVersion,
            "java" to "19"
        ))
    }
    filesMatching("*.mixins.json") {
        expand(mutableMapOf("java" to "19"))
    }
}