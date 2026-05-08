plugins {
    java
    `java-library`
    id("net.minecraftforge.gradle") version "6.0.36"
}

val commandLibVersion: String by project

minecraft {
    mappings("snapshot", "20210309-1.16.5")
}

repositories {
    maven {
        url = uri("https://libraries.minecraft.net")
    }
    maven { url = uri("https://maven.minecraftforge.net/") }
    maven { url = uri("https://jitpack.io") }
}

dependencies {
    add("minecraft", "net.minecraftforge:forge:1.16.5-36.2.31")
    compileOnlyApi("org.jetbrains:annotations:20.1.0")
    api(project(":common"))
    api("com.github.Maru32768.CommandLib:forge:$commandLibVersion")
}
