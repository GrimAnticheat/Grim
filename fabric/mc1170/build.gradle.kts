dependencies {
    minecraft("com.mojang:minecraft:1.17")
    mappings("net.fabricmc:yarn:1.17+build.1:v2")
    compileOnly(project(":fabric:mc1161"))

    modImplementation(fabricApi.module("fabric-lifecycle-events-v1", "0.46.1+1.17"))
    modImplementation("me.lucko:fabric-permissions-api:0.3.1")
}
