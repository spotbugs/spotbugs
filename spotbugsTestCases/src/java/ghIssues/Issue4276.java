package ghIssues;

public class Issue4276 {
    static String value;

    public String test(StringBuilder builder) {
        if (value == null) {
            value = builder.toString();
        }
        return value;
    }
}
