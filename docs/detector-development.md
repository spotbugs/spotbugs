# Detector Development in SpotBugs

## Core detector model

Detectors are discovered from plugin metadata and executed by the analysis engine.

Key extension points:

- `Detector` / `Detector2` APIs
- Base classes such as bytecode and opcode-stack scanners
- Plugin metadata in `findbugs.xml`
- Bug pattern messages in `messages.xml`

Core runtime locations in this repository:

- Detector code: `spotbugs/src/main/java/edu/umd/cs/findbugs/detect/`
- Plugin metadata: `spotbugs/etc/findbugs.xml`
- Bug messages/categories: `spotbugs/etc/messages*.xml`

## Workflow for detector changes

1. Start from the reported bug type, detector class, or failing test before exploring broader engine code.
2. Reproduce the issue first with a focused failing test (or add one).
3. Check for an existing regression test for the detector or bug pattern in `spotbugs-tests` before creating a new test class.
4. Add/update a minimal repro input in `spotbugsTestCases` and wire it in `spotbugs-tests`.
5. Locate the existing detector class and related helper/database detectors.
6. Identify bug pattern IDs and categories in `findbugs.xml`.
7. Update detector logic with pass ordering and shared-database side effects in mind.
8. Update `messages.xml` only when bug descriptions or detector details must change.

## Narrowing the fix

For a normal detector bug, keep the first pass of investigation to:

- The detector class in `spotbugs/src/main/java/edu/umd/cs/findbugs/detect/`
- The nearest existing regression test in `spotbugs-tests/src/test/java/...`
- The minimal repro input in `spotbugsTestCases/src/...`

Do not jump to `FindBugs2`, `ExecutionPlan`, plugin loading, or shared analysis infrastructure unless the failing test or call path shows the bug is not detector-local.

Before editing, identify whether the issue is a:

- false positive
- false negative
- crash/exception during analysis
- metadata mismatch

The regression test should make that intent explicit so the fix does not accidentally trade one failure mode for another.

## Ordering and pass constraints

Detector behavior can depend on pass order and prerequisite detectors.

- Ordering constraints are declared in `findbugs.xml` (`SplitPass`, `WithinPass`).
- `ExecutionPlan` enforces detector ordering and may force-enable dependencies.

Do not change detector ordering metadata casually; verify related tests.

`ExecutionPlan` may forcibly enable prerequisite detectors from ordering constraints.  
Changing `SplitPass`/`WithinPass` metadata can affect detectors beyond the one you touched.

## Metadata invariants (hard constraints)

- Existing bug type IDs are stable external identifiers; do not rename or reuse them.
- Existing detector `class`/`reports` mappings in `findbugs.xml` must stay consistent with emitted bug types.
- If a bug type is added or intentionally changed, update both `spotbugs/etc/findbugs.xml` and `spotbugs/etc/messages*.xml` in the same change.
- Keep category and code mappings stable unless the change explicitly redefines behavior and tests cover migration impact.
- After metadata edits, run `LoadMessagesTest` and relevant detector regression tests.
- Logic-only detector fixes usually do not require metadata edits. Do not touch `findbugs.xml` or `messages*.xml` unless bug types, detector registration, ordering, category/code mappings, or user-visible descriptions changed.
- Do not add a new bug type or detector for a detector bug fix unless the issue explicitly requires new externally visible behavior.

## Test placement rules

- Prefer extending an existing detector regression test class when one already covers the detector or bug pattern.
- Add a new regression test class only when no suitable existing test owns that detector behavior.
- Keep repro inputs as small as possible in `spotbugsTestCases/src/...`; avoid broad fixture churn when a single focused class is enough.
- When practical, assert both the expected finding and the absence of the known false positive/false negative regression.

## Safe-change checklist

Before merging detector changes:

- Confirm a failing test existed before detector logic changes and now passes.
- Preserve existing bug type identifiers unless behavior intentionally changes.
- Verify no unintended category/code/detector mapping changes in metadata.
- Run focused tests first, then broader tests based on scope.
- Use this validation order for detector fixes:
  1. Focused regression test (`./gradlew --configure-on-demand :spotbugs-tests:test --tests <fully.qualified.TestClass>`)
  2. Detector/module suite (`./gradlew --configure-on-demand :spotbugs-tests:test`)
  3. `LoadMessagesTest` if metadata changed
  4. Broader project checks only if scope expanded beyond detector-local changes
- Review interactions with suppression/filtering and bug rank/priority behavior.
- Ensure cross-module updates are complete: `spotbugs` + `spotbugs-tests` + `spotbugsTestCases` when behavior changes.

## High-risk areas for detector work

- Shared analysis databases and first-pass detectors
- Detector ordering constraints
- Global metadata (`findbugs.xml`, `messages.xml`)
- Core engine execution (`FindBugs2`, `ExecutionPlan`)
