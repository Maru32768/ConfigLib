plugins {
    java
}

repositories {
    mavenCentral()
    maven {
        url = uri("https://libraries.minecraft.net")
    }
    maven { url = uri("https://jitpack.io") }
}

dependencies {
    implementation(project(":common"))
    annotationProcessor(project(":processor"))
}

tasks.register("prepareKotlinBuildScriptModel") {
}
