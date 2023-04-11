import org.jetbrains.kotlin.gradle.tasks.KotlinCompile;

plugins {
    id("java-library")
    id("maven-publish")
    id("io.freefair.lombok") version "6.6"
    id("com.github.johnrengelman.shadow") version "7.1.2"
    kotlin("jvm") version System.getProperty("kotlin_version")
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

    java.sourceCompatibility = JavaVersion.VERSION_19
    java.targetCompatibility = JavaVersion.VERSION_19

    repositories {
        mavenLocal()
        mavenCentral()

        maven("https://jitpack.io/") // Grim API
    }

    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(19))
        }

        sourceCompatibility = JavaVersion.VERSION_19
        targetCompatibility = JavaVersion.VERSION_19
        withSourcesJar()
        withJavadocJar()
    }

    tasks.build {
        dependsOn(tasks.shadowJar)
    }

    tasks.withType<JavaCompile> {
        options.encoding = "UTF-8"
        options.release.set(19)
        sourceCompatibility = JavaVersion.VERSION_19.name
        targetCompatibility = JavaVersion.VERSION_19.name
    }

    tasks.withType<KotlinCompile> {
        kotlinOptions.jvmTarget = "19"
    }

    publishing.publications.create<MavenPublication>("maven") {
        artifact(tasks["shadowJar"])
    }
}