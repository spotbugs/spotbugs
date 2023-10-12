plugins {
    `kotlin-dsl`
    id("com.diffplug.spotless") version "6.22.0"
}

repositories {
    gradlePluginPortal()
}
dependencies {
    implementation("com.diffplug.gradle:goomph:3.43.0")
    implementation("com.diffplug.spotless:spotless-plugin-gradle:6.22.0")
}
