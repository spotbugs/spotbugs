package ghIssues;

import java.util.Arrays;

import edu.umd.cs.findbugs.annotations.ExpectWarning;

/**
 * <a href="https://github.com/spotbugs/spotbugs/issues/3900">#3900</a>.
 */
public class Issue3900 {
    @ExpectWarning("RV_RETURN_VALUE_IGNORED_NO_SIDE_EFFECT")
    public void ignoredCopyOf() {
        int[] arr = {1, 2, 3};
        Arrays.copyOf(arr, 10);
    }
}
