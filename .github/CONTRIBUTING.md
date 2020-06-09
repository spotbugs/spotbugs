# Contributing guideline

Before you submit issue or pull request, please check the following points.

## Before reporting a problem with detectors

When you find problems such as false-positives or false-negatives, consider to add a JUnit test case to reproduce.
Just three steps to follow:

1. Create a [minimum and complete](http://stackoverflow.com/help/mcve) .java file under `spotbugsTestCases/src/java` directory.
2. Create a unit test case under `spotbugs-tests/src/test/java` directory, Refer to [this commit](https://github.com/spotbugs/spotbugs/commit/c05c0f029c7ae4874791fddbd6e954c5908b80ff) as example.
3. Confirm that `./gradlew clean build` is failed by your new unit test case.

## Before you propose new rules

Please consider to follow the same points with ***Before reporting problem in detectors***.
