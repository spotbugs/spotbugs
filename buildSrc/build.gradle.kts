plugins {
    `kotlin-dsl`
    id("com.diffplug.spotless") version "5.16.0"
}

repositories {
    gradlePluginPortal()
}
dependencies {
    implementation("com.diffplug.gradle:goomph:3.34.0")
}

spotless {
    kotlinGradle {
        ktlint()
    }
}
