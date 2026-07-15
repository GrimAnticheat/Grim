package versioning

import org.gradle.api.Project

/**
 * Utility for computing the version string of GrimAC artifacts.
 *
 * Uses Gradle's providers.exec for git invocations so that
 * org.gradle.configuration-cache=true can serialize the task graph.
 * Each helper takes a Project so the git workingDir is anchored to the
 * project root (was ambient JVM cwd before; flaky when invoked from
 * the workspace composite root).
 */
object VersionUtil {

    fun computeVersion(project: Project, baseVersion: String): String {
        if (BuildConfig.release) {
            return baseVersion
        }

        val commitHash = getGitCommitHash(project)
        val branch = getGitBranch(project)

        val modifiers = buildList {
            if (!BuildConfig.shadePE) add("lite")
            if (!BuildConfig.relocate) add("no_relocate")
        }.joinToString("-").takeIf { it.isNotEmpty() }

        return buildString {
            append(baseVersion)
            append("-")
            branch?.let { append("$it-") }
            append(commitHash)
            modifiers?.let { append("+$it") }
        }
    }

    fun getGitCommitHash(project: Project, full: Boolean = false): String {
        return try {
            val args = if (full) listOf("git", "rev-parse", "HEAD")
                       else listOf("git", "rev-parse", "--short", "HEAD")
            val out = project.providers.exec {
                commandLine(args)
                workingDir(project.projectDir)
                isIgnoreExitValue = true
            }.standardOutput.asText.get().trim()
            out.take(minOf(out.length, 7))
        } catch (e: Exception) {
            "unknown"
        }
    }

    fun getGitBranch(project: Project, raw: Boolean = false): String? {
        val rawBranch = try {
            project.providers.exec {
                commandLine("git", "rev-parse", "--abbrev-ref", "HEAD")
                workingDir(project.projectDir)
                isIgnoreExitValue = true
            }.standardOutput.asText.get().trim()
        } catch (e: Exception) {
            return null
        }

        if (raw) return rawBranch

        val branch = rawBranch
            .replace(Regex("[^a-zA-Z0-9_.-]+"), "_")
            .replace(Regex("_{2,}"), "_")
            .trim(' ', '.', '_', '-')
            .removePrefix("heads_")

        val mainBranch = System.getenv("GRIM_MAIN_BRANCH") ?: "2.0"

        return when (branch) {
            "main", mainBranch -> null
            else -> branch
        }
    }

    fun getGitUser(project: Project): String {
        return try {
            project.providers.exec {
                commandLine("git", "config", "user.name")
                workingDir(project.projectDir)
                isIgnoreExitValue = true
            }.standardOutput.asText.get().trim().ifEmpty { "unknown" }
        } catch (_: Exception) {
            "unknown"
        }
    }

}
