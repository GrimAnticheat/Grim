plugins {
    `java-library`
}

repositories {
    mavenCentral()
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
    maven("https://repo.dmulloy2.net/repository/public/")
}

dependencies {
    compileOnly("org.spigotmc:spigot-api:1.7.10-R0.1-SNAPSHOT")
    compileOnly("io.netty:netty-all:4.0.23.Final")
    compileOnly("com.comphenix.protocol:ProtocolLib:3.7.0")
}

java {
    withSourcesJar()
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    sourceCompatibility = "1.8"
    targetCompatibility = "1.8"
}
