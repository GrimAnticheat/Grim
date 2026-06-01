import net.fabricmc.loom.task.RemapJarTask
import versioning.BuildConfig

val minecraft_version: String by project
val fabric_version: String by project

// Top-level Grim Fabric aggregator. Produces the published `grimac-fabric-<ver>.jar`
// containing both the intermediary chain (1.16.1→1.21.11) and the official 26.X stub
// as nested mods. Fabric Loader picks which variant to enable at runtime based on the
// declared minecraft version ranges in each nested mod.

plugins {
    `maven-publish`
    alias(libs.plugins.fabric.loom)
    grim.`base-conventions`
    grim.`jij-conventions`
}

repositories {
    if (BuildConfig.mavenLocalOverride) mavenLocal()
    exclusive("https://maven.fabricmc.net/") {
        includeGroup("net.fabricmc")
        includeGroup("net.fabricmc.fabric-api")
    }
    // PE snapshots live here; the aggregator-level include(libs.packetevents.fabric)
    // needs this resolvable even when mavenLocalOverride is off (CI / fresh checkouts).
    exclusive("https://repo.grim.ac/snapshots") {
        includeGroup("ac.grim.grimac")
        includeGroup("com.github.retrooper")
    }
    mavenCentral()
}

dependencies {
    // The aggregator itself only needs a Minecraft to satisfy Loom; bind the lowest
    // supported version. The shipped variant mods carry their own per-version deps.
    minecraft("com.mojang:minecraft:$minecraft_version")
    mappings(loom.officialMojangMappings())
    modImplementation(libs.fabric.loader)

    // fabric-common is a regular Java library so it's pulled via Loom's `include(...)`
    // JiJ. The variant mods (fabric-intermediary, fabric-official) are nested below
    // via `nestedJars.from(remapJar)` to avoid the double-JiJ of dev + remapped
    // artifacts.
    include(project(":fabric-common"))

    // PE lives at the aggregator level so its depends.minecraft >=1.16.1 means it
    // stays active across the full MC range (1.16.1 → 26.X), regardless of whether
    // the user's MC matches grimac-fabric-intermediary's range (<26) or
    // grimac-fabric-official's range (>=26.1).
    include(libs.packetevents.fabric)
}

publishing.publications.create<MavenPublication>("maven") {
    artifact(tasks["remapJar"])
}

tasks {
    remapJar {
        archiveBaseName = "${rootProject.name}-fabric"
        archiveVersion = rootProject.version as String

        // Nest variant remapJars by file path (not by task reference), so the aggregator
        // doesn't trigger full project configuration of the variants — which would
        // inject dev/namedElements jars into our include configuration.
        dependsOn(":fabric-intermediary:remapJar", ":fabric-official:remapJar")
        nestedJars.from(
            project(":fabric-intermediary").layout.buildDirectory.file(
                "libs/${rootProject.name}-fabric-intermediary-${rootProject.version}.jar"
            )
        )
        nestedJars.from(
            project(":fabric-official").layout.buildDirectory.file(
                "libs/${rootProject.name}-fabric-official-${rootProject.version}.jar"
            )
        )
    }
}
