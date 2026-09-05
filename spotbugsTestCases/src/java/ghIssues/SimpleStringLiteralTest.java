package ghIssues;

import edu.umd.cs.findbugs.annotations.NoWarning;

public class SimpleStringLiteralTest {
    static String value;

    @NoWarning("LI_LAZY_INIT_STATIC")
    String test() {
        if (value == null) value = "hello";
        return value;
    }
}
