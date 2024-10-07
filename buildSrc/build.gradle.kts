plugins {
    `kotlin-dsl`
    id("com.diffplug.spotless") version "6.25.0"
}

repositories {
    gradlePluginPortal()
}
dependencies {
    implementation("com.diffplug.gradle:goomph:4.1.0")
    implementation("com.diffplug.spotless:spotless-plugin-gradle:6.25.0")
}
