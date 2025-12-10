import versioning.BuildConfig

val minecraft_version: String by project
val yarn_mappings: String by project
val fabric_version: String by project

plugins {
    `maven-publish`
    alias(libs.plugins.fabric.loom)
    grim.`base-conventions`
    grim.`jij-conventions`
}

dependencies {
    minecraft("com.mojang:minecraft:$minecraft_version")
    mappings(loom.officialMojangMappings())
    modImplementation(fabricApi.module("fabric-lifecycle-events-v1", fabric_version))

    modImplementation("me.lucko:fabric-permissions-api:0.3.1")

    modImplementation(libs.cloud.fabric)
    modImplementation(libs.fabric.loader)
    if (BuildConfig.shadePE) {
        modImplementation(libs.packetevents.fabric)
    } else {
        compileOnly(libs.packetevents.fabric)
    }
    compileOnly("org.slf4j:slf4j-api:2.0.17")
    compileOnly("org.apache.logging.log4j:log4j-api:2.24.3")

    modApi(libs.packetevents.fabric)
}

// The configurations below will only apply to :fabric and its submodules, not its siblings or the root project
allprojects {
    apply(plugin = "fabric-loom")
    apply(plugin = "grim.base-conventions")
    apply(plugin = "maven-publish")

    repositories {
        if (BuildConfig.mavenLocalOverride) {
            mavenLocal()
        }

        exclusiveContent {
            forRepository {
                maven("https://maven.fabricmc.net/")
            }
            filter {
                includeGroup("net.fabricmc")
                includeGroup("net.fabricmc.fabric-api")
            }
        }

        exclusiveContent {
            forRepository {
                maven("https://repo.grim.ac/snapshots") // Grim API & PacketEvents
            }
            filter {
                includeGroup("ac.grim.grimac")
                includeGroup("com.github.retrooper")
            }
        }

        exclusiveContent {
            forRepository {
                maven("https://jitpack.io") { // Conditional Mixin
                    mavenContent { releasesOnly() }
                }
            }
            filter {
                includeGroup("com.github.Fallen-Breath.conditional-mixin")
            }
        }

        exclusiveContent {
            // The repository URL is determined by the project's name
            if (project.name == "mc1161") {
                // For the 1.16.1 subproject, the old snapshot is on the Grim repo
                forRepository {
                    maven("https://repo.grim.ac/snapshots")
                }
            } else {
                // For all other subprojects, the releases are on Maven Central
                forRepository {
                    mavenCentral()
                }
            }
            // This filter applies to whichever repository was chosen above
            filter {
                includeGroup("me.lucko")
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

    loom {
        accessWidenerPath = file("src/main/resources/grimac.accesswidener")
    }

    dependencies {
        // I hate this syntax, is there an alternative to make modCompileOnly(libs.package.name) work?
        val libsx = rootProject.extensions.getByType<VersionCatalogsExtension>().named("libs")
        // Use the libs extension from the root project
        modImplementation(libsx.findLibrary("cloud-fabric").get()) {
            exclude(group = "net.fabricmc.fabric-api")
        }
        modImplementation(libsx.findLibrary("fabric-loader").get())

        implementation(project(":common"))
    }

    publishing.publications.create<MavenPublication>("maven") {
        artifact(tasks["remapJar"])
    }

    tasks {
        remapJar {
            archiveBaseName = "${rootProject.name}-fabric${if (project.name != "fabric") "-${project.name}" else ""}"
            archiveVersion = rootProject.version as String
        }

        remapSourcesJar {
            archiveVersion = rootProject.version as String
        }
    }
}

subprojects {
    dependencies {
        // configuration = "namedElements" required when depending on another loom project
        implementation(project(":fabric", configuration = "namedElements"))
    }
}

subprojects.forEach {
    tasks.named("remapJar").configure {
        dependsOn("${it.path}:remapJar")
    }
}

tasks.remapJar.configure {
    subprojects.forEach { subproject ->
        subproject.tasks.matching { it.name == "remapJar" }.configureEach {
            nestedJars.from(this)
        }
    }
}
