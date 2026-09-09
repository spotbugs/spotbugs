package ghIssues;

/**
 * Test cases for GitHub issue #3904:
 * IL_INFINITE_LOOP false positive when an inner loop condition is always false.
 */
public class Issue3904 {

    // Inner do-while condition is always false (j < 1 when j == 1) -> exits immediately.
    // Only the outer do-while (i < 5 when i == 1) is truly infinite.
    // SpotBugs should report the outer loop but NOT the inner loop.
    public void innerLoopAlwaysFalse() {
        int i = 1;
        do {
            int j = 1;
            do {
            } while (j < 1); // NOT an infinite loop - condition is always false
        } while (i < 5); // IS an infinite loop - condition is always true
    }

    // Inner do-while condition is always true (j <= 1 when j == 1) -> truly infinite.
    // Both loops should be reported.
    public void innerLoopAlwaysTrue() {
        int i = 1;
        do {
            int j = 1;
            do {
            } while (j <= 1); // IS an infinite loop - condition is always true
        } while (i < 5); // IS an infinite loop - condition is always true
    }

    // Single do-while with always-false condition -> exits after one iteration, not infinite.
    public void singleLoopAlwaysFalse() {
        int i = 1;
        do {
        } while (i < 1); // NOT an infinite loop - condition is always false
    }

    // Standard infinite do-while (constant condition always true).
    public void singleLoopAlwaysTrue() {
        int i = 1;
        do {
        } while (i < 5); // IS an infinite loop
    }
}
