allprojects {
    // TODO: Revisit this local project group after the temporary local CommandLib composite build is removed.
    // ConfigLib and CommandLib both have "common", "bukkit", and "forge" subprojects; keeping the same local
    // Gradle coordinates makes composite-build substitution redirect CommandLib internals back to ConfigLib.
    group = "net.kunmc.lab.configlib"
    version = "0.21.0"
}

val publishedProjects = setOf("common", "bukkit", "forge", "processor")

subprojects {
    apply(plugin = "java")
    apply(plugin = "java-library")
    apply(plugin = "idea")

    configure<JavaPluginExtension> {
        toolchain.languageVersion.set(JavaLanguageVersion.of(11))
        withSourcesJar()
    }

    configure<org.gradle.plugins.ide.idea.model.IdeaModel> {
        module {
            isDownloadJavadoc = true
            isDownloadSources = true
        }
    }

    tasks.withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
    }

    if (project.name in publishedProjects) {
        apply(plugin = "maven-publish")

        configure<PublishingExtension> {
            publications {
                create<MavenPublication>("maven") {
                    groupId = "net.kunmc.lab"
                    artifactId = project.name
                    version = project.version.toString()
                    from(components["java"])
                    pom {
                        licenses {
                            license {
                                name.set("MIT License")
                                url.set("https://opensource.org/licenses/MIT")
                            }
                        }
                    }
                }
            }
        }
    }

    tasks.withType<Javadoc>().configureEach {
        (options as StandardJavadocDocletOptions).apply {
            charSet = "UTF-8"
            encoding = "UTF-8"
        }
    }

    tasks.named<Jar>("jar") {
        doFirst {
            copy {
                from(rootProject.rootDir)
                into(layout.buildDirectory.dir("resources/main"))
                include("LICENSE*")
            }
        }
    }

    tasks.named<Jar>("sourcesJar") {
        from(rootProject.file("README.md"))
    }
}
