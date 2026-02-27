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

    /**
     * Negated instanceof: !(s3 instanceof String).
     * The body is only reached when s3 is NOT an instance of String.
     * Since String is a subtype of String, this implies s3 is null.
     * Should report NP_LOAD_OF_KNOWN_NULL_VALUE.
     */
    public static void foo3(String s3) {
        if (!(s3 instanceof String)) {
            String str = s3.toString(); // NP warning expected
        }
    }

    /**
     * instanceof checks a subtype of the variable's type (String is a subtype of Object).
     * "not instanceof" does NOT imply null: s4 could be any non-String Object.
     * Should NOT report NP_LOAD_OF_KNOWN_NULL_VALUE.
     */
    public static void foo4(Object s4) {
        if (s4 instanceof String) {
            return;
        }
        s4.toString(); // no NP warning expected
    }
}
