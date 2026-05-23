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
    // Mojang stopped publishing official mappings starting MC 26.X and Yarn has no 26.X
    // build either; Fabric intermediary publishes the empty `0.0.0` mapping for it.
    // fabric-official is currently a structural stub that loads on 26.X but cannot
    // reference meaningful MC symbols until usable mappings land.
    mappings("net.fabricmc:intermediary:0.0.0:v2")
    modImplementation(libs.fabric.loader)

    // PE is intentionally NOT a dep here: PE's accessWidener is intermediary-namespaced
    // and conflicts with the intermediary 0.0.0 mappings used by fabric-official.
    // Bring PE back in once usable 26.X mappings exist and we can compile against named.
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
            archiveBaseName = "${rootProject.name}-fabric${if (project.name != "fabric-official") "-${project.name}" else "-official"}"
            archiveVersion = rootProject.version as String
        }
    }
}

subprojects {
    dependencies {
        implementation(project(":fabric-official", configuration = "namedElements"))
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
