package ghIssues;

public class Issue3966 {

    /**
     * True positive: s.toString() is redundant when s is already a String.
     * Both toString() calls should be reported.
     */
    public String chainedToStringTP(String s) {
        // s.toUpperCase().toString() - toString() is redundant
        // s.toString().toLowerCase() - toString() is redundant
        return s.toUpperCase().toString() + s.toString().toLowerCase();
    }

    /**
     * True positive: single toString() call on a String.
     */
    public String singleToStringTP(String s) {
        return s.toString();
    }

    /**
     * True positive: toString() in middle of chain.
     */
    public String toStringMiddleTP(String s) {
        return s.toString().toLowerCase();
    }
}
