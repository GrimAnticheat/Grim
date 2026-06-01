import versioning.BuildConfig

val minecraft_version: String by project
val fabric_version: String by project

// Plugin choice rationale:
//   This module uses the short `fabric-loom` plugin (LoomGradlePlugin, the remap
//   variant) with `mappings(intermediary:0.0.0:v2)` — a published empty intermediary
//   stub. Because the stub has zero entries, the named→intermediary remap pass is
//   effectively a no-op, leaving Mojang-named bytecode untouched in remapJar output.
//   This is intentional and matches the practical effect of LoomNoRemap
//   (LoomNoRemapGradlePlugin via the fully-qualified `net.fabricmc.fabric-loom` id)
//   without requiring the different jar/task/configuration plumbing that PE's
//   fabric-official uses. See PE's fabric-official build.gradle.kts for the
//   alternative pattern. Both produce equivalent jars when source contains no
//   intermediary refs, which is the case here (and will remain the case when real
//   26.X-mojmap anticheat code lands — see KNOWN BLOCKERS comment below).
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
    // 26.X status — KNOWN BLOCKERS preventing a functional Grim anticheat engine on
    // this branch (tracked as scaffold until they resolve):
    //   1. FabricMC has not published a tiny intermediary mapping for 26.X. The 0.0.0:v2
    //      stub is the only mapping the maven currently serves.
    //   2. Switching to net.fabricmc.fabric-loom (LoomNoRemap) lets source compile
    //      against the pre-deobfuscated 26.X jar's Mojang names — that works fine for
    //      PE because PE has no fabric-ecosystem deps. Grim has hard deps on
    //      cloud-fabric, fabric-permissions-api, and fabric-api event modules, ALL of
    //      which ship intermediary-named bytecode. With LoomNoRemap there's no
    //      runtime intermediary remap, so those refs are dead.
    //   3. Re-enable in steps once any of: FabricMC publishes a 26.X intermediary;
    //      cloud-fabric / fabric-permissions-api publish 26.X-native builds; or we
    //      write Mojang-name shims for each missing dep.
    mappings("net.fabricmc:intermediary:0.0.0:v2")
    modImplementation(libs.fabric.loader)

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
