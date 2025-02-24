plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    implementation(libs.spotless)
    implementation(libs.lombok)
    implementation(libs.shadow)
}
