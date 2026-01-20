dependencies {
    minecraft("com.mojang:minecraft:1.17.1")
    mappings(loom.officialMojangMappings())
    compileOnly(project(":fabric:mc1161", configuration = "namedElements"))

    modImplementation(fabricApi.module("fabric-lifecycle-events-v1", "0.46.1+1.17"))
    modCompileOnly("me.lucko:fabric-permissions-api:0.3.1")
}
