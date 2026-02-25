package ghIssues;

public class Issue3916 {

    /**
     * Classic null check via != null - NP_LOAD_OF_KNOWN_NULL_VALUE should be reported.
     */
    public static void foo1(String s1) {
        if (s1 != null) {
            return;
        }
        String str = s1.toString(); // NP warning expected
    }

    /**
     * Null check via instanceof - NP_LOAD_OF_KNOWN_NULL_VALUE should also be reported.
     * When s2 instanceof String is false and the static type of s2 is String,
     * s2 must be null on the fall-through path.
     */
    public static void foo2(String s2) {
        if (s2 instanceof String) {
            return;
        }
        String str = s2.toString(); // NP warning expected
    }
}
