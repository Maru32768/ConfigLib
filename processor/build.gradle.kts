plugins {
    java
}

description = "Optional ConfigLib annotation processor for POJO annotation lint checks."

repositories {
    mavenCentral()
    maven {
        url = uri("https://libraries.minecraft.net")
    }
    maven { url = uri("https://jitpack.io") }
}

dependencies {
    implementation(project(":common"))
    testImplementation(project(":common"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.8.2")
}

tasks.register("prepareKotlinBuildScriptModel") {
}

tasks.test {
    useJUnitPlatform()
}
