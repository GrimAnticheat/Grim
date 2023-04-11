repositories {
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/") // Spigot
    maven("https://repo.viaversion.com") // ViaVersion
    maven("https://repo.aikar.co/content/groups/aikar/") // ACF
    maven("https://nexus.scarsz.me/content/repositories/releases") // Configuralize
    maven("https://clojars.org/repo") // MultiPaper MultiLib
    maven("https://repo.opencollab.dev/maven-snapshots/") // Floodgate
    maven("https://repo.codemc.io/repository/maven-snapshots/") // PacketEvents
    maven("https://repo.clojars.org") // FastUtil, Discord-Webhooks
}

dependencies {
    implementation("com.github.retrooper.packetevents:spigot:2.0-SNAPSHOT")
    implementation("co.aikar:acf-paper:0.5.1-20230402.114301-23")
    implementation("club.minnced:discord-webhooks:0.8.0")
    implementation("it.unimi.dsi:fastutil:8.5.9")
    implementation("org.jetbrains:annotations:23.1.0") // Why is this needed to compile?
    implementation("github.scarsz:configuralize:1.4.0")
    implementation("com.github.puregero:multilib:1.1.8")

    implementation("com.github.grimanticheat:grimapi:add576ba8b")
    // Used for local testing: implementation("ac.grim.grimac:grimapi:1.0")

    compileOnly("org.geysermc.floodgate:api:2.0-SNAPSHOT")
    compileOnly("org.spigotmc:spigot-api:1.19.3-R0.1-SNAPSHOT")
    compileOnly("com.viaversion:viaversion-api:4.1.1")
    compileOnly("io.netty:netty-all:4.1.85.Final")
}

tasks.shadowJar {
    minimize()
    archiveFileName.set("${project.name}-${project.version}.jar")
    relocate("io.github.retrooper.packetevents", "ac.grim.grimac.shaded.io.github.retrooper.packetevents")
    relocate("com.github.retrooper.packetevents", "ac.grim.grimac.shaded.com.github.retrooper.packetevents")
    relocate("co.aikar.acf", "ac.grim.grimac.shaded.acf")
    relocate("club.minnced", "ac.grim.grimac.shaded.discord-webhooks")
    relocate("github.scarsz.configuralize", "ac.grim.grimac.shaded.configuralize")
    relocate("com.github.puregero", "ac.grim.grimac.shaded.com.github.puregero")
    relocate("com.google.gson", "ac.grim.grimac.shaded.gson")
    relocate("alexh", "ac.grim.grimac.shaded.maps")
    relocate("it.unimi.dsi.fastutil", "ac.grim.grimac.shaded.fastutil")
    relocate("net.kyori", "ac.grim.grimac.shaded.kyori")
    relocate("okhttp3", "ac.grim.grimac.shaded.okhttp3")
    relocate("okio", "ac.grim.grimac.shaded.okio")
    relocate("org.yaml.snakeyaml", "ac.grim.grimac.shaded.snakeyaml")
    relocate("org.json", "ac.grim.grimac.shaded.json")
    relocate("org.intellij", "ac.grim.grimac.shaded.intellij")
    relocate("org.jetbrains", "ac.grim.grimac.shaded.jetbrains")
}