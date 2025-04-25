package versioning

/**
 * BuildConfig provides access to user-defined build flags that control how a GrimAC
 * build is assembled. These flags are set via Gradle project properties or environment variables.
 *
 * You can use these flags to enable/disable features like shading, relocation, or release mode.
 *
 * Usage (command line):
 * ```
 * ./gradlew build -PshadePE=true -Prelocate=false -Prelease=true
 * ```
 *
 * Or using environment variables:
 * ```
 * SHADE_PE=true RELOCATE_JAR=false RELEASE=true ./gradlew build
 * ```
 *
 * @property shadePE If true, shades PacketEvents into the jar. Default: true.
 * @property relocate If true, relocates shaded dependencies to avoid conflicts. Default: true.
 * @property release If true, omits commit hash and modifiers from version string. Default: false.
 */
object BuildConfig {
    val shadePE: Boolean
        get() = System.getProperty("shadePE")?.toBoolean()
            ?: System.getenv("SHADE_PE")?.toBoolean()
            ?: true

    val relocate: Boolean
        get() = System.getProperty("relocate")?.toBoolean()
            ?: System.getenv("RELOCATE_JAR")?.toBoolean()
            ?: true

    val release: Boolean
        get() = System.getProperty("release")?.toBoolean()
            ?: System.getenv("RELEASE")?.toBoolean()
            ?: false
}