plugins {
    `java-library`
    `maven-publish`
    grim.`base-conventions`
    id("com.gradleup.shadow") version "9.0.0-beta6"
}


val relocate: Boolean by rootProject.extra

repositories {
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
    api("com.github.retrooper:packetevents-api:2.7.1-SNAPSHOT")
    api("org.incendo:cloud-core:2.0.0")
    api("club.minnced:discord-webhooks:0.8.0") // Newer versions include kotlin-stdlib, which leads to incompatibility with plugins that use Kotlin
    api("it.unimi.dsi:fastutil:8.5.15")
    api("github.scarsz:configuralize:1.4.0")
    api("net.kyori:adventure-text-minimessage:4.17.0")
    api("org.jetbrains:annotations:24.1.0")

    // Used for local testing:
//    implementation("ac.grim.grimac:GrimAPI:1.0")
    api("com.github.grimanticheat:grimapi:f1eff912b6")

    compileOnly("org.geysermc.floodgate:api:2.0-SNAPSHOT")
    compileOnly("com.viaversion:viaversion-api:5.0.4-SNAPSHOT")
    compileOnly("io.netty:netty-all:4.1.85.Final")
}

tasks.build {
    dependsOn(tasks.shadowJar)
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.release.set(17)
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
    disableAutoTargetJvm()
}

publishing.publications.create<MavenPublication>("maven") {
    artifact(tasks["shadowJar"])
}

tasks.shadowJar {
    minimize()
    archiveFileName.set("${rootProject.name}-${project.name}-${rootProject.version}.jar")
    if (relocate) {
        relocate("io.github.retrooper.packetevents", "ac.grim.grimac.shaded.io.github.retrooper.packetevents")
        relocate("com.github.retrooper.packetevents", "ac.grim.grimac.shaded.com.github.retrooper.packetevents")
        relocate("co.aikar.commands", "ac.grim.grimac.shaded.acf")
        relocate("co.aikar.locale", "ac.grim.grimac.shaded.locale")
        relocate("club.minnced", "ac.grim.grimac.shaded.discord-webhooks")
        relocate("github.scarsz.configuralize", "ac.grim.grimac.shaded.configuralize")
        relocate("com.github.puregero", "ac.grim.grimac.shaded.com.github.puregero")
        relocate("com.google.code.gson", "ac.grim.grimac.shaded.gson")
        relocate("alexh", "ac.grim.grimac.shaded.maps")
        relocate("it.unimi.dsi.fastutil", "ac.grim.grimac.shaded.fastutil")
        relocate("net.kyori", "ac.grim.grimac.shaded.kyori")
        relocate("okhttp3", "ac.grim.grimac.shaded.okhttp3")
        relocate("okio", "ac.grim.grimac.shaded.okio")
        relocate("org.yaml.snakeyaml", "ac.grim.grimac.shaded.snakeyaml")
        relocate("org.json", "ac.grim.grimac.shaded.json")
        relocate("org.intellij", "ac.grim.grimac.shaded.intellij")
        relocate("org.jetbrains", "ac.grim.grimac.shaded.jetbrains")
    }
}
