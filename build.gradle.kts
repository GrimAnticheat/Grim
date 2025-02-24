//plugins {
//    java
//    `maven-publish`
//    id("com.gradleup.shadow") version "9.0.0-beta6" apply false
//    alias(libs.plugins.fabric.loom) apply false
//    id("com.diffplug.spotless") version "6.25.0" apply false
//}

group = "ac.grim.grimac"
version = "2.3.72"
description = "Libre simulation anticheat designed for 1.21 with 1.8-1.21 support, powered by PacketEvents 2.0."

// Set to false for debug builds
// You cannot live reload classes if the jar relocates dependencies
// Checks Project properties -> environment variable -> defaults true
extra["relocate"] = project.findProperty("relocate")?.toString()?.toBoolean()
    ?: System.getenv("RELOCATE_JAR")?.toBoolean()
            ?: true

// Build Optimization:
// - Add parallel build support
// - Add build caching
//tasks.withType<JavaCompile>().configureEach {
//    options.isFork = true
//    options.isIncremental = true
//}
