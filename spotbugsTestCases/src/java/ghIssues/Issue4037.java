package ghIssues;

import edu.umd.cs.findbugs.annotations.ExpectWarning;

/**
 * <a href="https://github.com/spotbugs/spotbugs/issues/4037">#4037</a>.
 */
public class Issue4037 {

    @ExpectWarning("NP_BOOLEAN_RETURN_NULL")
    public Boolean isValidDirect(String input) {
        if (input == null) {
            return null;
        }
        return Boolean.TRUE;
    }

    @ExpectWarning("NP_BOOLEAN_RETURN_NULL")
    public Boolean isValidIndirect(String input) {
        if (input == null) {
            Boolean result = null;
            return result;
        }
        return Boolean.TRUE;
    }

    @ExpectWarning("NP_BOOLEAN_RETURN_NULL")
    public Boolean checkStatus(int code) {
        if (code < 0) {
            Boolean result = null;
            return result;
        }
        return code == 0 ? Boolean.TRUE : Boolean.FALSE;
    }
}
