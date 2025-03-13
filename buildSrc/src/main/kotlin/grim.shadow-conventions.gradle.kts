import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("com.gradleup.shadow")
}

val relocate: Boolean by rootProject.extra

// Create a configuration for shading dependencies from common
val shadowCommon: Configuration by project.configurations.creating {
    isCanBeResolved = true
    isCanBeConsumed = false
    extendsFrom(project.configurations.getByName("implementation"))
}

tasks.named<ShadowJar>("shadowJar") {
    minimize()
    archiveFileName.set("${rootProject.name}-${project.name}-${rootProject.version}.jar")
    from(shadowCommon) // Use from() instead of direct assignment

    if (relocate) {
        relocate(
            "io.github.retrooper.packetevents",
            "ac.grim.grimac.shaded.io.github.retrooper.packetevents"
        )
        relocate(
            "com.github.retrooper.packetevents",
            "ac.grim.grimac.shaded.com.github.retrooper.packetevents"
        )
        relocate("club.minnced", "ac.grim.grimac.shaded.discord-webhooks")
        relocate("org.sl4j", "ac.grim.grimac.shaded.sl4j") // Required by discord-webhooks
        relocate("github.scarsz.configuralize", "ac.grim.grimac.shaded.configuralize")
        relocate("com.github.puregero", "ac.grim.grimac.shaded.com.github.puregero")
        relocate("com.google.code.gson", "ac.grim.grimac.shaded.gson")
        relocate("alexh", "ac.grim.grimac.shaded.maps")
        relocate("it.unimi.dsi.fastutil", "ac.grim.grimac.shaded.fastutil")
        relocate("net.kyori", "ac.grim.grimac.shaded.kyori")
        relocate("okhttp3", "ac.grim.grimac.shaded.okhttp3")
        relocate("okio", "ac.grim.grimac.shaded.okio")
        relocate("org.yaml.snakeyaml", "ac.grim.grimac.shaded.snakeyaml")
        relocate("org.json", "ac.grim.grimac.shaded.json")
        relocate("org.intellij", "ac.grim.grimac.shaded.intellij")
        relocate("org.jetbrains", "ac.grim.grimac.shaded.jetbrains")
        relocate("org.incendo", "ac.grim.grimac.shaded.incendo")
        relocate("io.leangen.geantyref", "ac.grim.grimac.shaded.geantyref") // Required by cloud
    }
    mergeServiceFiles()
}

tasks.named("assemble") {
    dependsOn(tasks.named("shadowJar"))
}
