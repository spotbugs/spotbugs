package ghIssues;

import java.util.ArrayList;
import java.util.List;

/**
 * Reproducer for https://github.com/spotbugs/spotbugs/issues/3483
 *
 * <p>{@code AbstractAssertDetector} sets {@code inAssert} when it sees a read of
 * {@code $assertionsDisabled} and only clears it when it sees {@code new AssertionError}.
 * A method that reads the flag without ever throwing leaves the detector "inside an
 * assertion" for every method analysed afterwards.</p>
 *
 * <p>NOTE: this class declares {@code $assertionsDisabled} by hand, so it must not contain
 * any {@code assert} statement - javac would then emit a synthetic field with the same
 * name and the compilation would fail.</p>
 */
public class Issue3483 {

    public static boolean $assertionsDisabled;

    private static int field;

    private static final List<String> list = new ArrayList<>();

    private final Object first;
    private final Object second;

    /** Reads {@code $assertionsDisabled} but never creates an {@code AssertionError}. */
    public static void poison() {
        if ($assertionsDisabled) {
            return;
        }
    }

    /** No assertion in sight: the PUTSTATIC must not be reported. */
    public static void victim() {
        field = 42;
    }

    /** Same leak for the ..._METHOD variant named in the issue title. */
    public static void victimMethod() {
        list.remove(null);
    }

    /** Same leak inside a constructor: two PUTFIELDs. */
    public Issue3483(Object first, Object second) {
        this.first = first;
        this.second = second;
    }

    public static int getField() {
        return field;
    }

    public Object getFirst() {
        return first;
    }

    public Object getSecond() {
        return second;
    }
}
