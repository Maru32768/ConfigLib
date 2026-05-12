pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://maven.minecraftforge.net/")
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.5.0"
}

rootProject.name = "ConfigLibTestPlugin"

// TODO: Replace this local CommandLib composite build with released artifacts before publishing.
includeBuild("../../../CommandLib") {
    dependencySubstitution {
        substitute(module("com.github.Maru32768.CommandLib:spigot")).using(project(":spigot"))
    }
}
