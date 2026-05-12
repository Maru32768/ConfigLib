rootProject.name = "SamplePlugin"

val configLibRoot = file("../..")
if (configLibRoot.resolve("settings.gradle.kts").isFile) {
    includeBuild(configLibRoot) {
        dependencySubstitution {
            substitute(module("com.github.Maru32768.ConfigLib:paper"))
                .using(project(":paper"))
        }
    }
}

val commandLibRoot = file("../../../CommandLib")
if (commandLibRoot.resolve("settings.gradle.kts").isFile) {
    includeBuild(commandLibRoot) {
        dependencySubstitution {
            substitute(module("com.github.Maru32768.CommandLib:paper"))
                .using(project(":paper"))
        }
    }
}
