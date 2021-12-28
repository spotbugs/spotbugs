plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
}
dependencies {
    implementation("com.diffplug.gradle:goomph:3.34.0") {
        exclude("com.diffplug.spotless", "spotless-lib")
    }
}
