//import net.minecrell.pluginyml.bukkit.BukkitPluginDescription.Permission

plugins {
    id("java")
    id("maven-publish")
    id("com.github.johnrengelman.shadow") version "7.1.2"
    id("io.freefair.lombok") version "6.6"
    //id("net.minecrell.plugin-yml.bukkit") version "0.5.2"
}


group = "ac.grim.grimac"
version = "2.3.45"
description = "Libre simulation anticheat designed for 1.20 with 1.8-1.20 support, powered by PacketEvents 2.0."
java.sourceCompatibility = JavaVersion.VERSION_1_8
java.targetCompatibility = JavaVersion.VERSION_1_8

repositories {
    mavenLocal()
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/") // Spigot
    maven("https://jitpack.io/") // Grim API
    maven("https://repo.viaversion.com") // ViaVersion
    maven("https://repo.aikar.co/content/groups/aikar/") // ACF
    maven("https://nexus.scarsz.me/content/repositories/releases") // Configuralize
    maven("https://clojars.org/repo") // MultiPaper MultiLib
    maven("https://repo.opencollab.dev/maven-snapshots/") // Floodgate
    maven("https://repo.codemc.io/repository/maven-snapshots/") // PacketEvents
    mavenCentral()
    maven {
        url = uri("https://repo.clojars.org")
        name = "Clojars"
    }
    // FastUtil, Discord-Webhooks
}

dependencies {
    implementation("com.github.retrooper.packetevents:spigot:2.0-SNAPSHOT")
    implementation("co.aikar:acf-paper:0.5.1-20230402.114301-23")
    implementation("club.minnced:discord-webhooks:0.8.0")
    implementation("it.unimi.dsi:fastutil:8.5.9")
    implementation("org.jetbrains:annotations:23.1.0") // Why is this needed to compile?
    implementation("github.scarsz:configuralize:1.4.0")
    implementation("com.github.puregero:multilib:1.1.8")

    implementation("com.github.grimanticheat:grimapi:c3a80bdea5")
    // Used for local testing: implementation("ac.grim.grimac:grimapi:1.0")

    compileOnly("org.geysermc.floodgate:api:2.0-SNAPSHOT")
    compileOnly("org.spigotmc:spigot-api:1.19.3-R0.1-SNAPSHOT")
    compileOnly("com.viaversion:viaversion-api:4.1.1")
    compileOnly("io.netty:netty-all:4.1.85.Final")
}

/*bukkit {
    name = "GrimAC"
    author = "GrimAC"

    main = "ac.grim.grimac.GrimAC"
    apiVersion = "1.13"

    softDepend = listOf(
        "ProtocolLib",
        "ProtocolSupport",
        "ViaVersion",
        "Essentials",
        "ViaBackwards",
        "ViaRewind",
        "Geyser-Spigot"
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

}*/

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
    relocate("io.github.retrooper.packetevents", "ac.grim.grimac.shaded.io.github.retrooper.packetevents")
    relocate("com.github.retrooper.packetevents", "ac.grim.grimac.shaded.com.github.retrooper.packetevents")
    relocate("co.aikar.acf", "ac.grim.grimac.shaded.acf")
    relocate("club.minnced", "ac.grim.grimac.shaded.discord-webhooks")
    relocate("github.scarsz.configuralize", "ac.grim.grimac.shaded.configuralize")
    relocate("com.github.puregero", "ac.grim.grimac.shaded.com.github.puregero")
    relocate("com.google.gson", "ac.grim.grimac.shaded.gson")
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
