plugins {
  id("com.gradle.enterprise") version "3.15"
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

gradleEnterprise {
  buildScan {
    termsOfServiceUrl = "https://gradle.com/terms-of-service"
    termsOfServiceAgree = "yes"
  }
}
