package ghIssues;

/**
 * False positive on exception path from a static method.
 * Reported by Niels Basjes ( https://github.com/nielsbasjes ).
 **/

public final class Issue1155 {
    // ================================================================================
    // The problem situation
    public static void check(boolean condition) {
        if (condition) {
            throw new IllegalStateException("Should have found it");
        }
    }

    /*
     * This static method call will throw an exception if 'found' is null.
     * The 'require' method is a static method and cannot be overridden to change the behavior.
     * So if 'found' is null the return statement is never reached.
     * So the reported NP_NULL_ON_SOME_PATH on the dereference of 'found' in the return statement is incorrect.
     */
    public int demoOfInvalidReport(boolean use, String input) {
        String found = null;
        if (use) {
            found = input;
        }

        check(found == null);
        return found.length();
    }

    // ================================================================================

    /*
     * The exact same code as above but now the static method has been inlined.
     * This does not give an error report (which is correct).
     */
    public int demoOfCorrectNoReport(boolean use, String input) {
        String found = null;
        if (use) {
            found = input;
        }

        if (found == null) {
            throw new IllegalStateException("Should have found it");
        }

        return found.length();
    }

    // ================================================================================
    // The problem situation with only the 'check' method is now called 'failIf'
    // THE NAME OF THE STATIC METHOD ELIMINATES THE PROBLEM ?!?!?!

    public static void failIf(boolean condition) {
        if (condition) {
            throw new IllegalStateException("Should have found it");
        }
    }

    /*
     * This static method call will throw an exception if 'found' is null.
     * The 'require' method is a static method and cannot be overridden to change the behavior.
     * So if 'found' is null the return statement is never reached.
     * So the reported NP_NULL_ON_SOME_PATH on the dereference of 'found' in the return statement is incorrect.
     */
    public int demoOfCorrectNoReportDifferentMethodName(boolean use, String input) {
        String found = null;
        if (use) {
            found = input;
        }

        failIf(found == null);
        return found.length();
    }

}
