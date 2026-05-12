import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    java
    id("com.gradleup.shadow") version "9.4.1"
}

group = "net.kunmc.lab"
version = "1.0.0"

repositories {
    mavenCentral()
    maven {
        name = "papermc-repo"
        url = uri("https://repo.papermc.io/repository/maven-public/")
    }
    maven {
        name = "sonatype"
        url = uri("https://oss.sonatype.org/content/groups/public/")
    }
    maven { url = uri("https://jitpack.io") }
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21-R0.1-SNAPSHOT")

    implementation("com.github.Maru32768.CommandLib:paper:latest.release")
    implementation("com.github.Maru32768.ConfigLib:paper:latest.release")
}

tasks.named<ShadowJar>("shadowJar") {
    archiveFileName.set("${rootProject.name}-${project.version}.jar")
    dependencies {
        include(dependency("com.github.Maru32768.CommandLib:paper:.*"))
        include(dependency("com.github.Maru32768.ConfigLib:paper:.*"))
        include(dependency("com.google.code.gson:gson:.*"))
        include(dependency("org.snakeyaml:snakeyaml-engine:.*"))
    }
    relocate("net.kunmc.lab.commandlib", "${project.group}.${project.name.lowercase()}.commandlib")
    relocate("net.kunmc.lab.configlib", "${project.group}.${project.name.lowercase()}.configlib")
    relocate("com.google.gson", "${project.group}.${project.name.lowercase()}.gson")
    relocate("org.snakeyaml.engine", "${project.group}.${project.name.lowercase()}.snakeyaml.engine")
}

tasks.named("build") {
    dependsOn(tasks.named("shadowJar"))
}

configure<JavaPluginExtension> {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
}

tasks.named<ProcessResources>("processResources") {
    val props = mapOf("version" to version)
    inputs.properties(props)
    filteringCharset = "UTF-8"
    filesMatching("plugin.yml") {
        expand(props)
    }
}
