import net.minecrell.pluginyml.bukkit.BukkitPluginDescription.Permission
import versioning.BuildConfig

plugins {
    `maven-publish`
    grim.`base-conventions`
    grim.`shadow-conventions`
    id("de.eldoria.plugin-yml.bukkit") version "0.8.0"
    id("xyz.jpenilla.run-paper") version "3.0.0-beta.1"
}

repositories {
    // 1. Fallback for non-exclusive deps (e.g. Maven Central deps)
    if (BuildConfig.mavenLocalOverride) mavenLocal()

    // 2. Exclusive Repositories (One HTTP request per dep)
    exclusive("https://repo.papermc.io/repository/maven-public/", { name = "papermc" }) {
        includeGroup("io.papermc.paper")
        includeGroup("net.md-5")
    }

    exclusive("https://libraries.minecraft.net", { mavenContent { releasesOnly() } }) {
        includeModule("com.mojang", "brigadier")
    }

    exclusive("https://repo.extendedclip.com/content/repositories/placeholderapi/") {
        includeGroup("me.clip")
    }

    exclusive("https://repo.grim.ac/snapshots") {
        includeGroup("ac.grim.grimac")
        includeGroup("com.github.retrooper")
    }

    exclusive("https://nexus.scarsz.me/content/repositories/releases", { mavenContent { releasesOnly() } }) {
        includeGroup("github.scarsz")
    }

    mavenCentral()
}


dependencies {
    compileOnly(libs.paper.api)
    compileOnly(libs.placeholderapi)

    if (BuildConfig.shadePE) {
        implementation(libs.packetevents.spigot)
    } else {
        compileOnly(libs.packetevents.spigot)
    }
    implementation(libs.cloud.paper)
    implementation(libs.adventure.platform.bukkit)
    implementation(libs.grim.bukkit.internal)

    implementation(project(":common"))
    shadow(project(":common"))
}

bukkit {
    name = "GrimAC"
    author = "GrimAC"
    main = "ac.grim.grimac.platform.bukkit.GrimACBukkitLoaderPlugin"
    website = "https://grim.ac/"
    apiVersion = "1.13"
    foliaSupported = true

    if (!BuildConfig.shadePE) {
        depend = listOf("packetevents")
    }

    softDepend = listOf(
        "ProtocolLib",
        "ProtocolSupport",
        "Essentials",
        "ViaVersion",
        "ViaBackwards",
        "ViaRewind",
        "Geyser-Spigot",
        "floodgate",
        "FastLogin",
        "PlaceholderAPI",
    )

    permissions {
        register("grim.alerts") {
            description = "Receive alerts for violations"
            default = Permission.Default.OP
        }

        register("grim.alerts.enable-on-join") {
            description = "Enable alerts on join"
            default = Permission.Default.OP
        }

        register("grim.performance") {
            description = "Check performance metrics"
            default = Permission.Default.OP
        }

        register("grim.profile") {
            description = "Check user profile"
            default = Permission.Default.OP
        }

        register("grim.brand") {
            description = "Show client brands on join"
            default = Permission.Default.OP
        }

        register("grim.brand.enable-on-join") {
            description = "Enable showing client brands on join"
            default = Permission.Default.OP
        }

        register("grim.sendalert") {
            description = "Send cheater alert"
            default = Permission.Default.OP
        }

        register("grim.nosetback") {
            description = "Disable setback"
            default = Permission.Default.FALSE
        }

        register("grim.nomodifypacket") {
            description = "Disable modifying packets"
            default = Permission.Default.FALSE
        }

        register("grim.exempt") {
            description = "Exempt from all checks"
            default = Permission.Default.FALSE
        }

        register("grim.verbose") {
            description = "Receive verbose alerts for violations"
            default = Permission.Default.OP
        }

        register("grim.verbose.enable-on-join") {
            description =
                "Enable verbose alerts on join"
            default = Permission.Default.FALSE
        }

        register("grim.list") {
            description =
                "Shows lists of specific data"
            default = Permission.Default.FALSE
        }

    }
}

publishing.publications.create<MavenPublication>("maven") {
    artifact(tasks["shadowJar"])
}

tasks {
    runServer {
        minecraftVersion("1.21.11")
    }

    shadowJar {
        manifest {
            attributes["paperweight-mappings-namespace"] = "mojang"
        }
    }
}
