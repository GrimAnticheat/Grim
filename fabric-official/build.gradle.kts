import versioning.BuildConfig

val minecraft_version: String by project
val fabric_version: String by project

// WHY the empty `intermediary:0.0.0:v2` mappings stub: it makes the named->intermediary
// remap a no-op, so the official-mapped 26.X bytecode is left untouched in remapJar (the
// 26.X anticheat code is official-mappings-only, so there is nothing to remap).
plugins {
    `maven-publish`
    alias(libs.plugins.fabric.loom)
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
    // cloud-fabric, fabric-permissions-api and fabric-api are published in Fabric's
    // `official` (Mojang) mapping namespace for MC 26.1, so they link against the
    // empty-stub classpath with no remap and use PLAIN configs (implementation/
    // compileOnly), not the mod* remap configs the yarn-mapped intermediary module needs.
    mappings("net.fabricmc:intermediary:0.0.0:v2")
    modImplementation(libs.fabric.loader)

    // Command framework. beta.16 pinned here (catalog tracks beta.15 for the intermediary
    // line); exclude transitive fabric-api so it doesn't pull a second unpinned copy.
    implementation("org.incendo:cloud-fabric:2.0.0-beta.16") {
        exclude(group = "net.fabricmc.fabric-api")
    }
    implementation(libs.cloud.core)

    // Optional soft dependency, guarded at runtime by isModLoaded(...) in FabricSenderFactory.
    compileOnly("me.lucko:fabric-permissions-api:0.7.0")
    // fabric-permissions-api's getPermissionValue returns fabric-api's TriState.
    compileOnly("net.fabricmc.fabric-api:fabric-api:$fabric_version")

    implementation(project(":common"))
    // NMS-free Fabric platform code shared with fabric-intermediary lives here.
    implementation(project(":fabric-common"))
    compileOnly(libs.packetevents.api)
    compileOnly(libs.packetevents.fabric)
    compileOnly("org.slf4j:slf4j-api:2.0.17")
    compileOnly("org.apache.logging.log4j:log4j-api:2.24.3")
}

allprojects {
    apply(plugin = "fabric-loom")
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
        modImplementation(libsx.findLibrary("fabric-loader").get())
        // :common is intentionally NOT pulled here; its transitive PE dep would force
        // Loom to remap an intermediary-namespaced access widener against 0.0.0 (fails).
        // When real 26.X mappings land, re-add `implementation(project(":common"))`.
    }

    publishing.publications.create<MavenPublication>("maven") {
        artifact(tasks["remapJar"])
    }

    tasks {
        // Intermediary 0.0.0 has no "named" namespace, so source remap fails. Disable
        // sources jar generation in any subproject that registers it.
        matching { it.name == "remapSourcesJar" || it.name == "sourcesJar" }
            .configureEach { enabled = false }

        remapJar {
            archiveBaseName = if (project == project(":fabric-official")) {
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
        implementation(project(":fabric-official", configuration = "namedElements"))
        compileOnly(project(":common"))
        // Shared NMS-free Fabric code (e.g. FabricFutureUtil) lives in fabric-common;
        // the per-version submodules reference it, so it must be on their compile path.
        compileOnly(project(":fabric-common"))
        val libsx = rootProject.extensions.getByType<VersionCatalogsExtension>().named("libs")
        compileOnly(libsx.findLibrary("packetevents-api").get())
        compileOnly(libsx.findLibrary("packetevents-fabric").get())
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
