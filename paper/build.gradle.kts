val commandLibVersion: String by project

repositories {
    mavenCentral()
    maven {
        name = "papermc-repo"
        url = uri("https://repo.papermc.io/repository/maven-public/")
    }
    maven {
        url = uri("https://libraries.minecraft.net")
    }
    maven { url = uri("https://jitpack.io") }
}

configure<JavaPluginExtension> {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21-R0.1-SNAPSHOT")
    compileOnlyApi("org.jetbrains:annotations:20.1.0")
    api(project(":common"))
    api("com.github.Maru32768.CommandLib:paper:$commandLibVersion")
    compileOnly("com.mojang:brigadier:1.0.18")
}
