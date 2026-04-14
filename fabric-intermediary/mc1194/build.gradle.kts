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
    minecraft("com.mojang:minecraft:1.19.4")
    mappings(loom.officialMojangMappings())
    compileOnly(project(":fabric-intermediary:mc1161", configuration = "namedElements"))
    compileOnly(project(":fabric-intermediary:mc1171", configuration = "namedElements"))
    modImplementation(fabricApi.module("fabric-lifecycle-events-v1", "0.76.0+1.19.4"))
    modCompileOnly("me.lucko:fabric-permissions-api:0.3.1")
}

loom {
    accessWidenerPath = file("src/main/resources/grimac.accesswidener")
}

tasks.compileJava {
    options.release.set(25)
}
