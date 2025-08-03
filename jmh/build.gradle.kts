/**
 * ┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┓
 * ┃          GrimAC ‑ JMH Module          ┃
 * ┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛
 *
 * Stand-alone build script (Option B)
 *  – keeps every version right here
 */

plugins {
    id("java")                                    // compile benchmarks
    id("me.champeau.jmh") version "0.7.2"         // JMH Gradle plugin
}

/* ───────────────────────────────
 * Version constants (⇩ tweak here)
 * ─────────────────────────────── */
val jmhLibVersion    = "1.37"                     // JMH core libs
val junitVersion     = "5.10.0"

/* ─────────────────────────────── */
repositories {
    maven("https://repo.grim.ac/snapshots") { // Grim API
        content {
            includeGroup("ac.grim.grimac")
            includeGroup("com.github.retrooper")
        }
    }
    mavenCentral()
    maven("https://nexus.scarsz.me/content/repositories/releases") // Configuralize
    maven("https://repo.papermc.io/repository/maven-public/") //MockBukkit
    maven("https://repo.viaversion.com") // ViaVersion
}

/* ─────────────────────────────── */
dependencies {
    /* code under test */
    implementation("org.openjdk.jmh:jmh-core:1.37")
    implementation("org.openjdk.jmh:jmh-generator-annprocess:1.37")
    implementation("com.github.seeseemelk:MockBukkit-v1.21:3.133.2")
    implementation(libs.packetevents.spigot)
    implementation("org.objenesis:objenesis:3.4")
    implementation("io.netty:netty-all:4.1.85.Final")
    implementation("io.netty:netty-transport:4.1.85.Final")
    implementation("org.slf4j:slf4j-simple:2.0.16")
    implementation(libs.via.version.api)
    implementation("org.openjdk.jol:jol-core:0.17")

    implementation(project(":common"))
    jmh(project(":common"))                       // also on the JMH class-paths
    jmh("org.slf4j:slf4j-simple:2.0.16")
    jmh("org.openjdk.jmh:jmh-core:1.37")
    jmh("org.openjdk.jmh:jmh-generator-annprocess:1.37")
    jmh("org.openjdk.jol:jol-core:0.17")

    jmhAnnotationProcessor("org.openjdk.jmh:jmh-generator-annprocess:1.37")

    /* extra benchmarking libs
       jmh("org.some:fast-util:1.0.0") */

    /* tests (if you keep unit tests in this module) */
    testImplementation(platform("org.junit:junit-bom:$junitVersion"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

/* ─────────────────────────────── */
jmh {
    jvmArgsAppend.set(
        listOf(
            "-Djdk.attach.allowAttachSelf=true",
            "-Djol.magicFieldOffset=true",
            "--add-opens", "java.base/java.nio=ALL-UNNAMED",
            "--add-opens", "java.base/java.lang=ALL-UNNAMED",
            "--add-opens", "me.grim.bench.blockchange.original_low_hanging_fruit=ALL-UNNAMED"
        )
    )
}

/* Make `gradle check` also run micro-benchmarks (optional) */
tasks.named("check") { dependsOn("jmh") }

/* Small compile-time tweaks (optional) */
tasks.withType<JavaCompile>().configureEach {
    options.isFork        = true
    options.isIncremental = true
}
