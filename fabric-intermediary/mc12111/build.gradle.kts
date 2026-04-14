import net.fabricmc.loom.task.RemapJarTask
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.tasks.bundling.Jar
import org.gradle.jvm.toolchain.JavaLanguageVersion

plugins {
    id("net.fabricmc.fabric-loom-remap")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(25))
}

dependencies {
    minecraft("com.mojang:minecraft:1.21.11")
    mappings(loom.officialMojangMappings())
    implementation(project(":common"))
    compileOnly(project(":fabric-official:mc261"))
    compileOnly(libs.cloud.fabric.intermediary.compile) {
        exclude(group = "net.fabricmc.fabric-api")
    }
    compileOnly(project(":fabric-intermediary:mc1161", configuration = "namedElements"))
    compileOnly(project(":fabric-intermediary:mc1171", configuration = "namedElements"))
    compileOnly(project(":fabric-intermediary:mc1194", configuration = "namedElements"))
    compileOnly(project(":fabric-intermediary:mc1205", configuration = "namedElements"))
    modImplementation(fabricApi.module("fabric-lifecycle-events-v1", "0.141.1+1.21.11"))
    modCompileOnly(libs.fabric.loader)
    modCompileOnly("me.lucko:fabric-permissions-api:0.6.1")
}

loom {
    accessWidenerPath = file("src/main/resources/grimac.accesswidener")
}

tasks.compileJava {
    options.release.set(25)
}

tasks.named<RemapJarTask>("remapJar").configure {
    val mc261Jar = rootProject.project(":fabric-official:mc261").tasks.named<Jar>("jar")
    dependsOn(mc261Jar)
    from(zipTree(mc261Jar.get().archiveFile.get().asFile)) {
        include("ac/grim/grimac/platform/fabric/**")
        exclude("ac/grim/grimac/platform/fabric/mc1161/**")
        exclude("ac/grim/grimac/platform/fabric/mc1171/**")
        exclude("ac/grim/grimac/platform/fabric/mc1194/**")
        exclude("ac/grim/grimac/platform/fabric/mc1205/**")
        exclude("ac/grim/grimac/platform/fabric/mc1216/**")
        exclude("ac/grim/grimac/platform/fabric/mixins/**")
        exclude("ac/grim/grimac/platform/fabric/scheduler/**")
    }
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}
