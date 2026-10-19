@Suppress("PropertyName")
val minecraft_version: String by project

dependencies {
    minecraft("com.mojang:minecraft:$minecraft_version")
}

// Optional production-loader integration mod. It is not included in the release jar.
val messageTest by sourceSets.creating {
    compileClasspath += sourceSets.main.get().output + sourceSets.main.get().compileClasspath
    runtimeClasspath += sourceSets.main.get().output + sourceSets.main.get().runtimeClasspath
}
configurations[messageTest.implementationConfigurationName].extendsFrom(
    configurations.implementation.get(), configurations.compileOnly.get()
)
dependencies {
    add(messageTest.implementationConfigurationName, "net.kyori:adventure-api:5.2.0")
    add(messageTest.implementationConfigurationName, "net.kyori:adventure-text-serializer-gson:5.2.0")
}
tasks.register<Jar>("messageTestJar") {
    from(messageTest.output)
    archiveClassifier.set("message-test")
}
