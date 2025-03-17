plugins {
    `maven-publish`
    grim.`base-conventions`
}

repositories {
    mavenLocal()
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
    api(libs.packetevents.api)
    api(libs.cloud.core)
    api("org.incendo:cloud-processors-requirements:1.0.0-rc.1")
    api("github.scarsz:configuralize:1.4.1:slim") {
        exclude(group = "org.yaml", module = "snakeyaml")
    }
    // Bump snakeyaml (transitive dep of configuralize) 1.29 -> 2.2 for geyser-fabric
    api("org.yaml:snakeyaml:2.2")
    api(libs.discord.webhooks)
    api(libs.fastutil)
    api(libs.adventure.text.minimessage)
    api(libs.jetbrains.annotations)

    // Used for local testing:
    // api("ac.grim.grimac:GrimAPI:1.0")
    api("com.github.grimanticheat:grimapi:e0b2835fcf")

    compileOnly(libs.floodgate.api)
    compileOnly(libs.via.version.api)
    compileOnly(libs.netty)
}

publishing.publications.create<MavenPublication>("maven") {
    from(components["java"])
}
