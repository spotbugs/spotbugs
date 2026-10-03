package ghIssues;

/**
 * Test case for GitHub issue #4309:
 * MC_OVERRIDABLE_METHOD_CALL_IN_CONSTRUCTOR should be reported when a constructor
 * calls an overridable method that takes arguments (e.g. a setter).
 * @see <a href="https://github.com/spotbugs/spotbugs/issues/4309">GitHub issue</a>
 */
class Issue4309 {
    int value;

    Issue4309(int value) {
        setValue(value);
    }

    void setValue(int value) {
        this.value = value;
    }
}
