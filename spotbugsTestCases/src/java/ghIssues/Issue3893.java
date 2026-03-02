package ghIssues;

/**
 * Test cases for GitHub issue #3893:
 * ST_WRITE_TO_STATIC_FROM_INSTANCE_METHOD false negative in non-static inner class.
 *
 * When a named non-static inner class writes to a static field of its outer class,
 * the class name contains '$' (e.g., Issue3893$Inner). The old priority-bump logic
 * for any class with '$' in its name suppressed reporting, causing a false negative.
 */
public class Issue3893 {
    static int counter;

    // Case 1: outer (non-inner) class writes to static field — always correctly reported
    void setCounter(int value) {
        counter = value;
    }

    class Inner {
        // Case 2: named non-static inner class writes to outer static field directly
        void setCounter(int value) {
            counter = value;
        }

        // Case 3: named non-static inner class writes to outer static field via explicit class qualifier
        void resetCounter() {
            Issue3893.counter = 0;
        }
    }
}
