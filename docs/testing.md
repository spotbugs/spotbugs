# SpotBugs Testing Guide

## Test structure

- `spotbugs-tests/` contains detector and engine regression tests.
- `spotbugsTestCases/` contains source/bytecode inputs analyzed by tests.
- `test-harness-core/` (`AnalysisRunner`) executes SpotBugs programmatically.
- `test-harness/` and `test-harness-jupiter/` provide assertion helpers and JUnit 5 integration.

Common pattern:

1. Add or update a minimal repro case in `spotbugsTestCases/src/...`.
2. Add or update a test in `spotbugs-tests/src/test/java/...`.
3. Assert expected bug presence/absence with harness matchers.

## Useful commands

- Full local verification before PR (project guideline):  
  `./gradlew spotlessApply build smoketest`
- Run all tests:  
  `./gradlew test`
- Run detector/engine test module:  
  `./gradlew :spotbugs-tests:test`
- Run a focused test (recommended for quick iteration):  
  `./gradlew --configure-on-demand :spotbugs-tests:test --tests <fully.qualified.TestClass>`

Example focused check for message metadata:

- `./gradlew --configure-on-demand :spotbugs-tests:test --tests edu.umd.cs.findbugs.LoadMessagesTest`

## Detector regression expectations

When changing detector logic, analysis behavior, or bug metadata:

- Update existing regression tests where possible.
- Add a targeted regression test for the new/fixed behavior.
- Cover both positive findings and false-positive avoidance when practical.
- Keep bug type identifiers stable unless intentionally changing behavior and documentation.

## What to validate before merge

- Relevant focused tests for the changed detector/engine area.
- At least module-level test run (`:spotbugs-tests:test`) for detector changes.
- Full verification command when change scope is broad.
