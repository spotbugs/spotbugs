# SpotBugs Module Map

SpotBugs is a multi-module Gradle build for bytecode analysis, integrations, and test infrastructure.

## Core modules

- `spotbugs/`  
  Core engine and built-in detectors. Key runtime metadata lives in `spotbugs/etc/findbugs.xml` and `spotbugs/etc/messages*.xml`.
- `spotbugs-annotations/`  
  Public annotations consumed by analyzed code and detectors (`edu.umd.cs.findbugs.annotations`).
- `spotbugs-ant/`  
  Ant task integration for running SpotBugs in Ant builds.

## Testing modules

- `spotbugs-tests/`  
  JUnit-based regression tests for detectors and engine behavior.
- `spotbugsTestCases/`  
  Java/Groovy sample sources and compiled artifacts used as analysis targets in tests.
- `test-harness-core/`  
  Core analysis runner (`AnalysisRunner`) used by tests and plugin developers.
- `test-harness/`  
  Hamcrest matchers and shared helpers for asserting bug instances.
- `test-harness-jupiter/`  
  JUnit 5 extension/runner integration (`SpotBugsExtension`, `SpotBugsRunner`).

## IDE modules

- `eclipsePlugin/`  
  Eclipse plugin packaging and metadata generation.
- `eclipsePlugin-test/`, `eclipsePlugin-junit/`  
  Eclipse plugin test/support modules.

## Build support

- `buildSrc/`  
  Shared Gradle conventions and build logic plugins.
- `gradle/`  
  Shared Gradle scripts (Spotless, Checkstyle, test JVM args, publishing, etc.).

## Dependency flow (high level)

- Most modules depend on `:spotbugs`.
- `:spotbugs-tests` depends on `:spotbugs`, `:spotbugsTestCases`, and test harness modules.
- `:test-harness` and `:test-harness-jupiter` depend on `:test-harness-core`.
- `:spotbugs` depends on `:spotbugs-annotations` and packages engine + GUI sources.

## Ownership rule for edits

Before changing code, identify the owning module first.  
If behavior spans modules (for example detector behavior + test harness), update tests in `spotbugs-tests` in the same change.
For detector behavior changes, expect coordinated updates across `spotbugs` + `spotbugs-tests` + `spotbugsTestCases`.
If bug type metadata changes, update `spotbugs/etc/findbugs.xml` and `spotbugs/etc/messages*.xml` together.
