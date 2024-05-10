plugins {
  id("com.gradle.develocity") version "3.17.3"
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
