pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://maven.minecraftforge.net/")
    }
}

rootProject.name = "samplemod"

val configLibRoot = file("../..")
if (configLibRoot.resolve("settings.gradle.kts").isFile) {
    includeBuild(configLibRoot) {
        dependencySubstitution {
            substitute(module("com.github.Maru32768.ConfigLib:forge"))
                .using(project(":forge"))
        }
    }
}

// TODO: Replace this local CommandLib composite build with released artifacts before publishing.
val commandLibRoot = file("../../../CommandLib")
if (commandLibRoot.resolve("settings.gradle.kts").isFile) {
    includeBuild(commandLibRoot) {
        dependencySubstitution {
            substitute(module("com.github.Maru32768.CommandLib:forge"))
                .using(project(":forge"))
        }
    }
}
