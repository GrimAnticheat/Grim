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

    // Pull the variants in as nested JiJ entries. fabric-common is a regular Java
    // library so we use `implementation` + `include`; the two variant projects
    // produce their own Loom remapJars via the `namedElements` configuration.
    include(project(":fabric-common"))
    include(project(":fabric-intermediary", configuration = "namedElements"))
    include(project(":fabric-official", configuration = "namedElements"))
}

evaluationDependsOn(":fabric-intermediary")
evaluationDependsOn(":fabric-official")

publishing.publications.create<MavenPublication>("maven") {
    artifact(tasks["remapJar"])
}

tasks {
    remapJar {
        archiveBaseName = "${rootProject.name}-fabric"
        archiveVersion = rootProject.version as String

        val intermediaryRemap = project(":fabric-intermediary").tasks.named<RemapJarTask>("remapJar")
        val officialRemap = project(":fabric-official").tasks.named<RemapJarTask>("remapJar")
        dependsOn(intermediaryRemap)
        dependsOn(officialRemap)
        nestedJars.from(intermediaryRemap)
        nestedJars.from(officialRemap)
    }
}
