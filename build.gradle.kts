import org.spongepowered.gradle.vanilla.repository.MinecraftPlatform

plugins {
    id("java-library")
    id("org.spongepowered.gradle.vanilla") version "0.2.1-SNAPSHOT"
}

group = "dev.booky"
version = "1.0.0"

repositories {
    maven("https://repo.papermc.io/repository/maven-public/")
}

minecraft {
    version(project.ext["mcVersion"] as String)
    platform(MinecraftPlatform.SERVER)

    runs {
        server("generate") {
            mainClass("dev.booky.generation.GenerationMain")
            args(project.layout.projectDirectory.dir("generated").toString())
        }
    }
}
