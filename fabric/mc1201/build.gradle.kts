dependencies {
    minecraft("com.mojang:minecraft:1.20.1")
    mappings("net.fabricmc:yarn:1.20.1+build.10:v2")

    modImplementation(fabricApi.module("fabric-lifecycle-events-v1", "0.92.3+1.20.1"))
    modImplementation("me.lucko:fabric-permissions-api:0.3.1")
}

tasks.compileJava {
    options.release.set(17)
}
