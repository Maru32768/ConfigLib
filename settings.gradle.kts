pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://maven.minecraftforge.net/")
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.5.0"
}

rootProject.name = "ConfigLib"
include("spigot", "paper", "common", "common-java17-tests", "forge", "processor", "processor-smoke-test")

// TODO: Replace this local CommandLib composite build with released artifacts before publishing.
includeBuild("../CommandLib") {
    dependencySubstitution {
        substitute(module("com.github.Maru32768.CommandLib:common")).using(project(":common"))
        substitute(module("com.github.Maru32768.CommandLib:spigot")).using(project(":spigot"))
        substitute(module("com.github.Maru32768.CommandLib:paper")).using(project(":paper"))
        substitute(module("com.github.Maru32768.CommandLib:forge")).using(project(":forge"))
        substitute(module("com.github.Maru32768.CommandLib:spigot-testing")).using(project(":spigot-testing"))
        substitute(module("com.github.Maru32768.CommandLib:paper-testing")).using(project(":paper-testing"))
        substitute(module("com.github.Maru32768.CommandLib:common-testing")).using(project(":common-testing"))
    }
}
