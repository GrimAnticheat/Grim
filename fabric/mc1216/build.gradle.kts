dependencies {
    minecraft("com.mojang:minecraft:1.21.6")
    mappings(loom.officialMojangMappings())
    compileOnly(project(":fabric:mc1161", configuration = "namedElements"))
    compileOnly(project(":fabric:mc1171", configuration = "namedElements"))
    compileOnly(project(":fabric:mc1194", configuration = "namedElements"))
    compileOnly(project(":fabric:mc1205", configuration = "namedElements"))

    modImplementation(fabricApi.module("fabric-lifecycle-events-v1", "0.128.1+1.21.6"))
    modImplementation("me.lucko:fabric-permissions-api:0.6.1")
    include("me.lucko:fabric-permissions-api:0.6.1")
}


tasks.compileJava {
    options.release.set(21)
}
