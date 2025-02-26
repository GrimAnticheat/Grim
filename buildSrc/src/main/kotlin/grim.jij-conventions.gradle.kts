import kotlinx.serialization.json.*
import java.util.jar.JarFile
import java.util.zip.ZipEntry

plugins {
    `java-library`
}

val jijDependencies: Configuration by project.configurations.creating {
    isCanBeResolved = true
    isCanBeConsumed = false
    extendsFrom(project.configurations.getByName("implementation"))
    project.configurations.findByName("modImplementation")?.let { extendsFrom(it) }
}

data class DependencyIdentifier(val group: String, val name: String, val version: String) {
    override fun toString() = "$group:$name:$version"
}

fun parseFabricModJson(jarFile: File, project: Project): Set<DependencyIdentifier> {
    val nestedJars = mutableSetOf<DependencyIdentifier>()
    try {
        JarFile(jarFile).use { jar ->
            val fabricModJsonEntry: ZipEntry? = jar.getEntry("fabric.mod.json")
            if (fabricModJsonEntry != null) {
                jar.getInputStream(fabricModJsonEntry).bufferedReader().use { reader ->
                    val jsonContent = reader.readText()
                    val jsonElement = Json.parseToJsonElement(jsonContent)
                    val jsonObject = jsonElement.jsonObject
                    val jarsArray = jsonObject["jars"]?.jsonArray ?: return@use
                    jarsArray.forEach { jarEntry: JsonElement ->
                        val filePath =
                            jarEntry.jsonObject["file"]?.jsonPrimitive?.content ?: return@forEach
                        project.logger.debug(
                            "Found nested JAR in fabric.mod.json: {} in {}",
                            filePath,
                            jarFile
                        )
                        val jarName = filePath.substringAfterLast("/")
                        val parts = jarName.removeSuffix(".jar").split("-")
                        if (parts.size >= 2) {
                            val inferredName = parts.dropLast(1).joinToString("-")
                            val inferredVersion = parts.last()
                            nestedJars.add(DependencyIdentifier("", inferredName, inferredVersion))
                            project.logger.debug("Inferred nested dependency from fabric.mod.json: :$inferredName:$inferredVersion")
                        }
                    }
                }
            }
        }
    } catch (e: Exception) {
        project.logger.error("Failed to parse fabric.mod.json in $jarFile: ${e.message}")
    }
    return nestedJars
}

