@Suppress("PropertyName")
val minecraft_version: String by project

dependencies {
    minecraft("com.mojang:minecraft:$minecraft_version")
    implementation("net.kyori:adventure-api:5.2.0")
    implementation("net.kyori:adventure-text-serializer-gson:5.2.0")
}
