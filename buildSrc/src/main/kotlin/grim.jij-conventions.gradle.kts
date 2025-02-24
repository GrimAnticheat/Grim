import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.Dependency
import org.gradle.kotlin.dsl.dependencies

plugins {
    `java-library` // Ensure basic Java support
}

// Create a configuration for JIJ dependencies
val jijDependencies: Configuration by project.configurations.creating {
    isCanBeResolved = true
    isCanBeConsumed = false
    extendsFrom(project.configurations.getByName("implementation"))
}

// Function to determine if a dependency should be included as JIJ
fun isJijTarget(dependency: Dependency): Boolean {
    // Exclude dependencies that are typically provided by the platform (e.g., Minecraft, Fabric, etc.)
    // This can be extended by platforms to exclude additional dependencies
    val group = dependency.group ?: return true
    return !group.startsWith("net.minecraft") &&
            !group.startsWith("net.fabricmc") &&
            dependency.name != "minecraft" &&
            dependency.name != "fabric-loader" &&
            dependency.name != "common"
}

// Apply JIJ for dependencies
tasks.withType<Jar>().configureEach {
    // Ensure the JIJ configuration is resolved
    from(jijDependencies) {
        into("META-INF/jars") // Standard location for JIJ dependencies
    }
}

// Include dependencies as JIJ
dependencies {
    jijDependencies.allDependencies.forEach { dep ->
        if (dep.group != null && dep.name != null) {
            if (isJijTarget(dep)) {
                "include"(dep)
            }
        }
    }

    // Include project dependencies (e.g., :common)
    project.dependencies {
        "include"(project(":common"))
    }
}
