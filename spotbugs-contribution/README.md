# SpotBugs visitors documentation contribution

This small scaffold helps extract a list of SpotBugs visitor/detector classes and produce a starter reStructuredText document you can submit as part of a documentation PR for issue #1454.

Files added:
- `docs/visitors-reference.rst` - starter document (generated or edited)
- `scripts/ExtractVisitors.java` - small scanner that finds likely detector classes in a SpotBugs source tree and emits an RST table to stdout
- `scripts/ExtractVisitorsImproved.java` - improved version that uses findbugs.xml as authoritative source
- `generated/visitors.inc` - auto-generated include file (committed for convenience, can be regenerated)

Quick usage

1. Build or obtain a local copy of the SpotBugs source (the scanner requires the source files):

   git clone https://github.com/spotbugs/spotbugs.git

2. Run the scanner (requires Java 8+):

   javac scripts/ExtractVisitors.java
   java -cp scripts ExtractVisitors path/to/spotbugs/spotbugs

3. Redirect the output to `docs/visitors-reference.rst` and edit descriptions/bug pattern mappings.

Notes

- The original scanner (`ExtractVisitors.java`) is heuristic-based: it looks for classes whose name or inheritance suggest they are detectors. 
- The improved version (`ExtractVisitorsImproved.java`) uses `findbugs.xml` as the authoritative source for all detector classes.
- Manual review is still required to add accurate descriptions and bug pattern mappings.

## Generated Files Policy

The `generated/visitors.inc` file is committed to git for convenience and to show the current state of the documentation. This follows common practices in documentation projects where generated content is committed when:
- It provides immediate value to reviewers and users
- The generation process requires specific build environment setup
- The generated content is relatively stable and doesn't change frequently

The file can always be regenerated using the scripts, but committing it allows others to see the current documentation state without running the generation tools.
