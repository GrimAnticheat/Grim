import versioning.BuildConfig

plugins {
    `maven-publish`
    grim.`base-conventions`
}

repositories {
    // We still call mavenLocal() conditionally at the top for non-exclusive deps (general fallback)
    if (BuildConfig.mavenLocalOverride) mavenLocal()

    // Grim API & PacketEvents
    exclusive("https://repo.grim.ac/snapshots") {
        includeGroup("ac.grim.grimac")
        includeGroup("com.github.retrooper")
    }

    // ViaVersion
    exclusive("https://repo.viaversion.com", { mavenContent { releasesOnly() } }) {
        includeGroup("com.viaversion")
    }

    // Configuralize
    exclusive("https://nexus.scarsz.me/content/repositories/releases", { mavenContent { releasesOnly() } }) {
        includeGroup("github.scarsz")
    }

    // Cumulus
    exclusive("https://repo.opencollab.dev/maven-releases/", { mavenContent { releasesOnly() } }) {
        includeGroup("org.geysermc.api")
    }

    // Floodgate
    exclusive("https://repo.opencollab.dev/maven-snapshots/", { mavenContent { snapshotsOnly() } }) {
        includeGroup("org.geysermc.floodgate")
        includeGroup("org.geysermc.cumulus")
        includeModule("org.geysermc", "common")
        includeModule("org.geysermc", "geyser-parent")
    }

    mavenCentral()
}


dependencies {
    if (BuildConfig.shadePE) {
        api(libs.packetevents.api)
    } else {
        compileOnly(libs.packetevents.api)
    }
    api(libs.cloud.core)
    api(libs.cloud.processors.requirements)
    api(libs.configuralize) {
        artifact {
            classifier = "slim"
        }
        exclude(group = "org.yaml", module = "snakeyaml")
    }
    // Bump snakeyaml (transitive dep of configuralize) 1.29 -> 2.2+ for geyser-fabric
    api(libs.snakeyaml)
    api(libs.fastutil)
    api(libs.adventure.text.minimessage)
    api(libs.jetbrains.annotations)
    api(libs.hikaricp)

    api(libs.grim.api)
    api(libs.grim.internal)
    compileOnly(libs.grim.internal.shims)

    compileOnly(libs.geyser.base.api) {
        isTransitive = false // messes with guava otherwise
    }

    compileOnly(libs.floodgate.api)
    compileOnly(libs.viaversion)
    compileOnly(libs.netty)
}

publishing.publications.create<MavenPublication>("maven") {
    from(components["java"])
}
