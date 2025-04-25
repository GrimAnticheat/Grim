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
        }.takeIf { it.isNotEmpty() }?.joinToString("-")

        return buildString {
            append(baseVersion)
            append("-")
            branch?.let { append("$it-") }
            append(commitHash)
            modifiers?.let { append("+$modifiers") }
        }
    }

    /**
     * Retrieves the current Git commit as a short hash.
     */
    private fun getGitCommitHash(): String {
        val stdout = ByteArrayOutputStream()
        ProcessBuilder("git", "rev-parse", "--short", "HEAD")
            .redirectErrorStream(true)
            .start()
            .apply { waitFor() }
            .inputStream
            .use { stdout.writeBytes(it.readAllBytes()) }
        return stdout.toString().trim()
    }

    /**
     * Returns the current Git branch, sanitised for use in file names.
     * If the branch is "main" or "2.0", returns null.
     *
     * Any slash (/) in the branch name is replaced with an underscore (_)
     * to avoid filesystem issues.
     */
    private fun getGitBranch(): String? {
        val stdout = ByteArrayOutputStream()

        ProcessBuilder("git", "rev-parse", "--abbrev-ref", "HEAD")
            .redirectErrorStream(true)
            .start()
            .apply { waitFor() }
            .inputStream.use { stdout.writeBytes(it.readAllBytes()) }

        val branch = stdout.toString().trim()

        return when (branch) {
            "main", "2.0" -> null                    // ← ignore these branches
            else           -> branch.replace("/", "_")
        }
    }
}