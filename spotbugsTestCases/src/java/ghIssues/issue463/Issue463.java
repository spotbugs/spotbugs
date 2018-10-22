package ghIssues.issue463;

import com.google.errorprone.annotations.CanIgnoreReturnValue;

/**
 * @see <a href="https://github.com/spotbugs/spotbugs/issues/463">GitHub issue</a>
 */
public class Issue463 {
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
        Issue463 i = new Issue463();
        i.returnAValue("foobar");
        return i.getValue();
    }

    public static String testWithError() {
        Issue463 i = new Issue463();
        i.returnAnotherValue("foobar");
        return i.getValue();
    }
}
