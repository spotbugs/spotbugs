package ghIssues;

/**
 * Test case for GitHub issue #4034:
 * UWF_NULL_FIELD should be reported for a field initialized with `cast null`.
 * @see <a href="https://github.com/spotbugs/spotbugs/issues/4034">GitHub issue</a>
 */
public class Issue4034 {

    // Field initialized with a cast null value.
    private String strVal = (String) null;

    public String getStrVal() {
        return strVal;
    }
}
