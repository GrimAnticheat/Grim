import net.fabricmc.loom.task.RemapJarTask
import com.google.gson.GsonBuilder
import java.io.File

val minecraft_version: String by project
val yarn_mappings: String by project
val loader_version: String by project
val fabric_version: String by project

plugins {
    `maven-publish`
    id("fabric-loom") version "1.9.2"
    grim.`base-conventions`
    grim.`jij-conventions`
}

repositories {
    mavenLocal()
    maven {
        name = "FabricMC"
        url = uri("https://maven.fabricmc.net/")
    }
    maven {
        name = "papermc"
        url = uri("https://repo.papermc.io/repository/maven-public/")
    }
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/") // Spigot
    maven("https://jitpack.io/") { // Grim API
        content {
            includeGroup("com.github.grimanticheat")
        }
    }
    maven("https://repo.viaversion.com") // ViaVersion
    maven("https://nexus.scarsz.me/content/repositories/releases") // Configuralize
    maven("https://repo.opencollab.dev/maven-snapshots/") // Floodgate
    maven("https://repo.opencollab.dev/maven-releases/") // Cumulus (for Floodgate)
    maven("https://repo.codemc.io/repository/maven-releases/") // PacketEvents
    maven("https://repo.codemc.io/repository/maven-snapshots/")
    maven("https://s01.oss.sonatype.org/content/repositories/snapshots/")
    mavenCentral()
    // FastUtil, Discord-Webhooks
}

dependencies {
    minecraft("com.mojang:minecraft:$minecraft_version")
    mappings("net.fabricmc:yarn:$yarn_mappings")
    modImplementation("net.fabricmc:fabric-loader:$loader_version")
    modImplementation("net.fabricmc.fabric-api:fabric-api:$fabric_version")

    modImplementation(libs.packetevents.fabric)
    modImplementation("org.incendo:cloud-fabric:2.0.0-beta.10")
    modImplementation("net.kyori:adventure-platform-fabric:6.2.0")
    modImplementation("me.lucko:fabric-permissions-api:0.3.1")

    implementation(project(":common"))
    include("me.lucko:fabric-permissions-api:0.3.1")
}

loom {
    accessWidenerPath = file("src/main/resources/grimac.accesswidener")
}

// Update the delegated properties
val mod_name: String  = "GrimAC"
val mod_access_widener: String = "grimac.accesswidener"
val mod_id: String = rootProject.name
val mod_description: String = rootProject.description ?: "A server-side mod"
val mod_environment: String = "server"
val mod_authors: String = "GrimAC"
val mod_license: String = "GPLv3"
val mod_entrypoints_main: String = "ac.grim.grimac.platform.fabric.GrimACFabricLoaderPlugin"

// Add this task to generate fabric.mod.json
tasks.register("generateFabricModJson") {
    group = "fabric"
    description = "Generates fabric.mod.json file"

    doLast {
        val fabricModJsonDir = file("src/main/resources")
        fabricModJsonDir.mkdirs()
        val fabricModJsonFile = File(fabricModJsonDir, "fabric.mod.json")

        val entrypoints = mutableMapOf<String, List<String>>(
            "main" to mod_entrypoints_main.split(",").map { it.trim() },
            "preLaunch" to mod_entrypoints_main.split(",").map { it.trim() }
        )
        // Don't include client entrypoint since we're server-only
        // If you later change mod_environment, you can add client entrypoints back

        val modJson = mapOf(
            "schemaVersion" to 1,
            "id" to mod_id,
            "version" to "${project.version}",
            "name" to mod_name,
            "description" to mod_description,
            "authors" to mod_authors.split(",").map { it.trim() },
            "license" to mod_license,
            "environment" to mod_environment,
            "entrypoints" to entrypoints,
            "accessWidener" to mod_access_widener,
            "depends" to mapOf(
                "fabricloader" to ">=$loader_version",
                "minecraft" to minecraft_version,
                "fabric-api" to "*"
            )
        )

        val gson = GsonBuilder().setPrettyPrinting().create()
        fabricModJsonFile.writeText(gson.toJson(modJson))
    }
}

// Make the processResources task depend on our generation task
tasks.named("processResources") {
    dependsOn("generateFabricModJson")
}

// Improve remapping tasks
tasks.withType<RemapJarTask>().configureEach {
    archiveFileName.set("${rootProject.name}-${project.name}-${rootProject.version}.jar")

    // Include classes from :common project directly
    from(project(":common").sourceSets.main.get().output)
}

tasks.withType<RemapJarTask>().configureEach {
    archiveFileName.set("${rootProject.name}-${project.name}-${rootProject.version}.jar")
}

publishing.publications.create<MavenPublication>("maven") {
    artifact(tasks["remapJar"])
}
