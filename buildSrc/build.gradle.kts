plugins {
    `kotlin-dsl`
    id("com.diffplug.spotless") version "8.1.0"
}

repositories {
    gradlePluginPortal()
}
dependencies {
    implementation("com.diffplug.gradle:goomph:4.4.1")
    implementation("com.diffplug.spotless:spotless-plugin-gradle:8.1.0")
}
