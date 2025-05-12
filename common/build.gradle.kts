import versioning.BuildConfig

plugins {
    `maven-publish`
    grim.`base-conventions`
}

repositories {
    if (BuildConfig.mavenLocalOverride) {
        mavenLocal()
    }
    maven {
        name = "papermc"
        url = uri("https://repo.papermc.io/repository/maven-public/")
    }
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/") // Spigot
    maven("https://repo.grim.ac/snapshots") { // Grim API
        content {
            includeGroup("ac.grim.grimac")
            includeGroup("com.github.retrooper")
        }
    }
    maven("https://repo.viaversion.com") // ViaVersion
    maven("https://nexus.scarsz.me/content/repositories/releases") // Configuralize
    maven("https://repo.opencollab.dev/maven-snapshots/") // Floodgate
    maven("https://repo.opencollab.dev/maven-releases/") // Cumulus (for Floodgate)
    maven("https://s01.oss.sonatype.org/content/repositories/snapshots/")
    mavenCentral() // FastUtil
}

dependencies {
    if (BuildConfig.shadePE) {
        api(libs.packetevents.api)
    } else {
        compileOnly(libs.packetevents.api)
    }
    api(libs.cloud.core)
    api("org.incendo:cloud-processors-requirements:1.0.0-rc.1")
    api("github.scarsz:configuralize:1.4.1:slim") {
        exclude(group = "org.yaml", module = "snakeyaml")
    }
    // Bump snakeyaml (transitive dep of configuralize) 1.29 -> 2.2 for geyser-fabric
    api("org.yaml:snakeyaml:2.2")
    api(libs.fastutil)
    api(libs.adventure.text.minimessage)
    api(libs.jetbrains.annotations)

    api("ac.grim.grimac:GrimAPI:1.1.0.0")

    compileOnly(libs.floodgate.api)
    compileOnly(libs.via.version.api)
    compileOnly(libs.netty)
}

publishing.publications.create<MavenPublication>("maven") {
    from(components["java"])
}
