plugins {
    `java-library`
}

repositories {
    mavenCentral()
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
    maven("https://repo.dmulloy2.net/repository/public/")
}

dependencies {
    // 引用 libs 目录下的所有 jar 文件
    compileOnly(files("libs/spigot-server-1.7.10-R0.1-SNAPSHOT.jar"))
    compileOnly(files("libs/ProtocolLib1.7.jar"))
    compileOnly("io.netty:netty-all:4.0.23.Final")
}
java {
    withSourcesJar()
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    sourceCompatibility = "1.8"
    targetCompatibility = "1.8"
}
