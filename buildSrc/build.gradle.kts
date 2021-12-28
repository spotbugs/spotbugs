plugins {
    `kotlin-dsl`
    id("com.diffplug.spotless") version "6.1.0"
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
