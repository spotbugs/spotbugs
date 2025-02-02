plugins {
    `kotlin-dsl`
    id("com.diffplug.spotless") version "7.0.2"
}

repositories {
    gradlePluginPortal()
}
dependencies {
    implementation("com.diffplug.gradle:goomph:4.2.0")
    implementation("com.diffplug.spotless:spotless-plugin-gradle:7.0.2")
}
