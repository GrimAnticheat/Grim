import versioning.BuildConfig

val minecraft_version: String by project

plugins {
    `maven-publish`
    alias(libs.plugins.fabric.loom.unobfuscated)
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
    minecraft("com.mojang:minecraft:$minecraft_version")

    include(project(":fabric:shared"))
    include(project(":fabric:intermediary"))
    include(project(":fabric:official"))
    include(libs.packetevents.fabric)
}

publishing.publications.create<MavenPublication>("maven") {
    artifact(tasks["jar"])
}

tasks {
    jar {
        archiveBaseName = "${rootProject.name}-fabric"
        archiveVersion = rootProject.version as String
    }
}
