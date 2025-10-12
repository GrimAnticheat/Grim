import versioning.BuildConfig

plugins {
    `maven-publish`
    grim.`base-conventions`
}

repositories {
    if (BuildConfig.mavenLocalOverride) {
        mavenLocal()
    }

    exclusiveContent {
        forRepository {
            maven("https://repo.grim.ac/snapshots") // Grim API
        }
        filter {
            includeGroup("ac.grim.grimac")
            includeGroup("com.github.retrooper")
        }
    }

    exclusiveContent {
        forRepository {
            maven("https://repo.viaversion.com") { // ViaVersion
                mavenContent { releasesOnly() }
            }
        }
        filter {
            includeGroup("com.viaversion")
        }
    }

    exclusiveContent {
        forRepository {
            maven("https://nexus.scarsz.me/content/repositories/releases") { // Configuralize
                mavenContent { releasesOnly() }
            }
        }
        filter {
            includeGroup("github.scarsz")
        }
    }

    exclusiveContent {
        forRepository {
            maven("https://repo.opencollab.dev/maven-releases/") { // Cumulus (for Floodgate)
                mavenContent { releasesOnly() }
            }
        }
        filter {
            includeGroup("org.geysermc.api")
        }
    }

    exclusiveContent {
        forRepository {
            maven("https://repo.opencollab.dev/maven-snapshots/") { // Floodgate
                mavenContent { snapshotsOnly() }
            }
        }
        filter {
            includeGroup("org.geysermc.floodgate")
            includeGroup("org.geysermc.cumulus")
            includeModule("org.geysermc", "common")
            includeModule("org.geysermc", "geyser-parent")
        }
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
