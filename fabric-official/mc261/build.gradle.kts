import versioning.BuildConfig

val minecraft_version: String by project
val fabric_version: String by project

plugins {
    alias(libs.plugins.fabric.loom)
    grim.`base-conventions`
}

dependencies {
    api(project(":fabric-common"))
    compileOnly(project(":common"))

    minecraft("com.mojang:minecraft:$minecraft_version")
    implementation("net.fabricmc.fabric-api:fabric-api:$fabric_version")

    compileOnly("me.lucko:fabric-permissions-api:0.7.0")

    implementation(libs.cloud.fabric) {
        exclude(group = "net.fabricmc.fabric-api")
    }

    implementation(libs.fabric.loader)

    compileOnly(libs.packetevents.api)

    compileOnly("org.slf4j:slf4j-api:2.0.17")
    compileOnly("org.apache.logging.log4j:log4j-api:2.24.3")
}

repositories {
    if (BuildConfig.mavenLocalOverride) mavenLocal()

    exclusive("https://maven.fabricmc.net/") {
        includeGroup("net.fabricmc")
        includeGroup("net.fabricmc.fabric-api")
    }

    exclusive("https://repo.grim.ac/snapshots") {
        includeGroup("ac.grim.grimac")
        includeGroup("com.github.retrooper")
    }

    exclusive("https://jitpack.io", { mavenContent { releasesOnly() } }) {
        includeGroup("com.github.Fallen-Breath.conditional-mixin")
    }

    exclusive("https://repo.viaversion.com", { mavenContent { releasesOnly() } }) {
        includeGroup("com.viaversion")
    }

    exclusive("https://nexus.scarsz.me/content/repositories/releases", { mavenContent { releasesOnly() } }) {
        includeGroup("github.scarsz")
    }

    exclusive("https://repo.opencollab.dev/maven-releases/", { mavenContent { releasesOnly() } }) {
        includeGroup("org.geysermc.api")
    }

    exclusive("https://repo.opencollab.dev/maven-snapshots/", { mavenContent { snapshotsOnly() } }) {
        includeGroup("org.geysermc.floodgate")
        includeGroup("org.geysermc.cumulus")
        includeModule("org.geysermc", "common")
        includeModule("org.geysermc", "geyser-parent")
    }

    exclusive(mavenCentral()) { includeGroup("me.lucko") }

    maven("https://central.sonatype.com/repository/maven-snapshots/") {
        content { includeGroup("org.incendo") }
    }

    mavenCentral()
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(25))
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(25)
}

loom {
    accessWidenerPath = file("src/main/resources/grimac.accesswidener")
}
