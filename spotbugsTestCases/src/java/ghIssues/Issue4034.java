package ghIssues;

import edu.umd.cs.findbugs.annotations.ExpectWarning;

/**
 * <a href="https://github.com/spotbugs/spotbugs/issues/4034">#4034</a>.
 */
public class Issue4034 {
    @ExpectWarning("UWF_NULL_FIELD")
    private String strVal = (String) null;

    public int compute(int input) {
        int result = 0;
        if (strVal != null) {
            result += strVal.length();
        }
        return result + input;
    }
}
