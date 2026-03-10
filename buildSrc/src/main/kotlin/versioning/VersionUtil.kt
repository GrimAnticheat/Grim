package versioning

import java.io.ByteArrayOutputStream

/**
 * Utility for computing the version string of GrimAC artifacts.
 *
 * The version string is constructed based on:
 * - A base semantic version (e.g., "2.3.72")
 * - Git commit hash (unless in release mode)
 * - Git branch name (unless in release mode or main)
 * - Modifiers (e.g., lite, no_relocate) for non-default build configurations
 *
 * Example outputs:
 * - `2.3.72` (release build)
 * - `2.3.72-a4f8b21+lite` (preview build without PE shading)
 * - `2.3.72-feature_branch-a4f8b21+lite-no_relocate`
 *
 * @see BuildConfig for controlling the release/modifier behavior
 */
object VersionUtil {

    /**
     * Computes the full version string for the build.
     *
     * @param baseVersion The base semantic version (e.g., "2.3.72")
     * @return Full version string including commit hash, branch, and modifiers if applicable
     */
    fun computeVersion(baseVersion: String): String {
        if (BuildConfig.release) {
            return baseVersion
        }

        val commitHash = getGitCommitHash()
        val branch = getGitBranch()

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

    /**
     * Retrieves the current Git commit
     */
    fun getGitCommitHash(full: Boolean = false): String {
        val stdout = ByteArrayOutputStream()
        val command = listOfNotNull("git", "rev-parse", if (full) null else "--short", "HEAD")
        ProcessBuilder(command)
            .redirectErrorStream(true)
            .start()
            .apply { waitFor() }
            .inputStream
            .use { stdout.writeBytes(it.readAllBytes()) }
        val fullCommit = stdout.toString().trim()
        return fullCommit.take(minOf(fullCommit.length, 7))
    }

    /**
     * Returns the current Git branch, sanitised for use in file names.
     * If the branch is "main" or "2.0", returns null.
     *
     * Any slash (/) in the branch name is replaced with an underscore (_)
     * to avoid filesystem issues.
     */
    fun getGitBranch(raw: Boolean = false): String? {
        val stdout = ByteArrayOutputStream()

        ProcessBuilder("git", "rev-parse", "--abbrev-ref", "HEAD")
            .redirectErrorStream(true)
            .start()
            .apply { waitFor() }
            .inputStream.use { stdout.writeBytes(it.readAllBytes()) }

        if (raw) return stdout.toString().trim()

        val branch = stdout.toString().trim()
            .replace(Regex("[^a-zA-Z0-9_.-]+"), "_")
            .replace(Regex("_{2,}"), "_")
            .trim(' ', '.', '_', '-')
            .removePrefix("heads_")

        val mainBranch = System.getenv("GRIM_MAIN_BRANCH") ?: "2.0"

        return when (branch) {
            "main", mainBranch -> null                    // ← ignore these branches
            else -> branch
        }
    }

    fun getGitUser(): String {
        try {
            val stdout = ByteArrayOutputStream()
            ProcessBuilder("git", "config", "user.name")
                .redirectErrorStream(true)
                .start()
                .apply { waitFor() }
                .inputStream.use { stdout.writeBytes(it.readAllBytes()) }
            return stdout.toString().trim()
        } catch (_: Exception) {
            return "unknown"
        }
    }

}
