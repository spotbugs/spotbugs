# SpotBugs Copilot Instructions

Read these first:

- `docs/module-map.md`
- `docs/architecture.md`
- `docs/testing.md`
- `docs/detector-development.md`

## Repository focus

SpotBugs is a Java bytecode static-analysis engine with detector plugins.
Most behavior changes are in `spotbugs/` and usually require matching updates in `spotbugs-tests/`.

## Build and test

Primary tool: Gradle wrapper.

- Full verification (project guideline): `./gradlew spotlessApply build smoketest`
- Run all tests: `./gradlew test`
- Detector regression tests: `./gradlew :spotbugs-tests:test`
- Focused test: `./gradlew --configure-on-demand :spotbugs-tests:test --tests <fully.qualified.TestClass>`

Use the smallest relevant test scope first, then expand.

## Detector changes

When touching detector behavior:

1. Locate detector code in `spotbugs/src/main/java/edu/umd/cs/findbugs/detect/`.
2. Confirm metadata impact in `spotbugs/etc/findbugs.xml` and `spotbugs/etc/messages*.xml`.
3. Update/add regression tests in `spotbugs-tests` and inputs in `spotbugsTestCases`.
4. Preserve bug type IDs unless behavior intentionally changes.

## Key architecture points

- Engine orchestration: `edu.umd.cs.findbugs.FindBugs2`
- Detector/plugin registry: `DetectorFactoryCollection`, `PluginLoader`
- Detector ordering/passes: `plan/ExecutionPlan`
- Output model: `BugInstance`, `BugReporter`, `SortedBugCollection`

## High-caution areas

Change only with strong context and tests:

- `FindBugs2` analysis loop
- Execution plan ordering logic
- Plugin metadata loading
- `spotbugs/etc/findbugs.xml` ordering constraints
- Shared analysis caches/context and bug reporting pipeline

## Conventions

- Make minimal, module-owned changes.
- Keep tests close to behavior changes.
- Avoid unrelated refactors in the same PR.
- Prefer targeted test runs during iteration, then broader validation.