fun extractEmbeddedJars(jarFile: File, project: Project): Set<DependencyIdentifier> {
    val embeddedJars = mutableSetOf<DependencyIdentifier>()
    embeddedJars.addAll(parseFabricModJson(jarFile, project))
    try {
        JarFile(jarFile).use { jar ->
            val entries = jar.entries()
            while (entries.hasMoreElements()) {
                val entry = entries.nextElement()
                val entryName = entry.name
                if (entryName.endsWith(".jar") && (entryName.startsWith("META-INF/jarjar/") || entryName.startsWith(
                        "libs/"
                    ) || entryName.startsWith("META-INF/jars/"))
                ) {
                    project.logger.debug("Found embedded JAR: {} in {}", entryName, jarFile)
                    val tempFile = File.createTempFile("embedded-", ".jar")
                    tempFile.deleteOnExit()
                    jar.getInputStream(entry).use { input ->
                        tempFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                    JarFile(tempFile).use { embeddedJar ->
                        val manifest = embeddedJar.manifest
                        if (manifest != null) {
                            val group =
                                manifest.mainAttributes.getValue("Implementation-Vendor") ?: ""
                            val name = manifest.mainAttributes.getValue("Implementation-Title")
                                ?: embeddedJar.name
                            val version =
                                manifest.mainAttributes.getValue("Implementation-Version") ?: ""
                            if (group.isNotEmpty() && name.isNotEmpty() && version.isNotEmpty()) {
                                embeddedJars.add(DependencyIdentifier(group, name, version))
                                project.logger.debug("Identified embedded dependency: $group:$name:$version")
                            } else {
                                val jarName = entryName.substringAfterLast("/")
                                val parts = jarName.removeSuffix(".jar").split("-")
                                if (parts.size >= 2) {
                                    val inferredName = parts.dropLast(1).joinToString("-")
                                    val inferredVersion = parts.last()
                                    embeddedJars.add(
                                        DependencyIdentifier(
                                            "",
                                            inferredName,
                                            inferredVersion
                                        )
                                    )
                                    project.logger.debug("Inferred embedded dependency from filename: :$inferredName:$inferredVersion")
                                }
                            }
                        }
                    }
                }
            }
        }
    } catch (e: Exception) {
        project.logger.error("Failed to analyze JAR file $jarFile for embedded JARs: ${e.message}")
    }
    return embeddedJars
}

fun shouldExcludeDependency(
    dependency: ResolvedDependency,
    allEmbeddedDependencies: Set<DependencyIdentifier>,
    actuallyIncludedDependencies: Set<DependencyIdentifier>,
    project: Project
): Boolean {
    val group = dependency.moduleGroup
    val name = dependency.moduleName
    val version = dependency.moduleVersion
    val depId = DependencyIdentifier(group, name, version)

    if (group.startsWith("net.minecraft") ||
        group.startsWith("net.fabricmc") ||
        name == "minecraft" ||
        name == "fabric-loader" ||
        name == "fastutil" ||
        name.startsWith("fabric_") ||
        name.startsWith("fabric-api")
    ) {
        project.logger.debug("Excluding platform dependency: {}", depId)
        return true
    }

    if (name.endsWith("-bom") || name.contains("_bom")) {
        project.logger.debug("Excluding BOM file: {}", depId)
        return true
    }

    if (allEmbeddedDependencies.any { it.name == name && it.version == version }) {
        project.logger.debug("Excluding dependency already embedded in another JAR: {}", depId)
        return true
    }

    if (actuallyIncludedDependencies.any { it.name == name && it.version == version }) {
        project.logger.debug("Excluding duplicate dependency: {}", depId)
        return true
    }

    return false
}

fun isJijTarget(
    dependency: ResolvedDependency,
    allEmbeddedDependencies: Set<DependencyIdentifier>,
    actuallyIncludedDependencies: Set<DependencyIdentifier>,
    project: Project
): Boolean {
    project.logger.debug(
        "Checking JIJ target: {}",
        DependencyIdentifier(
            dependency.moduleGroup,
            dependency.moduleName,
            dependency.moduleVersion
        )
    )
    return !shouldExcludeDependency(
        dependency,
        allEmbeddedDependencies,
        actuallyIncludedDependencies,
        project
    )
}

fun includeDependencyWithExclusions(
    depId: DependencyIdentifier,
    project: Project,
    actuallyIncludedDependencies: MutableSet<DependencyIdentifier>,
    allEmbeddedDependencies: Set<DependencyIdentifier>
) {
    project.logger.debug("Including external dependency as JIJ with dynamic exclusions: {}", depId)
    project.dependencies {
        "include"("$depId") {
            allEmbeddedDependencies.forEach { embeddedDep ->
                if (embeddedDep.group.isNotEmpty()) {
                    exclude(group = embeddedDep.group, module = embeddedDep.name)
                } else {
                    exclude(module = embeddedDep.name)
                }
                project.logger.debug("Excluding embedded transitive dependency: ${embeddedDep.name}:${embeddedDep.version}")
            }
        }
    }
    actuallyIncludedDependencies.add(depId)
}

fun processDependencies(
    dependencies: Set<ResolvedDependency>,
    processed: MutableSet<String>,
    allEmbeddedDependencies: MutableMap<DependencyIdentifier, File>,
    actuallyIncludedDependencies: MutableSet<DependencyIdentifier>,
    project: Project
) {
    dependencies.forEach { dep: ResolvedDependency ->
        val depKey = "${dep.moduleGroup}:${dep.moduleName}:${dep.moduleVersion}"
        val depId = DependencyIdentifier(dep.moduleGroup, dep.moduleName, dep.moduleVersion)
        if (!processed.contains(depKey)) {
            processed.add(depKey)
            project.logger.debug("Resolved dependency: {}", depId)

            val jarFile = dep.moduleArtifacts.firstOrNull()?.file
            if (jarFile != null && jarFile.exists()) {
                val embeddedJars = extractEmbeddedJars(jarFile, project)
                embeddedJars.forEach { embeddedDep ->
                    allEmbeddedDependencies[embeddedDep] = jarFile
                }
            }

            if (isJijTarget(
                    dep,
                    allEmbeddedDependencies.keys,
                    actuallyIncludedDependencies,
                    project
                )
            ) {
                project.logger.debug("Processing JIJ dependency: {}", depId)
                val isProjectDependency = project.rootProject.allprojects.any { subproject ->
                    subproject.name == dep.moduleName && (subproject.group == dep.moduleGroup || dep.moduleGroup.isEmpty())
                }
                if (isProjectDependency) {
                    val projectPath = project.rootProject.allprojects.find { subproject ->
                        subproject.name == dep.moduleName && (subproject.group == dep.moduleGroup || dep.moduleGroup.isEmpty())
                    }?.path
                        ?: throw IllegalStateException("Project dependency not found: ${dep.moduleName}")
                    project.logger.debug("Including project dependency as JIJ: $projectPath")
                    project.dependencies {
                        "include"(project.project(projectPath))
                    }
                    actuallyIncludedDependencies.add(depId)
                } else {
                    includeDependencyWithExclusions(
                        depId,
                        project,
                        actuallyIncludedDependencies,
                        allEmbeddedDependencies.keys
                    )
                }
            }

            processDependencies(
                dep.children,
                processed,
                allEmbeddedDependencies,
                actuallyIncludedDependencies,
                project
            )
        }
    }
}

project.afterEvaluate {
    project.logger.debug("Resolving jijDependencies configuration")
    val resolvedDependencies = jijDependencies.resolvedConfiguration.firstLevelModuleDependencies

    val processed = mutableSetOf<String>()
    val allEmbeddedDependencies = mutableMapOf<DependencyIdentifier, File>()
    val actuallyIncludedDependencies = mutableSetOf<DependencyIdentifier>()

    fun collectAllEmbeddedDependencies(dependencies: Set<ResolvedDependency>) {
        dependencies.forEach { dep ->
            val depKey = "${dep.moduleGroup}:${dep.moduleName}:${dep.moduleVersion}"
            if (!processed.contains(depKey)) {
                processed.add(depKey)
                val jarFile = dep.moduleArtifacts.firstOrNull()?.file
                if (jarFile != null && jarFile.exists()) {
                    val embeddedJars = extractEmbeddedJars(jarFile, project)
                    embeddedJars.forEach { embeddedDep ->
                        allEmbeddedDependencies[embeddedDep] = jarFile
                        project.logger.debug(
                            "Collected embedded dependency: {} from {}",
                            embeddedDep,
                            jarFile
                        )
                    }
                }
                collectAllEmbeddedDependencies(dep.children)
            }
        }
    }

    processed.clear()
    collectAllEmbeddedDependencies(resolvedDependencies)

    processed.clear()
    processDependencies(
        resolvedDependencies,
        processed,
        allEmbeddedDependencies,
        actuallyIncludedDependencies,
        project
    )
}

tasks.withType<Jar>().configureEach {
    doFirst {
        project.logger.debug("Resolving JIJ dependencies for JAR inclusion")
        val resolvedDependencies =
            jijDependencies.resolvedConfiguration.firstLevelModuleDependencies
        val processed = mutableSetOf<String>()
        val allEmbeddedDependencies = mutableMapOf<DependencyIdentifier, File>()
        val actuallyIncludedDependencies = mutableSetOf<DependencyIdentifier>()

        fun collectAllEmbeddedDependencies(dependencies: Set<ResolvedDependency>) {
            dependencies.forEach { dep ->
                val depKey = "${dep.moduleGroup}:${dep.moduleName}:${dep.moduleVersion}"
                if (!processed.contains(depKey)) {
                    processed.add(depKey)
                    val jarFile = dep.moduleArtifacts.firstOrNull()?.file
                    if (jarFile != null && jarFile.exists()) {
                        val embeddedJars = extractEmbeddedJars(jarFile, project)
                        embeddedJars.forEach { embeddedDep ->
                            allEmbeddedDependencies[embeddedDep] = jarFile
                            project.logger.debug(
                                "Collected embedded dependency for JAR: {} from {}",
                                embeddedDep,
                                jarFile
                            )
                        }
                    }
                    collectAllEmbeddedDependencies(dep.children)
                }
            }
        }

        processed.clear()
        collectAllEmbeddedDependencies(resolvedDependencies)

        processed.clear()

        fun logDependencies(dependencies: Set<ResolvedDependency>) {
            dependencies.forEach { dep ->
                val depKey = "${dep.moduleGroup}:${dep.moduleName}:${dep.moduleVersion}"
                val depId = DependencyIdentifier(dep.moduleGroup, dep.moduleName, dep.moduleVersion)
                if (!processed.contains(depKey)) {
                    processed.add(depKey)
                    project.logger.debug("Resolved JIJ dependency for JAR: {}", depId)

                    val jarFile = dep.moduleArtifacts.firstOrNull()?.file
                    if (jarFile != null && jarFile.exists()) {
                        val embeddedJars = extractEmbeddedJars(jarFile, project)
                        embeddedJars.forEach { embeddedDep ->
                            allEmbeddedDependencies[embeddedDep] = jarFile
                        }
                    }

                    if (isJijTarget(
                            dep,
                            allEmbeddedDependencies.keys,
                            actuallyIncludedDependencies,
                            project
                        )
                    ) {
                        project.logger.debug("Including JIJ dependency in JAR: {}", depId)
                        actuallyIncludedDependencies.add(depId)
                    }
                    logDependencies(dep.children)
                }
            }
        }
        logDependencies(resolvedDependencies)
    }
}
