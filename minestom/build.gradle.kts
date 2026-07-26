// grim-minestom — additives Minestom-Platform-Modul (Geschwister zu :bukkit/:fabric).
// Rein additiv: implementiert Grims platform.api gegen Minestom + das packetevents-minestom-
// Binding, ohne :common / :api anzufassen → Upstream-Update bleibt ein konfliktfreies Rebase.
plugins {
    // java-library (nicht nur java): MinestomPlatformLoader implements PlatformLoader (aus :common),
    // d.h. :common-Typen sind Teil der ÖFFENTLICHEN API dieses Moduls. Nur mit java-library gibt es
    // die `api`-Konfiguration, die diese Typen an Konsumenten (:anticheat, :game-*) weiterreicht.
    `java-library`
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
    // Grims plattform-agnostischer Kern. api (nicht implementation), weil PlatformLoader &
    // die übrigen platform.api-Typen in den Signaturen dieses Moduls auftauchen und daher auf
    // dem Compile-Classpath der Konsumenten sichtbar sein müssen (sonst: "Kein Zugriff auf
    // PlatformLoader" beim Kompilieren von AnticheatBootstrap).
    api(project(":common"))

    // PacketEvents-API ist in :common nur compileOnly (jede Plattform bündelt PE selbst) → hier
    // explizit, weil die Adapter PE-Typen (ItemStack/GameMode/WrappedBlockState/Vector3d) nutzen.
    // Zur Laufzeit liefert das konsumierende Modul das packetevents-minestom-Binding.
    compileOnly(libs.packetevents.api)

    // Minestom liefern die konsumierenden Module zur Laufzeit; hier nur zum Kompilieren.
    compileOnly("net.minestom:minestom:2026.07.22-26.2")

    // Runtime-Libs, die Grims :common erwartet, dass die PLATTFORM sie liefert (auf Bukkit vom
    // Paper-Server, auf Fabric vom Spiel) — Minestom bringt sie NICHT mit. Als runtimeOnly hier,
    // damit sie an alle Konsumenten (:anticheat / :game-*) propagieren. Beginn des Phase-3-„long
    // tail": bei jedem weiteren NoClassDefFound beim Grim-Boot kommt die nächste Lib dazu.
    runtimeOnly("com.google.guava:guava:33.4.0-jre") // GrimAPI.<init> nutzt guava (ClassToInstanceMap u.a.)
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}
