# Contributing guideline

Before you submit issue or pull request, please check following points.

## Before reporting problem in detectors

When you found problems like false-positive or false-negative, consider to add a JUnit test case to reproduce.
Just three steps to follow:

1. Create a [minimum and complete](http://stackoverflow.com/help/mcve) .java file under `findbugsTestCases/src/java` directory.
2. Create an unit test case under `findbugs/src/test/java` directory, Refer [pull request #69](https://github.com/spotbugs/spotbugs/pull/69/files) as example.
3. Confirm that `./gradlew clean findbugs:build` is failed by your new unit test case.

## Before you propose new rules

Please consider to follow the same points with ***Before reporting problem in detectors***.
