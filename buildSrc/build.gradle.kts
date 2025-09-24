plugins {
    `kotlin-dsl`
    id("com.diffplug.spotless") version "8.0.0"
}

repositories {
    gradlePluginPortal()
}
dependencies {
    implementation("com.diffplug.gradle:goomph:4.3.0")
    implementation("com.diffplug.spotless:spotless-plugin-gradle:8.0.0")
}
