package buildlogic

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import java.util.zip.ZipFile

abstract class VerifyApiPackaging : DefaultTask() {
    @get:Classpath
    abstract val apiArtifacts: ConfigurableFileCollection

    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val pluginJar: RegularFileProperty

    @TaskAction
    fun verify() {
        val api = apiArtifacts.files.singleOrNull()
            ?: throw GradleException("Expected exactly one resolved GrimAPI jar: ${apiArtifacts.files}")
        ZipFile(api).use { reference ->
            ZipFile(pluginJar.get().asFile).use { plugin ->
                val classes = reference.entries().asSequence()
                    .map { it.name }
                    .filter { it.startsWith("ac/grim/grimac/api/") && it.endsWith(".class") }
                    .toList()
                if (classes.isEmpty()) throw GradleException("Resolved GrimAPI jar contains no API classes")
                val missing = classes.filter { plugin.getEntry(it) == null }
                if (missing.isNotEmpty()) {
                    throw GradleException("Plugin jar is missing public API classes:\n" + missing.joinToString("\n"))
                }
                logger.lifecycle("Verified all ${classes.size} GrimAPI classes in ${pluginJar.get().asFile.name}")
            }
        }
    }
}
