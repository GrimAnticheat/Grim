import versioning.BuildConfig

val minecraft_version: String by project
val fabric_version: String by project

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

dependencies {
    minecraft("com.mojang:minecraft:$minecraft_version")
    // MC 26.X jars are pre-deobfuscated with Mojang's official names, but neither
    // Mojang's manifest nor FabricMC publishes a tiny intermediary mapping for 26.X.
    // The 0.0.0:v2 stub is the only mapping the maven currently serves; Loom configures
    // against it cleanly as long as source code references no MC types. Per-version
    // source compiled against Mojang names lands once a real 26.X intermediary mapping
    // (or first-class no-mapping Loom support) is available.
    mappings("net.fabricmc:intermediary:0.0.0:v2")
    modImplementation(libs.fabric.loader)

    // PE's pure-Java api jar is safe to pull (no accessWidener inside). The fabric
    // variant is intentionally excluded — it ships an intermediary-namespaced AW that
    // Loom would try to remap against the 0.0.0 stub. Source must avoid net.minecraft.*
    // references so Loom's source remap is a no-op.
    compileOnly(libs.packetevents.api)
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
        val libsx = rootProject.extensions.getByType<VersionCatalogsExtension>().named("libs")
        compileOnly(libsx.findLibrary("packetevents-api").get())
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
