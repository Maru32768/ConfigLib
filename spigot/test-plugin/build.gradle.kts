import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.net.URL

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
    flatDir { dirs("server/cache", "libs") }
}

dependencies {
//    compileOnly(mapOf("name" to "patched_1.16.5"))
    compileOnly("com.destroystokyo.paper:paper-api:1.16.5-R0.1-SNAPSHOT")
    implementation("com.google.code.gson:gson:2.10")
    implementation("org.snakeyaml:snakeyaml-engine:2.9")
    implementation("com.github.Maru32768.CommandLib:spigot:0.17.1")
}

configure<JavaPluginExtension> {
    toolchain.languageVersion.set(JavaLanguageVersion.of(11))
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"

    // patched_1.16.5 内の古い GSON より implementation の GSON を優先する
    val implJars = configurations.getByName("compileClasspath")
        .filter { it.name.startsWith("gson") }
    classpath = files(implJars) + classpath
}

tasks.named<Jar>("jar") {
    doFirst {
        copy {
            from(".")
            into(layout.buildDirectory.dir("resources/main"))
            include("LICENSE*")
        }
    }
}

configure<SourceSetContainer> {
    named("main") {
        java.srcDir("../src/main/java")
        java.srcDir("../../common/src/main/java")
    }
}

tasks.named<ShadowJar>("shadowJar") {
    archiveFileName.set("${rootProject.name}-${project.version}.jar")
    relocate("net.kunmc.lab.commandlib", "${project.group}.${project.name.lowercase()}.commandlib")
    relocate("net.kunmc.lab.configlib", "${project.group}.${project.name.lowercase()}.configlib")
    relocate("com.google.gson", "${project.group}.${project.name.lowercase()}.gson")
    relocate("org.snakeyaml.engine", "${project.group}.${project.name.toLowerCase()}.snakeyaml.engine")
}
tasks.named("build") { dependsOn(tasks.named("shadowJar")) }

tasks.named<ProcessResources>("processResources") {
    val props =
        mapOf("name" to rootProject.name, "version" to version, "MainClass" to getMainClassFQDN(projectDir.toPath()))
    inputs.properties(props)
    filteringCharset = "UTF-8"
    filesMatching("plugin.yml") {
        expand(props)
    }
}

tasks.register<Copy>("copyToServer") {
    group = "copy"
    mustRunAfter(tasks.named("build"))
    from(layout.buildDirectory.file("libs/${rootProject.name}-${version}.jar"))
    into("./server/plugins")
}

tasks.register("buildAndCopy") {
    group = "build"
    dependsOn(tasks.named("build"), tasks.named("copyToServer"), tasks.named("downloadPluginJars"))
}

tasks.register("downloadServerJar") {
    val url =
        uri("https://api.papermc.io/v2/projects/paper/versions/1.16.5/builds/794/downloads/paper-1.16.5-794.jar").toURL()
    val file = projectDir.toPath().toAbsolutePath().resolve("server/server.jar").toFile()
    if (!file.exists()) {
        url.openStream().use { stream ->
            Files.copy(stream, file.toPath(), StandardCopyOption.REPLACE_EXISTING)
        }
    }
}

tasks.register("downloadPluginJars") {
    group = "setup"
    doLast {
        downloadIfMissing(
            "https://edge.forgecdn.net/files/4529/697/PlugManX.jar",
            projectDir.toPath().resolve("server/plugins/PlugManX.jar").toFile()
        )
        downloadIfMissing(
            providers.gradleProperty("autoReloaderJarDownloadUrl")
                .orElse(providers.environmentVariable("CONFIGLIB_AUTORELOADER_JAR_URL"))
                .orElse("https://github.com/Maru32768/AutoReloader/releases/download/1.1.0/AutoReloader-1.1.0.jar")
                .get(),
            projectDir.toPath().resolve("server/plugins/AutoReloader-1.1.0.jar").toFile()
        )
    }
}

tasks.register("generatePatchedJar") {
    group = "setup"
    dependsOn(tasks.named("downloadServerJar"))
    val serverDir = projectDir.toPath().toAbsolutePath().resolve("server").toString()
    val file = Path.of(serverDir, "cache/patched_1.16.5.jar").toFile()
    if (!file.exists()) {
        val p = Runtime.getRuntime()
            .exec(arrayOf("java", "-jar", "$serverDir/server.jar", "nogui"), emptyArray(), Path.of(serverDir).toFile())
        p.waitFor()
        p.destroy()
    }
}

tasks.register("copyDefaultServerProperties") {
    group = "setup"
    dependsOn(tasks.named("downloadServerJar"))
    val serverDir = projectDir.toPath().toAbsolutePath().resolve("server")
    val dst = serverDir.resolve("server.properties")
    if (!dst.toFile().exists()) {
        Files.copy(serverDir.resolve("server.default.properties"), dst)
    }
}

fun getMainClassFQDN(projectPath: Path): String {
    return Files.walk(projectPath)
        .filter { it.fileName.toString().endsWith(".java") }
        .filter { path -> Files.lines(path).anyMatch { it.contains("extends JavaPlugin") } }
        .findFirst()
        .get()
        .toString()
        .replace("\\", ".").replace("/", ".")
        .replace(Regex(".*src\\.main\\.java\\.|.java$"), "")
}

fun downloadIfMissing(urlString: String, outputFile: File) {
    if (outputFile.exists()) {
        return
    }

    outputFile.parentFile.mkdirs()
    URL(urlString).openStream().use { input ->
        outputFile.outputStream().use { output ->
            input.copyTo(output)
        }
    }
}
