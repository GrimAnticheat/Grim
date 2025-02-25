import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ResolvedDependency
import org.gradle.kotlin.dsl.dependencies

plugins {
    `java-library` // Ensure basic Java support
}

// Create a configuration for JIJ dependencies
val jijDependencies: Configuration by project.configurations.creating {
    isCanBeResolved = true
    isCanBeConsumed = false
    // Extend from both implementation and modImplementation (Fabric-specific)
    extendsFrom(project.configurations.getByName("implementation"))
    project.configurations.findByName("modImplementation")?.let { extendsFrom(it) }
}

// Function to determine if a dependency should be included as JIJ
fun isJijTarget(dependency: ResolvedDependency): Boolean {
    val group = dependency.moduleGroup
    val name = dependency.moduleName
    val version = dependency.moduleVersion
    project.logger.lifecycle("Checking JIJ target: $group:$name:$version")

    // Exclude dependencies that are typically provided by the platform (e.g., Minecraft, Fabric, etc.)
    if (group.startsWith("net.minecraft") || group.startsWith("net.fabricmc") || name == "minecraft" || name == "fabric-loader") {
        project.logger.lifecycle("Excluding platform dependency: $group:$name:$version")
        return false
    }

    // Exclude BOM files (e.g., dependencies with names ending in "-bom")
    if (name.endsWith("-bom")) {
        project.logger.lifecycle("Excluding BOM file: $group:$name:$version")
        return false
    }

    return true
}

// Function to process dependencies recursively (including transitive dependencies)
fun processDependencies(
    dependencies: Set<ResolvedDependency>,
    processed: MutableSet<String>,
    project: Project
) {
    dependencies.forEach { dep: ResolvedDependency ->
        val depKey = "${dep.moduleGroup}:${dep.moduleName}:${dep.moduleVersion}"
        if (!processed.contains(depKey)) {
            processed.add(depKey)
            project.logger.lifecycle("Resolved dependency: ${dep.moduleGroup}:${dep.moduleName}:${dep.moduleVersion}")

            if (isJijTarget(dep)) {
                project.logger.lifecycle("Processing JIJ dependency: ${dep.moduleGroup}:${dep.moduleName}:${dep.moduleVersion}")

                // Check if the dependency is a project dependency (e.g., :common)
                val isProjectDependency = project.rootProject.allprojects.any { subproject ->
                    subproject.name == dep.moduleName && (subproject.group == dep.moduleGroup || dep.moduleGroup.isEmpty())
                }

                if (isProjectDependency) {
                    // Include project dependencies using project notation
                    val projectPath = project.rootProject.allprojects.find { subproject ->
                        subproject.name == dep.moduleName && (subproject.group == dep.moduleGroup || dep.moduleGroup.isEmpty())
                    }?.path ?: throw IllegalStateException("Project dependency not found: ${dep.moduleName}")
                    project.logger.lifecycle("Including project dependency as JIJ: $projectPath")
                    project.dependencies {
                        "include"(project.project(projectPath))
                    }
                } else {
                    // Include external dependencies using group:name:version notation
                    project.logger.lifecycle("Including external dependency as JIJ: ${dep.moduleGroup}:${dep.moduleName}:${dep.moduleVersion}")
                    project.dependencies {
                        "include"("${dep.moduleGroup}:${dep.moduleName}:${dep.moduleVersion}")
                    }
                }
            }

            // Process transitive dependencies
            processDependencies(dep.children, processed, project)
        }
    }
}

// Resolve dependencies and include them as JIJ during the configuration phase
project.afterEvaluate {
    // Resolve the jijDependencies configuration early
    project.logger.lifecycle("Resolving jijDependencies configuration")
    val resolvedDependencies = jijDependencies.resolvedConfiguration.firstLevelModuleDependencies

    // Include dependencies as JIJ (including transitive dependencies)
    val processed = mutableSetOf<String>()
    processDependencies(resolvedDependencies, processed, project)
}

// Apply JIJ for dependencies in the JAR
tasks.withType<Jar>().configureEach {
    // Log the resolved dependencies for debugging
    doFirst {
        project.logger.lifecycle("Resolving JIJ dependencies for JAR inclusion")
        val resolvedDependencies = jijDependencies.resolvedConfiguration.firstLevelModuleDependencies
        val processed = mutableSetOf<String>()
        fun logDependencies(dependencies: Set<ResolvedDependency>) {
            dependencies.forEach { dep ->
                val depKey = "${dep.moduleGroup}:${dep.moduleName}:${dep.moduleVersion}"
                if (!processed.contains(depKey)) {
                    processed.add(depKey)
                    project.logger.lifecycle("Resolved JIJ dependency for JAR: ${dep.moduleGroup}:${dep.moduleName}:${dep.moduleVersion}")
                    if (isJijTarget(dep)) {
                        project.logger.lifecycle("Including JIJ dependency in JAR: ${dep.moduleGroup}:${dep.moduleName}:${dep.moduleVersion}")
                    }
                    logDependencies(dep.children)
                }
            }
        }
        logDependencies(resolvedDependencies)
    }
}
