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

1. Locate the existing detector class and related helper/database detectors.
2. Identify bug pattern IDs and categories in `findbugs.xml`.
3. Update detector logic with ordering/side effects in mind.
4. Update `messages.xml` only when bug descriptions or detector details must change.
5. Add/update regression tests in `spotbugs-tests` with samples in `spotbugsTestCases`.

## Ordering and pass constraints

Detector behavior can depend on pass order and prerequisite detectors.

- Ordering constraints are declared in `findbugs.xml` (`SplitPass`, `WithinPass`).
- `ExecutionPlan` enforces detector ordering and may force-enable dependencies.

Do not change detector ordering metadata casually; verify related tests.

## Safe-change checklist

Before merging detector changes:

- Preserve existing bug type identifiers unless behavior intentionally changes.
- Verify no unintended category/code changes in metadata.
- Run focused tests first, then broader tests based on scope.
- Review interactions with suppression/filtering and bug rank/priority behavior.

## High-risk areas for detector work

- Shared analysis databases and first-pass detectors
- Detector ordering constraints
- Global metadata (`findbugs.xml`, `messages.xml`)
- Core engine execution (`FindBugs2`, `ExecutionPlan`)
