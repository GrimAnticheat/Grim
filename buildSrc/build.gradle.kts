plugins {
    `kotlin-dsl`
    kotlin("plugin.serialization") version "1.9.0" // Use the same Kotlin version as your main project
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.spotless)
    implementation(libs.lombok)
    implementation(libs.shadow)
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
}
