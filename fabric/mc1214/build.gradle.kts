dependencies {
    minecraft("com.mojang:minecraft:1.21.4")
    mappings("net.fabricmc:yarn:1.21.4+build.8:v2")

    modImplementation(fabricApi.module("fabric-lifecycle-events-v1", "0.118.0+1.21.4"))
    modImplementation("me.lucko:fabric-permissions-api:0.3.1")
}


tasks.compileJava {
    options.release.set(21)
}
