// Pure-Java shared library for Grim's Fabric variants. Currently sparse —
// most of Grim's Fabric code is MC-typed (mixins, scheduler, platform) and
// lives in fabric-intermediary or (eventually) fabric-official. This module
// exists to host any cross-variant API that emerges (chain-loader-style
// interfaces, version-agnostic types) and to mirror PE's structure.

plugins {
    `java-library`
    grim.`base-conventions`
}

repositories {
    exclusive("https://repo.grim.ac/snapshots") {
        includeGroup("ac.grim.grimac")
        includeGroup("com.github.retrooper")
    }
    exclusive("https://jitpack.io", { mavenContent { releasesOnly() } }) {
        includeGroup("com.github.Fallen-Breath.conditional-mixin")
    }
    exclusive("https://nexus.scarsz.me/content/repositories/releases", { mavenContent { releasesOnly() } }) {
        includeGroup("github.scarsz")
    }
    mavenCentral()
}

dependencies {
    compileOnly(project(":common"))
}
