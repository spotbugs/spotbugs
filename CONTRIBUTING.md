# Contributing guideline

Before you submit issue or pull request, please check the following points.

## Before reporting a problem with detectors

When you find problems such as false-positives or false-negatives, consider to add a JUnit test case to reproduce.
Just three steps to follow:

1. Create a [minimum and complete](http://stackoverflow.com/help/mcve) .java file under `spotbugsTestCases/src/java` directory.
2. Create a unit test case under `spotbugs/src/test/java` directory, Refer [pull request #69](https://github.com/spotbugs/spotbugs/pull/69/files) as example.
3. Confirm that `./gradlew clean spotbugs:build` is failed by your new unit test case.

## Before you propose new rules

Please consider to follow the same points with ***Before reporting problem in detectors***.
