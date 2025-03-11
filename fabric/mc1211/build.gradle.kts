dependencies {
    minecraft("com.mojang:minecraft:1.21.1")
    mappings("net.fabricmc:yarn:1.21.1+build.3:v2")

    modImplementation("net.fabricmc.fabric-api:fabric-api:0.115.1+1.21.1")
    modImplementation("me.lucko:fabric-permissions-api:0.3.1")

    modImplementation(libs.cloud.fabric)
    modImplementation(libs.fabric.loader)
}

tasks.compileJava {
    options.release.set(21)
}
