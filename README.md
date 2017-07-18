
# ![SpotBugs](https://spotbugs.github.io/images/logos/spotbugs_logo_300px.png)

[![Build Status](https://travis-ci.org/spotbugs/spotbugs.svg?branch=master)](https://travis-ci.org/spotbugs/spotbugs)
[![Coverage Status](https://coveralls.io/repos/github/spotbugs/spotbugs/badge.svg?branch=master)](https://coveralls.io/github/spotbugs/spotbugs?branch=master)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.github.spotbugs/spotbugs/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.github.spotbugs/spotbugs)
[![Javadocs](http://javadoc.io/badge/com.github.spotbugs/spotbugs.svg)](http://javadoc.io/doc/com.github.spotbugs/spotbugs)


[SpotBugs](https://spotbugs.github.io/) is the spiritual successor of [FindBugs](https://github.com/findbugsproject/findbugs), carrying on from the point where it left off with support of its community.

SpotBugs is licensed under the [GNU LESSER GENERAL PUBLIC LICENSE](https://github.com/spotbugs/spotbugs/blob/master/spotbugs/licenses/LICENSE.txt).

More information at the [official website](https://spotbugs.github.io/). A lot of things can still be found at the [old FindBugs website](http://findbugs.sourceforge.net).

# Build

SpotBugs is built using [Gradle](https://gradle.org). The recommended way to obtain it is to simply run the `gradlew` (or `gradlew.bat`) wrapper, which will automatically download and run the correct version as needed (using the settings in `gradle/wrapper/gradle-wrapper.properties`).

To see a list of build options, run `gradle tasks` (or `gradlew tasks`). The `build` task will perform a full build and test.

To build the SpotBugs plugin for Eclipse, you'll need to create the file `eclipsePlugin/local.properties`, containing a property `eclipseRoot.dir` that points to an Eclipse installation's root directory (see `.travis.yml` for an example), then run the build.
To prepare Eclipse environment only, run `./gradlew eclipse`. See also [detailed steps](https://github.com/spotbugs/spotbugs/blob/master/eclipsePlugin/doc/building_spotbugs_plugin.txt).

# Questions?
You can contact us using [our general purpose mailing list](https://github.com/spotbugs/discuss/issues?q=).
