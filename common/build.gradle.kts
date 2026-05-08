plugins {
    java
}

val commandLibVersion: String by project

repositories {
    mavenCentral()
    maven {
        url = uri("https://libraries.minecraft.net")
    }
    maven { url = uri("https://jitpack.io") }
}

dependencies {
    api("com.google.code.gson:gson:2.10")
    implementation("org.snakeyaml:snakeyaml-engine:2.9")
    compileOnlyApi("org.jetbrains:annotations:20.1.0")
    api("com.github.Maru32768.CommandLib:common:$commandLibVersion")
    testImplementation("com.github.Maru32768.CommandLib:common-testing:${commandLibVersion}")
    testImplementation("org.junit.jupiter:junit-jupiter:5.8.2")
    testImplementation("org.assertj:assertj-core:3.25.1")
    testImplementation("com.mojang:brigadier:1.0.18")
}

tasks.test {
    useJUnitPlatform()
}
