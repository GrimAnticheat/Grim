// Shared Grim code for the Fabric platform.
//
// Grim ships two Fabric variants: fabric-intermediary (Minecraft 1.16.1 through
// 1.21.11) and fabric-official (Minecraft 26.X). A lot of the Fabric platform
// code is the same in both because it never touches Minecraft's own classes. That
// shared, Minecraft-free code lives here once so we don't have to keep two copies
// in sync.
//
// The rule that makes that possible: nothing in this module may use a Minecraft
// (net.minecraft.*) type, and the module deliberately does NOT apply the
// fabric-loom plugin. It is an ordinary java-library that the top-level fabric/
// build bundles into the final mod jar via `include(project(":fabric-common"))`.
// If a class needs to call into Minecraft at all (even an un-imported method on
// MinecraftServer or ServerPlayer), it cannot live here and instead belongs in
// one of the version-specific variant modules.

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

    // PacketEvents (ItemStack/Vector3d/GameMode/User in the shared player + inventory
    // wrappers and the conversion-util interface). compileOnly, never bundled here: PE is
    // JiJ'd once at the top-level fabric/ aggregator, matching :common's own compileOnly.
    // adventure (net.kyori.*) flows transitively from :common's api(adventure-text-minimessage).
    compileOnly(libs.packetevents.api)

    // fabric-loader API (net.fabricmc.loader.api.*) used by the metrics/resolver code.
    // Plain jar — not a Minecraft artifact — so it is fine on a java-library.
    compileOnly(libs.fabric.loader)

    // Vendored bStats + JUL logging bridges pull these.
    compileOnly("org.yaml:snakeyaml:2.2")
    compileOnly("org.slf4j:slf4j-api:2.0.17")
    compileOnly("org.apache.logging.log4j:log4j-api:2.24.3")
    compileOnly(libs.jetbrains.annotations)
}
