dependencies {
    minecraft("com.mojang:minecraft:1.21.4")
    mappings("net.fabricmc:yarn:1.21.4+build.8:v2")

    modImplementation("net.fabricmc.fabric-api:fabric-api:0.118.0+1.21.4")
    modImplementation("me.lucko:fabric-permissions-api:0.3.1")

    modImplementation(libs.cloud.fabric)
    modImplementation(libs.fabric.loader)
}


tasks.compileJava {
    options.release.set(21)
}
