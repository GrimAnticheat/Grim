// grim-minestom — additives Minestom-Platform-Modul (Geschwister zu :bukkit/:fabric).
// Rein additiv: implementiert Grims platform.api gegen Minestom + das packetevents-minestom-
// Binding, ohne :common / :api anzufassen → Upstream-Update bleibt ein konfliktfreies Rebase.
plugins {
    java
}

repositories {
    // Grim-API/-internal + PacketEvents liegen auf maven.grim.ac / repo.grim.ac (nicht Central).
    // Repos werden NICHT von :common geerbt → hier vollständig spiegeln, ohne Content-Filter,
    // sonst scheitert die transitive Auflösung (z.B. grim-internal:1.6.0.9).
    mavenLocal()
    maven("https://maven.grim.ac/public/releases")
    maven("https://maven.grim.ac/public/snapshots")
    maven("https://repo.grim.ac/snapshots")
    maven("https://repo.viaversion.com")
    maven("https://nexus.scarsz.me/content/repositories/releases")
    maven("https://repo.opencollab.dev/maven-releases/")
    maven("https://repo.opencollab.dev/maven-snapshots/")
    mavenCentral()
}

java {
    toolchain {
        // Wie das grim-official/mc261-Leaf und unser :packetevents-minestom: JDK 25.
        // :common ist mit Toolchain 21 gebaut; dessen Bytecode läuft auf 25 problemlos.
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

dependencies {
    // Grims plattform-agnostischer Kern. implementation (nicht compileOnly), damit dessen
    // api-Abhängigkeiten (cloud, configuralize, …) auf dem Compile-Classpath liegen.
    implementation(project(":common"))

    // Minestom liefern die konsumierenden Module zur Laufzeit; hier nur zum Kompilieren.
    compileOnly("net.minestom:minestom:2026.07.22-26.2")
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}
