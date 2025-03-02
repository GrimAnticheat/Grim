plugins {
    id("fabric-loom")
    id("grim.base-conventions")
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
    minecraft("com.mojang:minecraft:1.21.1")
    mappings("net.fabricmc:yarn:1.21.1+build.3:v2")

    modImplementation("net.fabricmc:fabric-loader:0.16.10")
    modImplementation("net.fabricmc.fabric-api:fabric-api:0.115.1+1.21.1")

    modImplementation("org.incendo:cloud-fabric:2.0.0-beta.10")
    modImplementation("me.lucko:fabric-permissions-api:0.3.1")

    implementation(project(":fabric"))
    implementation(project(":common"))
}

loom {
    accessWidenerPath = file("src/main/resources/grimac.accesswidener")
}

tasks.compileJava {
    options.release.set(21)
}
