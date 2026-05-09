rootProject.name = "SamplePlugin"

val configLibRoot = file("../..")
if (configLibRoot.resolve("settings.gradle.kts").isFile) {
    includeBuild(configLibRoot) {
        dependencySubstitution {
            substitute(module("com.github.Maru32768.ConfigLib:bukkit"))
                .using(project(":bukkit"))
        }
    }
}

val commandLibRoot = file("../../../CommandLib")
if (commandLibRoot.resolve("settings.gradle.kts").isFile) {
    includeBuild(commandLibRoot) {
        dependencySubstitution {
            substitute(module("com.github.Maru32768.CommandLib:spigot"))
                .using(project(":spigot"))
        }
    }
}
