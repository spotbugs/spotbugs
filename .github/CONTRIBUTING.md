# Contributing guideline

Before you submit issue or pull request, please check the following points.

** Do not open intellij plugin issues here, open them at [intellij-plugin](https://github.com/JetBrains/spotbugs-intellij-plugin) **
** Search existing issues and pull requests to see if the issue was already discussed.
** Check our discussions to see if the issue was already discussed.
** Check for specific project we support to raise issue on, under [spotbugs](https://github.com/spotbugs)

## Before reporting a problem with detectors

When you find problems such as false-positives or false-negatives, consider to add a JUnit test case to reproduce.
Just three steps to follow:

1. Create a [minimum and complete](http://stackoverflow.com/help/mcve) .java file under `spotbugsTestCases/src/java` directory.
2. Create a unit test case under `spotbugs-tests/src/test/java` directory, Refer to [this commit](https://github.com/spotbugs/spotbugs/commit/c05c0f029c7ae4874791fddbd6e954c5908b80ff) as example.
3. Confirm that `./gradlew clean build` is failed by your new unit test case.

## Before you propose new rules

Please consider to follow the same points with ***Before reporting problem in detectors***.

## Before you submit a pull request

1. Run `./gradlew spotlessApply build smoketest` in your local to verify your change.
2. Make sure you updated the `CHANGELOG.md` accordingly. Detailed requirements are explained at the beginning of the changelog.
