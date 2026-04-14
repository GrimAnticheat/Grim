import org.gradle.jvm.toolchain.JavaLanguageVersion

plugins {
    id("net.fabricmc.fabric-loom-remap")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(25))
}

dependencies {
    implementation(project(":common"))
    compileOnly(project(":fabric-official:mc261"))
    compileOnly(libs.cloud.fabric.intermediary.compile) {
        exclude(group = "net.fabricmc.fabric-api")
    }
    minecraft("com.mojang:minecraft:1.16.1")
    mappings(loom.officialMojangMappings())
    modImplementation(fabricApi.module("fabric-lifecycle-events-v1", "0.42.0+1.16"))
    modCompileOnly("me.lucko:fabric-permissions-api:0.1-SNAPSHOT")
}

loom {
    accessWidenerPath = file("src/main/resources/grimac.accesswidener")
}

tasks.compileJava {
    options.release.set(25)
}
