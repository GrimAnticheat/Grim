// Cross-variant Grim Fabric library. Holds the Fabric platform code that is
// NMS-free (no net.minecraft.* on the compile classpath) and therefore identical
// across the intermediary (1.16.1-1.21.11) and official (26.X) variants, so it
// lives here once instead of being duplicated in both.
//
// HARD CONSTRAINT: this module must NOT depend on any version-specific Minecraft
// artifact and does not apply fabric-loom. It is a plain java-library that the
// fabric/ aggregator JiJ's via `include(project(":fabric-common"))`. Any Fabric
// class that references a net.minecraft type (even via an un-imported
// MinecraftServer/ServerPlayer method call) cannot live here; it stays in the
// MC-typed variant modules.

plugins {
    `java-library`
    grim.`base-conventions`
}

repositories {
    exclusive("https://maven.fabricmc.net/") {
        includeGroup("net.fabricmc")
        includeGroup("net.fabricmc.fabric-api")
    }
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
    // grim core + the version-neutral platform API (schedulers, Pair, GrimAPI, etc.).
    compileOnly(project(":common"))
    compileOnly(libs.grim.api)
    compileOnly(libs.grim.internal)
    compileOnly(libs.grim.internal.shims)

    // fabric-loader API (net.fabricmc.loader.api.*) used by the metrics/resolver code.
    // Plain jar — not a Minecraft artifact — so it is fine on a java-library.
    compileOnly(libs.fabric.loader)

    // PROTOTYPE (refactor/fabric-dedupe spike): packetevents is a plain (non-Minecraft)
    // library, so it is allowed on this NMS-free java-library (see header: grim-api /
    // packetevents / adventure / JDK). The injected ServerPlayer bridge returns PE
    // GameMode / Vector3d, which are the version-invariant types the duplicated player
    // wrapper already used. Needed for GrimInjectedServerPlayer + InjectedFabricPlatformPlayerBase.
    compileOnly(libs.packetevents.api)

    // Vendored bStats + JUL logging bridges pull these.
    compileOnly("org.yaml:snakeyaml:2.2")
    compileOnly("org.slf4j:slf4j-api:2.0.17")
    compileOnly("org.apache.logging.log4j:log4j-api:2.24.3")
    compileOnly(libs.jetbrains.annotations)
}
