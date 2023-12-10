plugins {
    `kotlin-dsl`
    id("com.diffplug.spotless") version "6.23.3"
}

repositories {
    gradlePluginPortal()
}
dependencies {
    implementation("com.diffplug.gradle:goomph:3.44.0")
    implementation("com.diffplug.spotless:spotless-plugin-gradle:6.23.3")
}
