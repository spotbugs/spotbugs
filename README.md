
# ![SpotBugs](https://spotbugs.github.io/images/logos/spotbugs_logo_300px.png)

[![Build Status](https://github.com/spotbugs/spotbugs/workflows/build/badge.svg)](https://github.com/spotbugs/spotbugs/actions)
[![Documentation Status](https://readthedocs.org/projects/spotbugs/badge/?version=latest)](https://spotbugs.readthedocs.io/en/latest/?badge=latest)
[![Coverage Status](https://sonarcloud.io/api/project_badges/measure?branch=release-3.1&project=com.github.spotbugs.spotbugs&metric=coverage)](https://sonarcloud.io/component_measures?id=com.github.spotbugs.spotbugs&metric=coverage)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.github.spotbugs/spotbugs/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.github.spotbugs/spotbugs)
[![Javadocs](https://javadoc.io/badge/com.github.spotbugs/spotbugs.svg)](https://javadoc.io/doc/com.github.spotbugs/spotbugs)

[SpotBugs](https://spotbugs.github.io/) is the spiritual successor of [FindBugs](https://github.com/findbugsproject/findbugs), carrying on from the point where it left off with support of its community.

SpotBugs is licensed under the [GNU LESSER GENERAL PUBLIC LICENSE](https://github.com/spotbugs/spotbugs/blob/master/spotbugs/licenses/LICENSE.txt).

More information at the [official website](https://spotbugs.github.io/). A lot of things can still be found at the [old FindBugs website](https://findbugs.sourceforge.net).

# Build

SpotBugs is built using [Gradle](https://gradle.org). The recommended way to obtain it is to simply run the `gradlew` (or `gradlew.bat`) wrapper, which will automatically download and run the correct version as needed (using the settings in `gradle/wrapper/gradle-wrapper.properties`).

To see a list of build options, run `gradle tasks` (or `gradlew tasks`). The `build` task will perform a full build and test.

To build the SpotBugs plugin for Eclipse, you'll need to create the file `eclipsePlugin/local.properties`, containing a property `eclipseRoot.dir` that points to an Eclipse installation's root directory (see `.github/workflows/release.yml` for an example), then run the build.
To prepare Eclipse environment only, run `./gradlew eclipse`. See also [detailed steps](https://github.com/spotbugs/spotbugs/blob/release-3.1/eclipsePlugin/doc/building_spotbugs_plugin.txt).

# Using SpotBugs

SpotBugs can be used standalone and through several integrations, including:

* [Ant](https://spotbugs.readthedocs.io/en/latest/ant.html)
* [Maven](https://spotbugs.readthedocs.io/en/latest/maven.html)
* [Gradle](https://spotbugs.readthedocs.io/en/latest/gradle.html)
* [Eclipse](https://spotbugs.readthedocs.io/en/latest/eclipse.html)
* [Sonarqube](https://github.com/spotbugs/sonar-findbugs)
* [IntelliJ IDEA](https://github.com/JetBrains/spotbugs-intellij-plugin)

# Questions?
You can contact us using [GitHub Discussions](https://github.com/spotbugs/spotbugs/discussions).
