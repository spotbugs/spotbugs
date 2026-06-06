# SpotBugs Architecture

## Analysis pipeline

Primary entry point: `edu.umd.cs.findbugs.FindBugs2`.

At a high level, `FindBugs2.execute()` does:

1. Build classpath and analysis cache.
2. Build analysis context (`AnalysisContext`) and class lists.
3. Load detector/plugin metadata (`DetectorFactoryCollection`, `PluginLoader`).
4. Build detector execution plan (`ExecutionPlan`) from ordering constraints and enabled detectors.
5. Analyze classes pass-by-pass and detector-by-detector.
6. Emit bugs through `BugReporter` into a bug collection.

Important implementation points:

- Execution planning uses ordering constraints from plugin metadata (`findbugs.xml`).
- In multi-pass plans, first pass runs on `referencedClassSet`; later passes run on `appClassList`.
- First pass is typically non-reporting data collection; changing first-pass detectors can shift later-pass outcomes.
- Per-class detector execution can run via executor service.
- `finishPass()` is invoked once per detector after each pass-wide class traversal.

## Detector lifecycle

Detectors are loaded from plugin metadata and instantiated per pass:

- Detector definitions and ordering live in `spotbugs/etc/findbugs.xml`.
- Human-readable metadata lives in `spotbugs/etc/messages*.xml`.
- `ExecutionPlan` resolves enabled detectors and ordering constraints.
- `ExecutionPlan` can forcibly enable prerequisite detectors required by ordering constraints.
- For each class in a pass, SpotBugs calls detector visit logic.
- After each pass, detectors receive `finishPass()`.

## Bug reporting flow

- Detectors create/report `BugInstance` objects.
- `BugReporter` implementations/decorators process filtering, suppression, thresholds, and output concerns.
- Final aggregated results are stored in `SortedBugCollection`.

Key classes:

- `BugInstance`
- `BugReporter`
- `SortedBugCollection`

## Plugin and extension model

- Plugins are loaded by `PluginLoader`.
- `DetectorFactoryCollection` tracks loaded plugins, factories, bug patterns, and categories.
- Plugin metadata files (`findbugs.xml`, `messages.xml`) are part of runtime behavior, not docs-only files.

## High-risk architecture areas

Changes here can affect many detectors at once:

- `FindBugs2` analysis loop and pass orchestration
- `ExecutionPlan` ordering logic
- `DetectorFactoryCollection` / `PluginLoader` metadata loading
- `AnalysisContext` and classfile cache behavior
- `BugReporter`/`SortedBugCollection` handling
- Ordering and definitions in `spotbugs/etc/findbugs.xml`
