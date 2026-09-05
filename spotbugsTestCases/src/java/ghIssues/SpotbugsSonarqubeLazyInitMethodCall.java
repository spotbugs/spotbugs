package ghIssues;

import edu.umd.cs.findbugs.annotations.ExpectWarning;

public class SpotbugsSonarqubeLazyInitMethodCall {
    static String value;

    @ExpectWarning("LI_LAZY_INIT_STATIC")
    String test(StringBuilder builder) {
        if (value == null) value = builder.toString();
        return value;
    }
}
