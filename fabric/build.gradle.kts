import net.fabricmc.loom.task.RemapJarTask

val minecraft_version: String by project
val yarn_mappings: String by project
val fabric_version: String by project

plugins {
    `maven-publish`
    alias(libs.plugins.fabric.loom)
    grim.`base-conventions`
    grim.`jij-conventions`
}

dependencies {
    minecraft("com.mojang:minecraft:$minecraft_version")
    mappings("net.fabricmc:yarn:$yarn_mappings")
    modImplementation(fabricApi.module("fabric-lifecycle-events-v1", fabric_version))

    modImplementation("me.lucko:fabric-permissions-api:0.3.1")

    modImplementation(libs.cloud.fabric)
    modImplementation(libs.fabric.loader)
    modImplementation(libs.packetevents.fabric)

    modApi(libs.packetevents.fabric)
}

// The configurations below will only apply to :fabric and its submodules, not its siblings or the root project
allprojects {
    apply(plugin = "fabric-loom")
    apply(plugin = "grim.base-conventions")

    repositories {
        mavenLocal()
        maven {
            name = "FabricMC"
            url = uri("https://maven.fabricmc.net/")
        }
        maven("https://jitpack.io/") // Grim API & Conditional Mixin
        maven("https://repo.viaversion.com") // ViaVersion
        maven("https://nexus.scarsz.me/content/repositories/releases") // Configuralize
        maven("https://repo.opencollab.dev/maven-snapshots/") // Floodgate
        maven("https://repo.opencollab.dev/maven-releases/") // Cumulus (for Floodgate)
        maven("https://repo.codemc.io/repository/maven-releases/") // PacketEvents
        maven("https://repo.codemc.io/repository/maven-snapshots/")
        maven("https://s01.oss.sonatype.org/content/repositories/snapshots/")
        mavenCentral()
        // FastUtil, Discord-Webhooks
    }

    loom {
        accessWidenerPath = file("src/main/resources/grimac.accesswidener")
    }

    dependencies {
        // I hate this syntax, is there an alternative to make modImplementation(libs.package.name) work?
        val libsx = rootProject.extensions.getByType<VersionCatalogsExtension>().named("libs")
        // Use the libs extension from the root project
        modImplementation(libsx.findLibrary("cloud-fabric").get())
        modImplementation(libsx.findLibrary("fabric-loader").get())

        implementation(project(":common"))
    }
}

subprojects {
    dependencies {
        // configuration = "namedElements" required when depending on another loom project
        implementation(project(":fabric", configuration = "namedElements"))
    }
}

tasks.withType<RemapJarTask>().configureEach {
    archiveFileName.set("${rootProject.name}-${project.name}-${rootProject.version}.jar")

    // Include classes from :common project directly
    from(project(":common").sourceSets.main.get().output)
}

subprojects.forEach {
    tasks.named("remapJar").configure {
        dependsOn("${it.path}:remapJar")
    }
}

tasks.remapJar.configure {
    subprojects.forEach { subproject ->
        subproject.tasks.matching { it.name == "remapJar" }.configureEach {
            nestedJars.from(this)
        }
    }
}

publishing.publications.create<MavenPublication>("maven") {
    artifact(tasks["remapJar"])
}
