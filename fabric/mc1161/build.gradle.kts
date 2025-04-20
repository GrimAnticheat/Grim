repositories {
    maven("https://oss.sonatype.org/content/repositories/snapshots")
}

dependencies {
    minecraft("com.mojang:minecraft:1.16.1")
    mappings("net.fabricmc:yarn:1.16.1+build.21:v2")

    modImplementation(fabricApi.module("fabric-lifecycle-events-v1", "0.42.0+1.16"))
    modImplementation("me.lucko:fabric-permissions-api:0.1-SNAPSHOT")
}

loom {
    accessWidenerPath = file("src/main/resources/grimac.accesswidener")
}
