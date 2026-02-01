pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }

    resolutionStrategy {
        eachPlugin {
            if (requested.id.id == "org.sonarqube") {
                useModule(
                    "org.sonarsource.scanner.gradle:sonarqube-gradle-plugin:7.2.2.6593"
                )
            }
        }
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }

    resolutionStrategy {
        dependencySubstitution {
            substitute(module("commons-io:commons-io"))
                .using(module("commons-io:commons-io:2.15.1"))
        }
    }
}

plugins {
  id("com.gradle.develocity") version "4.3.1"
}

include(":eclipsePlugin")
include(":eclipsePlugin-test")
include(":eclipsePlugin-junit")
include(":spotbugs")
include(":spotbugs-annotations")
include(":spotbugs-ant")
include(":spotbugs-tests")
include(":spotbugsTestCases")
include(":test-harness")
include(":test-harness-core")
include(":test-harness-jupiter")

rootProject.name = "spotbugs"

develocity {
  buildScan {
    termsOfUseUrl = "https://gradle.com/terms-of-service"
    termsOfUseAgree = "yes"
  }
}
