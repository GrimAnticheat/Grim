dependencies {
    minecraft("com.mojang:minecraft:1.21.5")
    mappings("net.fabricmc:yarn:1.21.5+build.1:v2")
    compileOnly(project(":fabric:mc1161", configuration = "namedElements"))
    compileOnly(project(":fabric:mc1171", configuration = "namedElements"))
    compileOnly(project(":fabric:mc1194", configuration = "namedElements"))
    compileOnly(project(":fabric:mc1205", configuration = "namedElements"))

    modImplementation(fabricApi.module("fabric-lifecycle-events-v1", "0.121.0+1.21.5"))
    modImplementation("me.lucko:fabric-permissions-api:0.3.1")
}


tasks.compileJava {
    options.release.set(21)
}
