val commandLibVersion: String by project

repositories {
    mavenCentral()
    maven {
        name = "spigotmc-repo"
        url = uri("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
    }
    maven {
        url = uri("https://libraries.minecraft.net")
    }
    maven { url = uri("https://jitpack.io") }
}

dependencies {
    compileOnly("org.spigotmc:spigot-api:1.16.5-R0.1-SNAPSHOT")
    compileOnlyApi("org.jetbrains:annotations:20.1.0")
    api(project(":common"))
    api("com.github.Maru32768.CommandLib:spigot:$commandLibVersion")
    compileOnly("com.mojang:brigadier:1.0.18")
}
