import org.jetbrains.kotlin.gradle.tasks.KotlinCompile;

plugins {
    id("java-library")
    id("maven-publish")
    id("io.freefair.lombok") version "6.6"
    id("com.github.johnrengelman.shadow") version "7.1.2"
    kotlin("jvm") version "1.8.10"
}

allprojects {
    apply(plugin = "kotlin")
    apply(plugin = "java-library")
    apply(plugin = "maven-publish")
    apply(plugin = "io.freefair.lombok")
    apply(plugin = "com.github.johnrengelman.shadow")

    group = "ac.grim.grimac"
    version = "2.3.40"
    description = "Libre simulation anticheat designed for 1.19 with 1.8-1.19 support, powered by PacketEvents 2.0."

    java.sourceCompatibility = JavaVersion.VERSION_17
    java.targetCompatibility = JavaVersion.VERSION_17

    repositories {
        mavenLocal()
        mavenCentral()

        maven("https://jitpack.io/") // Grim API
    }

    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(17))
        }

        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        withSourcesJar()
        withJavadocJar()
    }

    tasks.build {
        dependsOn(tasks.shadowJar)
    }

    tasks.withType<JavaCompile> {
        options.encoding = "UTF-8"
        options.release.set(17)
        sourceCompatibility = JavaVersion.VERSION_17.name
        targetCompatibility = JavaVersion.VERSION_17.name
    }

    tasks.withType<KotlinCompile> {
        kotlinOptions.jvmTarget = "17"
    }

    publishing.publications.create<MavenPublication>("maven") {
        artifact(tasks["shadowJar"])
    }
}