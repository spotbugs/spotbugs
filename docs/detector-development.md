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

1. Reproduce the issue first with a focused failing test (or add one).
2. Add/update a minimal repro input in `spotbugsTestCases` and wire it in `spotbugs-tests`.
3. Locate the existing detector class and related helper/database detectors.
4. Identify bug pattern IDs and categories in `findbugs.xml`.
5. Update detector logic with pass ordering and shared-database side effects in mind.
6. Update `messages.xml` only when bug descriptions or detector details must change.

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

## Safe-change checklist

Before merging detector changes:

- Confirm a failing test existed before detector logic changes and now passes.
- Preserve existing bug type identifiers unless behavior intentionally changes.
- Verify no unintended category/code/detector mapping changes in metadata.
- Run focused tests first, then broader tests based on scope (`:spotbugs-tests:test` before full build).
- Review interactions with suppression/filtering and bug rank/priority behavior.
- Ensure cross-module updates are complete: `spotbugs` + `spotbugs-tests` + `spotbugsTestCases` when behavior changes.

## High-risk areas for detector work

- Shared analysis databases and first-pass detectors
- Detector ordering constraints
- Global metadata (`findbugs.xml`, `messages.xml`)
- Core engine execution (`FindBugs2`, `ExecutionPlan`)
