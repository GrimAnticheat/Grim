// Cross-variant Grim Fabric library. Currently sparse — most Fabric code is
// MC-typed and lives in fabric-intermediary/-official.

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
