dependencies {
    minecraft("com.mojang:minecraft:1.21.1")
    mappings("net.fabricmc:yarn:1.21.1+build.3:v2")

    modImplementation(fabricApi.module("fabric-lifecycle-events-v1", "0.115.1+1.21.1"))
    modImplementation("me.lucko:fabric-permissions-api:0.3.1")
}

tasks.compileJava {
    options.release.set(21)
}
