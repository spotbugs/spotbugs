# Release procedure

When you release fixed version of SpotBugs, please follow these procedures.

## Release to Maven Central

Add necessary properties to `~/.gradle/gradle.properties` and make sure that you have proper `eclipsePlugin/local.properties` file to release Eclipse plugin at the same time. Then run `./gradlew build smoketest uploadArchives`.

Check [SonaType official page](http://central.sonatype.org/pages/gradle.html) for detail.

## Release to Eclipse Marketplace

TBU

## Release to Eclipse Update Site

Send pull-request to spotbugs/spotbugs.github.io, to update contents.
As files to upload, you can use the zip file which is uploaded to Maven Central when you release to there.

See [this pull-request](https://github.com/spotbugs/spotbugs.github.io/pull/12) as example.

## Release to Gradle Plugin Portal

Add necessary properties to `~/.gradle/gradle.properties` and run `./gradlew publishPlugins`.

## Release to ReadTheDocs

TBU
