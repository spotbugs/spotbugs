# SpotBugs visitors documentation contribution

This small scaffold helps extract a list of SpotBugs visitor/detector classes and produce a starter reStructuredText document you can submit as part of a documentation PR for issue #1454.

Files added:
- `docs/visitors-reference.rst` - starter document (generated or edited)
- `scripts/ExtractVisitors.java` - small scanner that finds likely detector classes in a SpotBugs source tree and emits an RST table to stdout

Quick usage

1. Build or obtain a local copy of the SpotBugs source (the scanner requires the source files):

   git clone https://github.com/spotbugs/spotbugs.git

2. Run the scanner (requires Java 8+):

   javac scripts/ExtractVisitors.java
   java -cp scripts ExtractVisitors path/to/spotbugs/spotbugs

3. Redirect the output to `docs/visitors-reference.rst` and edit descriptions/bug pattern mappings.

Notes

- The scanner is heuristic-based: it looks for classes whose name or inheritance suggest they are detectors. Manual review is still required to add accurate descriptions and bug pattern mappings.
