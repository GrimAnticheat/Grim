/**
 *          GrimAC Build Configuration
 *
 * Build Flags:
 * -PshadePE=true   - Enables 'lite' mode
 * -Prelocate=false - Adds 'no_relocate' modifier
 * -Prelease=true   - Removes commit/modifiers for release build
 *
 * Logic in: buildSrc/versioning/BuildConfig.kt & VersionUtil.kt
 */

import versioning.BuildConfig
import versioning.VersionUtil

BuildConfig.init(project)

val baseVersion = "2.3.73"
group = "ac.grim.grimac"
version = VersionUtil.computeVersion(baseVersion)
description = "Libre simulation anticheat designed for 1.21 with 1.8–1.21 support, powered by PacketEvents 2.0."

ext["timestamp"] = System.currentTimeMillis().toString()
ext["git_branch"] = VersionUtil.getGitBranch(true)
ext["git_commit"] = VersionUtil.getGitCommitHash(true)
ext["git_org"] = System.getenv("GRIM_GIT_ORG") ?: VersionUtil.getGitUser()
ext["git_repo"] = System.getenv("GRIM_GIT_REPO") ?: "Grim"

println("Build configuration:")
println("    shadePE            = ${BuildConfig.shadePE}")
println("    relocate           = ${BuildConfig.relocate}")
println("    mavenLocalOverride = ${BuildConfig.mavenLocalOverride}")
println("    release            = ${BuildConfig.release}")
println("    version            = $version")

tasks.register("printVersion") {
    group = "versioning"
    description = "Prints the computed project version"
    doLast {
        println("VERSION=$version")
    }
}

// ---------- Java Compile Optimization ----------
subprojects {
    tasks.withType<JavaCompile>().configureEach {
        options.isFork = true
        options.isIncremental = true
    }
}
