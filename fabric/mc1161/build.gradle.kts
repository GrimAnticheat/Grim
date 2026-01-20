dependencies {
    minecraft("com.mojang:minecraft:1.16.1")
    mappings(loom.officialMojangMappings())

    modImplementation(fabricApi.module("fabric-lifecycle-events-v1", "0.42.0+1.16"))
    modCompileOnly("me.lucko:fabric-permissions-api:0.1-SNAPSHOT")
}

loom {
    accessWidenerPath = file("src/main/resources/grimac.accesswidener")
}
