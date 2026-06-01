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

loom {
    accessWidenerPath = file("src/main/resources/grimac.accesswidener")
}

dependencies {
    minecraft("com.mojang:minecraft:$minecraft_version")
    // 26.X anticheat lives here. Compiles directly against the Mojang-named
    // 26.1.2 jar via the empty `intermediary:0.0.0:v2` stub (the named→
    // intermediary remap is a no-op since the stub has zero entries). Source
    // is the fabric-intermediary platform layer with the intermediary-bound
    // surface stripped:
    //
    //   - cloud-fabric / fabric-permissions-api / fabric-api event modules
    //     all ship intermediary-bound bytecode that won't link against 26.X
    //     Mojang names. They are NOT on the classpath. /grim commands and
    //     fabric-permissions-api lookups are no-op on this build by design
    //     (matches the catch path the intermediary chain takes when cloud
    //     is unavailable on older MC).
    //   - Server lifecycle / tick events are driven by MinecraftServerMixin
    //     into FabricServerEvents (see src/main/java/.../FabricServerEvents.java)
    //     replacing fabric-api's ServerLifecycleEvents + ServerTickEvents.
    //   - 26.X mojmap drift is handled inline (Permission.HasCommandLevel,
    //     services().profileResolver(), Inventory.getSelectedItem(),
    //     ResourceKey.identifier(), Player.sendSystemMessage, etc.). AW
    //     widens the same private fields the intermediary side does.
    //
    // mc261 covers the full 26.1.X family — mojmap is empirically signature-
    // stable across 26.1 / 26.1.1 / 26.1.2 (0 of 300 random classes drift,
    // 6 of 6 critical classes bit-identical). When 26.2 ships a release a
    // sibling mc262 breakpoint joins it.
    mappings("net.fabricmc:intermediary:0.0.0:v2")
    modImplementation(libs.fabric.loader)

    implementation(project(":common"))
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
