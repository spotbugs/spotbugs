package ghIssues;

/**
 * Reproducer for issue #4147: {@code NP_NULL_ON_SOME_PATH} verdict depends on
 * the operand order of a reference null check. {@code x == null} and
 * {@code null == x} are the same condition, so both forms must reach the same
 * verdict on the else branch that dereferences {@code x}.
 *
 * <p>Both methods guard a dereference of {@code x} with a null check that
 * throws on the null branch and dereferences {@code x} on the non-null branch.
 * Neither method should report {@code NP_NULL_ON_SOME_PATH}.</p>
 */
public class Issue4147 {

    Object x;

    /** Direct form {@code x == null} — must not report {@code NP_NULL_ON_SOME_PATH}. */
    int directForm() {
        if (x == null) {
            System.out.println("x is null");
        }
        if (x == null) {
            throw new NullPointerException();
        } else {
            return x.hashCode();
        }
    }

    /** Yoda form {@code null == x} — must not report {@code NP_NULL_ON_SOME_PATH}. */
    int yodaForm() {
        if (null == x) {
            System.out.println("x is null");
        }
        if (null == x) {
            throw new NullPointerException();
        } else {
            return x.hashCode();
        }
    }
}
