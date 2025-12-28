import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import org.gradle.api.artifacts.repositories.InclusiveRepositoryContentDescriptor
import org.gradle.kotlin.dsl.maven
import versioning.BuildConfig

/**
 * Registers a repository with strict content exclusivity.
 *
 * Logic:
 * 1. If BuildConfig.mavenLocalOverride is true, checks Maven Local first.
 * 2. Checks the provided remote URL second.
 * 3. Enforces strict filtering so Gradle never checks this repo for other deps,
 *    and never checks other repos (like Central) for these deps.
 */
fun RepositoryHandler.exclusive(
    url: String,
    repoConfig: MavenArtifactRepository.() -> Unit = {},
    filterConfig: InclusiveRepositoryContentDescriptor.() -> Unit
) {
    // We define the repositories inside the function scope so they are added to the handler
    val remote = maven(url, repoConfig)

    exclusive(remote, filterConfig)
}

fun RepositoryHandler.exclusive(
    remote: MavenArtifactRepository,
    filterConfig: InclusiveRepositoryContentDescriptor.() -> Unit
) {
    // We access BuildConfig here again to check for local override
    val local = if (BuildConfig.mavenLocalOverride) mavenLocal() else null

    exclusiveContent {
        if (local != null) {
            forRepositories(local, remote)
        } else {
            forRepositories(remote)
        }
        filter(filterConfig)
    }
}
