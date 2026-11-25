// Local developer overrides, including private Maven credentials.
run {
    val userProps = rootDir.resolve("gradle.user.properties")
    if (userProps.isFile) {
        val loaded = java.util.Properties()
        userProps.inputStream().use { loaded.load(it) }
        for ((key, value) in loaded) {
            System.setProperty(key.toString(), value.toString())
        }
    }
}

dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            from(files("libs.versions.toml"))
        }

        create("testlibs") {
            from(files("testlibs.versions.toml"))
        }
    }
}

pluginManagement {
    repositories {
        // For the Fabric Loom plugin
        exclusiveContent {
            forRepository {
                maven {
                    name = "FabricMC"
                    url = uri("https://maven.fabricmc.net/")
                }
            }
            filter {
                includeModule("fabric-loom", "fabric-loom.gradle.plugin")
                includeGroupByRegex("net.fabricmc.*")
            }
        }

        gradlePluginPortal()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
    id("com.gradle.develocity") version "4.2.1" apply false
}

if (gradle.startParameter.isBuildScan) {
    apply(plugin = "com.gradle.develocity")
    develocity {
        buildScan {
            // This is the magic part that bypasses the interactive "yes/no" prompt
            termsOfUseUrl = "https://gradle.com/terms-of-service"
            termsOfUseAgree = "yes"

            // Best practice for CI: ensure the scan finishes uploading before the step completes
            uploadInBackground = false

            // Automatically add useful tags and links to the scan
            if (System.getenv("CI") == "true") {
                tag("CI")
                link(
                    "GitHub Actions build",
                    System.getenv("GITHUB_SERVER_URL") + "/" + System.getenv("GITHUB_REPOSITORY") + "/actions/runs/" + System.getenv(
                        "GITHUB_RUN_ID"
                    )
                )
            }
        }
    }
}

rootProject.name = "grimac"
include("common")
include("bukkit")
include("fabric")
include(":fabric:shared")
include(":fabric:intermediary")
include(":fabric:intermediary:mc1161")
include(":fabric:intermediary:mc1171")
include(":fabric:intermediary:mc1194")
include(":fabric:intermediary:mc1205")
include(":fabric:intermediary:mc12111")
include(":fabric:official")
include(":fabric:official:mc261")

if (file("workspace.gradle.kts").exists()) apply(from = "workspace.gradle.kts")
