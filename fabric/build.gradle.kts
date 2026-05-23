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
    mavenCentral()
}

dependencies {
    // Bind to the lowest supported MC version so Loom is happy.
    minecraft("com.mojang:minecraft:$minecraft_version")
    mappings(loom.officialMojangMappings())
    modImplementation(libs.fabric.loader)

    // fabric-common is a regular Java library so it's pulled via Loom's `include(...)`
    // JiJ. The variant mods (fabric-intermediary, fabric-official) are nested below
    // via `nestedJars.from(remapJar)` to avoid the double-JiJ of dev + remapped
    // artifacts.
    include(project(":fabric-common"))
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
