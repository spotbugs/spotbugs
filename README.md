
# ![SpotBugs](https://spotbugs.github.io/images/logos/spotbugs_logo_300px.png)

[![Build Status](https://travis-ci.org/spotbugs/spotbugs.svg?branch=master)](https://travis-ci.org/spotbugs/spotbugs)
[![Coverage Status](https://coveralls.io/repos/github/spotbugs/spotbugs/badge.svg?branch=master)](https://coveralls.io/github/spotbugs/spotbugs?branch=master)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.github.spotbugs/spotbugs/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.github.spotbugs/spotbugs)
[![Javadocs](http://javadoc.io/badge/com.github.spotbugs/spotbugs.svg)](http://javadoc.io/doc/com.github.spotbugs/spotbugs)


[SpotBugs](https://spotbugs.github.io/) is the spiritual successor of [FindBugs](https://github.com/findbugsproject/findbugs), carrying on from the point where it left off with support of its community.


More information at the [official website](https://spotbugs.github.io/). A lot of things can still be found at the [old FindBugs website](http://findbugs.sourceforge.net).

# Build

SpotBugs is built using [Gradle](https://gradle.org). The recommended way to obtain it is to simply run the `gradlew` (or `gradlew.bat`) wrapper, which will automatically download and run the correct version as needed (using the settings in `gradle/wrapper/gradle-wrapper.properties`).

To see a list of build options, run `gradle tasks` (or `gradlew tasks`). The `build` task will perform a full build and test.

To build the FindBugs plugin for Eclipse, you'll need to create the file `eclipsePlugin/local.properties`, containing a property `eclipsePlugin.dir` that points to an Eclipse installation's plugin directory (see `.travis.yml` for an example), then run the build, or at least `./gradlew :eclipsePlugin:updateManifest` to setup the manifest / properties used by PDE.
