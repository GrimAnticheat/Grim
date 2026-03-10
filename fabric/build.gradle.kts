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

    modCompileOnly("me.lucko:fabric-permissions-api:0.3.1")

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
        // 1. Fallback for non-exclusive deps
        if (BuildConfig.mavenLocalOverride) mavenLocal()

        // 2. Exclusive Repositories
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

        // Special logic for LuckPerms
        if (project.name == "mc1161") {
            exclusive("https://repo.grim.ac/snapshots") { includeGroup("me.lucko") }
        } else {
            // Enforce Central for LuckPerms so we don't accidentally check other snapshot repos
            exclusive(mavenCentral()) { includeGroup("me.lucko") }
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
