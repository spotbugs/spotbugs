package ghIssues;

/**
 * Companion of {@link Issue3483}: an untouched class that must not inherit the
 * {@code inAssert} state left over by {@link Issue3483Poison}.
 * See https://github.com/spotbugs/spotbugs/issues/3483
 */
public class Issue3483Victim {

    private static int field;

    public static void victim() {
        field = 1;
    }

    public static int getField() {
        return field;
    }
}
