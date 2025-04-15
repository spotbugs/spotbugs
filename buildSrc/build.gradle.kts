plugins {
    `kotlin-dsl`
    id("com.diffplug.spotless") version "7.0.3"
}

repositories {
    gradlePluginPortal()
}
dependencies {
    implementation("com.diffplug.gradle:goomph:4.3.0")
    implementation("com.diffplug.spotless:spotless-plugin-gradle:7.0.3")
}
