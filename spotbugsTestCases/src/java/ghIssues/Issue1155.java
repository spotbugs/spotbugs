package ghIssues;

/**
 * False positive on exception path from a static method.
 * Reported by Niels Basjes ( https://github.com/nielsbasjes ).
 **/

public final class Issue1155 {
    public static void require(boolean condition, String error) {
        if (!condition) {
            throw new IllegalStateException(error);
        }
    }

    /*
     * This static method call will throw an exception if 'found' is null.
     * The 'require' method is a static method and cannot be overridden to change the behavior.
     * So if 'found' is null the return statement is never reached.
     * So the reported NP_NULL_ON_SOME_PATH on the dereference of 'found' in the return statement is incorrect.
     */
    public int demoOfInvalidReport(String input, String findValue) {
        String found = null;
        if (input.contains(findValue)) {
            found = input;
        }

        require(found != null, "Should have found it");
        return found.length();
    }

    /*
     * The exact same code as above but now the static method has been inlined.
     * This does not give an error report (which is correct).
     */
    public int demoOfCorrectReport(String input, String findValue) {
        String found = null;
        if (input.contains(findValue)) {
            found = input;
        }

        if (found == null) {
            throw new IllegalStateException("Should have found it");
        }

        return found.length();
    }

}
