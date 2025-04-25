plugins {
    `java-library`
    id("io.freefair.lombok")
    id("com.diffplug.spotless")
}

group = rootProject.group
version = rootProject.version
description = rootProject.description

// Java compilation settings
java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
    disableAutoTargetJvm()
    withSourcesJar()
    withJavadocJar()
}

// Lombok configuration
//lombok {
//    version.set("1.18.30") // Use the version from your version catalog if available
//}

// Spotless configuration
spotless {
    java {
        endWithNewline()
        indentWithSpaces(4)
        removeUnusedImports()
        trimTrailingWhitespace()
        targetExclude("build/generated/**/*")
    }

    kotlinGradle {
        endWithNewline()
        indentWithSpaces(4)
        trimTrailingWhitespace()
    }
}


tasks {
    withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
        options.release.set(17)
    }

    build {
        // Ensure spotlessApply runs before build
        dependsOn(tasks.named("spotlessApply"))
    }

    // Process resources (e.g., for plugin metadata files)
    processResources {
        inputs.property("version", project.version)
        filesMatching(listOf( // "plugin.yml",  Bukkit variable expansion taken care of by net.minecrell.plugin-yml.bukkit
            "bungee.yml", "velocity-plugin.json", "fabric.mod.json")
        ) {
            expand("version" to project.version)
        }
    }

    javadoc {
        title = "${rootProject.name}-${project.name} v${rootProject.version}"
        options.encoding = "UTF-8"
        options.overview =
            rootProject.file("buildSrc/src/main/resources/javadoc-overview.html").toString()
        setDestinationDir(file("${project.layout.buildDirectory.asFile.get()}/docs/javadoc"))
        options {
            (this as CoreJavadocOptions).addBooleanOption("Xdoclint:none", true)
        }
    }
}

// Default tasks
defaultTasks("build")
