package ghIssues;

/**
 * Test cases for GitHub issue #3963:
 * IL_INFINITE_LOOP false positive when loop condition uses int-to-double/float/long
 * comparison that is statically known to be false (loop exits immediately).
 */
public class Issue3963 {

    // False positive (now fixed): foo.length()=1 > 2.0 is false, loop exits immediately
    public void testDoubleComparisonFP() {
        String foo = "a";
        while (foo.length() > 2.0) {
        }
    }

    // True positive: foo.length()=3 > 2.0 is true, loop is infinite
    public void testDoubleComparisonTP() {
        String foo = "abc";
        while (foo.length() > 2.0) {
        }
    }

    // False positive (now fixed): foo.length()=1 > 2.0f is false, loop exits immediately
    public void testFloatComparisonFP() {
        String foo = "a";
        while (foo.length() > 2.0f) {
        }
    }

    // True positive: foo.length()=3 > 2.0f is true, loop is infinite
    public void testFloatComparisonTP() {
        String foo = "abc";
        while (foo.length() > 2.0f) {
        }
    }

    // False positive (now fixed): foo.length()=1 > 2L is false, loop exits immediately
    public void testLongComparisonFP() {
        String foo = "a";
        while (foo.length() > 2L) {
        }
    }

    // True positive: foo.length()=3 > 2L is true, loop is infinite
    public void testLongComparisonTP() {
        String foo = "abc";
        while (foo.length() > 2L) {
        }
    }
}
