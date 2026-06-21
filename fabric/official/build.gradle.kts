import versioning.BuildConfig

val minecraft_version: String by project
val fabric_version: String by project

plugins {
    `maven-publish`
    // No version: loom is already on the classpath from the parent :fabric project,
    // so a version request here would fail compatibility checking.
    id("net.fabricmc.fabric-loom")
    grim.`base-conventions`
    grim.`jij-conventions`
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

loom {
    accessWidenerPath = file("src/main/resources/grimac.accesswidener")
}

dependencies {
    minecraft("com.mojang:minecraft:$minecraft_version")
    compileOnly(libs.fabric.loader)

    implementation("org.incendo:cloud-fabric:2.0.0-beta.16") {
        exclude(group = "net.fabricmc.fabric-api")
    }
    implementation(libs.cloud.core)

    compileOnly("me.lucko:fabric-permissions-api:0.7.0")
    implementation(fabricApi.module("fabric-lifecycle-events-v1", fabric_version))
    compileOnly("net.fabricmc.fabric-api:fabric-api:$fabric_version")

    implementation(project(":common"))
    implementation(project(":fabric:shared"))
    compileOnly(libs.packetevents.api)
    compileOnly(libs.packetevents.fabric)
    compileOnly("org.slf4j:slf4j-api:2.0.17")
    compileOnly("org.apache.logging.log4j:log4j-api:2.24.3")
}

allprojects {
    apply(plugin = "net.fabricmc.fabric-loom")
    apply(plugin = "grim.base-conventions")
    apply(plugin = "maven-publish")

    repositories {
        if (BuildConfig.mavenLocalOverride) mavenLocal()

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

        mavenCentral()
    }

    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(25))
        }
    }

    dependencies {
        val libsx = rootProject.extensions.getByType<VersionCatalogsExtension>().named("libs")
        compileOnly(libsx.findLibrary("fabric-loader").get())
    }

    publishing.publications.create<MavenPublication>("maven") {
        artifact(tasks["jar"])
    }

    tasks {
        matching { it.name == "sourcesJar" }
            .configureEach { enabled = false }

        jar {
            archiveBaseName = if (project == project(":fabric:official")) {
                "${rootProject.name}-fabric-official"
            } else {
                "${rootProject.name}-fabric-${project.name}"
            }
            archiveVersion = rootProject.version as String
        }
    }
}

subprojects {
    dependencies {
        implementation(project(":fabric:official"))
        compileOnly(project(":common"))
        compileOnly(project(":fabric:shared"))
        val libsx = rootProject.extensions.getByType<VersionCatalogsExtension>().named("libs")
        compileOnly(libsx.findLibrary("packetevents-api").get())
        compileOnly(libsx.findLibrary("packetevents-fabric").get())
    }
}

subprojects.forEach {
    dependencies {
        include(project(it.path))
    }
}
