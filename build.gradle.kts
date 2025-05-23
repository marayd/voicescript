import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
//
plugins {
    id("java")
    id("com.github.johnrengelman.shadow") version "8.1.1"
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.14"
}

group = "org.mryd"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.plasmoverse.com/releases")
    maven("https://repo.plasmoverse.com/snapshots")
    maven("https://maven.maxhenkel.de/repository/public")
}

dependencies {
    paperweight.paperDevBundle("1.21.4-R0.1-SNAPSHOT")

    compileOnly("de.maxhenkel.voicechat:voicechat-api:2.1.12")
    compileOnly("su.plo.voice.server:paper:2.1.4")

    compileOnly("org.projectlombok:lombok:1.18.36")
    annotationProcessor("org.projectlombok:lombok:1.18.36")

    compileOnly("com.openai:openai-java:2.2.0")
    compileOnly("org.luaj:luaj-jse:3.0.1")
    compileOnly("com.alphacephei:vosk:0.3.45")
}


tasks.jar {
    enabled = false
}

tasks {
    build {
        dependsOn(named<ShadowJar>("shadowJar"))
    }
}