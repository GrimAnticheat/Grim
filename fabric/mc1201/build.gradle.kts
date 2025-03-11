dependencies {
    minecraft("com.mojang:minecraft:1.20.1")
    mappings("net.fabricmc:yarn:1.20.1+build.10:v2")

    modImplementation("net.fabricmc.fabric-api:fabric-api:0.92.3+1.20.1")
    modImplementation("me.lucko:fabric-permissions-api:0.3.1")

    modImplementation(libs.cloud.fabric)
    modImplementation(libs.fabric.loader)
}

tasks.compileJava {
    options.release.set(17)
}
