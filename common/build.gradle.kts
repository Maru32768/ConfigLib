plugins {
    java
}

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
    api("com.google.code.gson:gson:2.10")
    implementation("org.snakeyaml:snakeyaml-engine:2.9")
    compileOnly("org.jetbrains:annotations:20.1.0")
    // commonだとコマンド周りを上手く実装出来ないためspigotを使う
    compileOnly("com.github.Maru32768.CommandLib:spigot:$commandLibVersion")
    // CommandContext#sendMessageを呼ぶ時にnet.md_5.bungee.api.chat.BaseComponentを見つけられずコンパイルが通らないため追加
    compileOnly("org.spigotmc:spigot-api:1.16.5-R0.1-SNAPSHOT")
    testImplementation("com.github.Maru32768.CommandLib:spigot-testing:${commandLibVersion}")
    testImplementation("org.junit.jupiter:junit-jupiter:5.8.2")
    testImplementation("org.assertj:assertj-core:3.25.1")
    testImplementation("org.spigotmc:spigot-api:1.16.5-R0.1-SNAPSHOT")
    testImplementation("com.mojang:brigadier:1.0.18")
}

tasks.test {
    useJUnitPlatform()
}
