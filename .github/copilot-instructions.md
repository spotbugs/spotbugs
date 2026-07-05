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

- Use `./gradlew` from repo root; do not rely on system `gradle`.
- JDK 21 is required to run the full project build/test matrix.
- SpotBugs core artifacts are compiled with Java release 11 (`options.release = 11`).
- Full verification (project guideline): `./gradlew spotlessApply build smoketest`
- Run all tests: `./gradlew test`
- Detector regression tests: `./gradlew :spotbugs-tests:test`
- Focused test: `./gradlew --configure-on-demand :spotbugs-tests:test --tests <fully.qualified.TestClass>`
- In sandboxed environments where Eclipse downloads fail, keep runs module-scoped with `--configure-on-demand`.

Use the smallest relevant test scope first, then expand.

## Detector changes

When touching detector behavior:

1. Start from the reported bug type, detector class, or failing test and narrow scope before exploring unrelated engine code.
2. Reproduce with a focused failing test first (or add one) before changing detector logic.
3. Locate detector code in `spotbugs/src/main/java/edu/umd/cs/findbugs/detect/`.
4. Check for an existing regression test in `spotbugs-tests` for that detector or bug pattern before creating a new test class.
5. Update/add regression tests in `spotbugs-tests` and minimal inputs in `spotbugsTestCases`.
6. Confirm metadata impact in `spotbugs/etc/findbugs.xml` and `spotbugs/etc/messages*.xml` only if emitted bug types, detector registration, ordering, category/code mappings, or user-visible bug descriptions changed.
7. Preserve existing bug type IDs and detector IDs; do not rename/remove them for behavior tweaks.
8. Do not add a new bug type, detector, or engine-level workaround for a detector bug unless the issue explicitly requires new externally visible behavior.
9. Capture whether the fix targets a false positive, false negative, crash, or metadata mismatch; tests should assert that direction explicitly.

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

For detector bug fixes, default scope is the detector class, the closest regression test, and the minimal repro input. Do not edit engine/planning files unless the failing test or call path demonstrates the bug is outside detector-local logic.

## Conventions

- Make minimal, module-owned changes.
- Keep tests close to behavior changes.
- Avoid unrelated refactors in the same PR.
- Prefer targeted test runs during iteration, then broader validation.
- For detector fixes, validate in this order: focused regression test, `./gradlew --configure-on-demand :spotbugs-tests:test`, then broader project checks only if scope expands.
- Run `LoadMessagesTest` only when metadata files change.
- Do not modify `findbugs.xml` ordering constraints (`SplitPass`/`WithinPass`) without targeted regression coverage.
