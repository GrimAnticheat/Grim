evaluationDependsOn(":")

plugins {
    id("java")
}

group = "dev.tieseler.teleportation"
version = "2.3.65"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    compileOnly("dev.folia:folia-api:1.20.4-R0.1-SNAPSHOT")
    implementation(project(":"))
}