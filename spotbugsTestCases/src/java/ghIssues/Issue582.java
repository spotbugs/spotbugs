package ghIssues;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.errorprone.annotations.CheckReturnValue;
/**
 * @see <a href="https://github.com/spotbugs/spotbugs/issues/582">GitHub issue</a>
 */
@CheckReturnValue
public class Issue582 {
    private String value;

    String getValue() {
        return value;
    }

    @CanIgnoreReturnValue
    String returnAValue(String newValue) {
        value = newValue;
        return newValue;
    }

    String returnAnotherValue(String newValue) {
        value = newValue;
        return newValue;
    }

    public static String testNoError() {
        Issue582 i = new Issue582();
        i.returnAValue("foobar");
        return i.getValue();
    }

    public static String testWithError() {
        Issue582 i = new Issue582();
        i.returnAnotherValue("foobar");
        return i.getValue();
    }
}
