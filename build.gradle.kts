import net.minecrell.pluginyml.bukkit.BukkitPluginDescription.Permission

plugins {
    id("java")
    id("maven-publish")
    id("com.github.johnrengelman.shadow") version "8.1.1"
    id("io.freefair.lombok") version "8.6"
    id("net.minecrell.plugin-yml.bukkit") version "0.6.0"
}

group = "ac.grim.grimac"
version = "2.3.68"
description = "Libre simulation anticheat designed for 1.21 with 1.8-1.21 support, powered by PacketEvents 2.0."
java.sourceCompatibility = JavaVersion.VERSION_17
java.targetCompatibility = JavaVersion.VERSION_17

// Set to false for debug builds
// You cannot live reload classes if the jar relocates dependencies
var relocate = true;

repositories {
    mavenLocal()
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/") // Spigot
    maven("https://jitpack.io/") { // Grim API
        content {
            includeGroup("com.github.grimanticheat")
        }
    }
    maven("https://repo.viaversion.com") // ViaVersion
    maven("https://repo.aikar.co/content/groups/aikar/") // ACF
    maven("https://nexus.scarsz.me/content/repositories/releases") // Configuralize
    maven("https://repo.opencollab.dev/maven-snapshots/") // Floodgate
    maven("https://repo.opencollab.dev/maven-releases/") // Cumulus (for Floodgate)
    maven("https://repo.codemc.io/repository/maven-releases/") // PacketEvents
    maven("https://repo.codemc.io/repository/maven-snapshots/")
    mavenCentral()
    // FastUtil, Discord-Webhooks
}

dependencies {
    implementation("com.github.retrooper:packetevents-spigot:2.5.1-SNAPSHOT")
    implementation("co.aikar:acf-paper:0.5.1-SNAPSHOT")
    implementation("club.minnced:discord-webhooks:0.8.0") // Newer versions include kotlin-stdlib, which leads to incompatibility with plugins that use Kotlin
    implementation("it.unimi.dsi:fastutil:8.5.13")
    implementation("github.scarsz:configuralize:1.4.0")

    //implementation("com.github.grimanticheat:grimapi:1193c4fa41")
    // Used for local testing: implementation("ac.grim.grimac:GRIMAPI:1.0")
    implementation("com.github.grimanticheat:grimapi:fc5634e444")

    implementation("org.jetbrains:annotations:24.1.0")
    compileOnly("org.geysermc.floodgate:api:2.0-SNAPSHOT")
    compileOnly("org.spigotmc:spigot-api:1.18.2-R0.1-SNAPSHOT")
    compileOnly("com.viaversion:viaversion-api:5.0.4-SNAPSHOT")
    //
    compileOnly("io.netty:netty-all:4.1.85.Final")
}

bukkit {
    name = "GrimAC"
    author = "GrimAC"
    main = "ac.grim.grimac.GrimAC"
    website = "https://grim.ac/"
    apiVersion = "1.13"
    foliaSupported = true

    softDepend = listOf(
        "ProtocolLib",
        "ProtocolSupport",
        "Essentials",
        "ViaVersion",
        "ViaBackwards",
        "ViaRewind",
        "Geyser-Spigot",
        "floodgate",
        "FastLogin"
    )

    permissions {
        register("grim.alerts") {
            description = "Receive alerts for violations"
            default = Permission.Default.OP
        }

        register("grim.alerts.enable-on-join") {
            description = "Enable alerts on join"
            default = Permission.Default.OP
        }

        register("grim.performance") {
            description = "Check performance metrics"
            default = Permission.Default.OP
        }

        register("grim.profile") {
            description = "Check user profile"
            default = Permission.Default.OP
        }

        register("grim.brand") {
            description = "Show client brands on join"
            default = Permission.Default.OP
        }

        register("grim.sendalert") {
            description = "Send cheater alert"
            default = Permission.Default.OP
        }

        register("grim.nosetback") {
            description = "Disable setback"
            default = Permission.Default.FALSE
        }

        register("grim.nomodifypacket") {
            description = "Disable modifying packets"
            default = Permission.Default.FALSE
        }

        register("grim.exempt") {
            description = "Exempt from all checks"
            default = Permission.Default.FALSE
        }
    }
}

tasks.build {
    dependsOn(tasks.shadowJar)
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

publishing.publications.create<MavenPublication>("maven") {
    artifact(tasks["shadowJar"])
}

tasks.shadowJar {
    minimize()
    archiveFileName.set("${project.name}-${project.version}.jar")
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