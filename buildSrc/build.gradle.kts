plugins {
    `kotlin-dsl`
    id("com.diffplug.spotless") version "8.2.1"
}

repositories {
    gradlePluginPortal()
}
dependencies {
    implementation("com.diffplug.gradle:goomph:4.4.1")
    implementation("com.diffplug.spotless:spotless-plugin-gradle:8.2.0")
}
