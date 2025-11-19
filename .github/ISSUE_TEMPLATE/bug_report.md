---
name: Bug report
about: Create a report to help us improve
title: ''
labels: ''
assignees: ''

---

**Before opening an issue**

* Search existing issues and pull requests to see if the issue was already discussed (both open and closed).
* Check our discussions to see if the issue was already discussed.
* Check for specific project we support to raise the issue on, under [spotbugs](https://github.com/spotbugs)
* Do not open IntelliJ plugin issues here, open them at [intellij-plugin](https://github.com/JetBrains/spotbugs-intellij-plugin)

**Describe the bug**
A clear and concise description of what the bug is. Is it a false positive bug hit (SpotBugs reports a bug, but the code is correct), or false negative (SpotBugs should find the bug in a faulty code)?

**To Reproduce**
Please provide a code example, if applicable. Prefarably [a minimal, verifiable code example](https://stackoverflow.com/help/minimal-reproducible-example).

```java
public class Bug {
  public static void foo() {
    // code example
  }
}
```

**Expected behavior**
A clear and concise description of what you expected to happen.

**Environment**
 - Java Compiler: [e.g. Eclipse Temurin]
 - Java version: [e.g. JDK 21]
 - SpotBugs Version: [e.g. 4.9.8]
 - How do you use SpotBugs: [e.g. via Gradle Plugin, Maven Plugin, Ant Plugin, SonarQube Plugin, Eclipse IDE Plugin, CLI]
    - Plugin version: [e.g. SpotBugs Gradle Plugin 6.4.5]
    - Environment version for Plugin: [e.g. Gradle 9.2.1]

**Additional context**
Add any other context about the problem here.
