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

data class DependencyIdentifier(
    val group: String,
    val name: String,
    val version: String,
    val classifier: String = "" // Add classifier field, default to empty string
) {
    override fun toString() = "$group:$name:$version${if (classifier.isNotEmpty()) ":$classifier" else ""}"
}

// Does not support extracting classifiers but while the fabric standard is ambiguous on whether they can be included
// It does not appear dependency classifiers are in use by any mod I am aware of or can find
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
    embeddedJars.addAll(parseFabricModJson(jarFile, project)) // Retain existing logic for top-level fabric.mod.json parsing
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
                        val fabricModJsonEntry: ZipEntry? = embeddedJar.getEntry("fabric.mod.json")
                        if (fabricModJsonEntry != null) {
                            embeddedJar.getInputStream(fabricModJsonEntry).bufferedReader().use { reader ->
                                val jsonContent = reader.readText()
                                val jsonElement = Json.parseToJsonElement(jsonContent)
                                val jsonObject = jsonElement.jsonObject

                                // Extract id and parse into group
                                val id = jsonObject["id"]?.jsonPrimitive?.content
                                    ?: throw IllegalStateException("Missing 'id' in fabric.mod.json of embedded JAR: $entryName")
                                val idWithDots = id.replace("_", ".")
                                val lastDotIndex = idWithDots.lastIndexOf(".")
                                val group = if (lastDotIndex != -1) idWithDots.take(lastDotIndex) else ""

                                // Extract name and version from the JAR filename
                                val jarName = entryName.substringAfterLast("/").removeSuffix(".jar")
                                val (name, version) = parseNameAndVersionFromJarName(jarName)
                                    ?: throw IllegalStateException("Could not parse name and version from JAR filename: $jarName")

                                // Create DependencyIdentifier (classifier is empty for now)
                                val depId = DependencyIdentifier(group, name, version)
                                embeddedJars.add(depId)
                                project.logger.debug("Identified embedded dependency from fabric.mod.json and filename: {}", depId)
                            }
                        } else {
                            project.logger.error("No fabric.mod.json found in embedded JAR: {}. Skipping.", entryName)
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

fun parseNameAndVersionFromJarName(jarName: String): Pair<String, String>? {
    val versionStartRegex = Regex("-(?=\\d)")
    // Use regex to find the position of the hyphen followed by a digit
    val versionStartMatch = versionStartRegex.find(jarName) ?: return null
    val versionStartIndex = versionStartMatch.range.first

    // Split the JAR name into name and version at the version start index
    val name = jarName.take(versionStartIndex)
    val version = jarName.substring(versionStartIndex + 1) // Skip the hyphen

    return if (name.isNotEmpty() && version.isNotEmpty()) {
        Pair(name, version)
    } else {
        null
    }
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
    // Extract classifier from resolved dependency (if available)
    val classifier = dependency.moduleArtifacts.firstOrNull()?.classifier ?: ""
    val depId = DependencyIdentifier(group, name, version, classifier)

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

    if (allEmbeddedDependencies.any { it.name == name && it.version == version && it.classifier == classifier }) {
        project.logger.debug("Excluding dependency already embedded in another JAR: {}", depId)
        return true
    }

    if (actuallyIncludedDependencies.any { it.name == name && it.version == version && it.classifier == classifier }) {
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
    val classifier = dependency.moduleArtifacts.firstOrNull()?.classifier ?: ""
    project.logger.debug(
        "Checking JIJ target: {}",
        DependencyIdentifier(
            dependency.moduleGroup,
            dependency.moduleName,
            dependency.moduleVersion,
            classifier
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
        // Include dependency with classifier (if present)
        "include"("${depId.group}:${depId.name}:${depId.version}${if (depId.classifier.isNotEmpty()) ":${depId.classifier}" else ""}") {
            allEmbeddedDependencies.forEach { embeddedDep ->
                if (embeddedDep.group.isNotEmpty()) {
                    exclude(group = embeddedDep.group, module = embeddedDep.name)
                } else {
                    exclude(module = embeddedDep.name)
                }
                project.logger.debug("Excluding embedded transitive dependency: ${embeddedDep.name}:${embeddedDep.version}${if (embeddedDep.classifier.isNotEmpty()) ":${embeddedDep.classifier}" else ""}")
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
        val classifier = dep.moduleArtifacts.firstOrNull()?.classifier ?: ""
        val depKey = "${dep.moduleGroup}:${dep.moduleName}:${dep.moduleVersion}${if (classifier.isNotEmpty()) ":$classifier" else ""}"
        val depId = DependencyIdentifier(dep.moduleGroup, dep.moduleName, dep.moduleVersion, classifier)
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

    fun collectAllEmbeddedDependenciesForAfterEvaluate(dependencies: Set<ResolvedDependency>) {
        dependencies.forEach { dep ->
            val classifier = dep.moduleArtifacts.firstOrNull()?.classifier ?: ""
            val depKey = "${dep.moduleGroup}:${dep.moduleName}:${dep.moduleVersion}${if (classifier.isNotEmpty()) ":$classifier" else ""}"
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
                collectAllEmbeddedDependenciesForAfterEvaluate(dep.children)
            }
        }
    }

    processed.clear()
    collectAllEmbeddedDependenciesForAfterEvaluate(resolvedDependencies)

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

        fun collectAllEmbeddedDependenciesForJar(dependencies: Set<ResolvedDependency>) {
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
                    collectAllEmbeddedDependenciesForJar(dep.children)
                }
            }
        }

        processed.clear()
        collectAllEmbeddedDependenciesForJar(resolvedDependencies)

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
