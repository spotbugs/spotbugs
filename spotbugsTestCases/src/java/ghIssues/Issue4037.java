package ghIssues;

/**
 * {@code NP_BOOLEAN_RETURN_NULL} must be reported whenever a {@code Boolean}-typed
 * method returns {@code null}, regardless of whether the {@code null} is written
 * directly ({@code return null;}) or first stored in a local variable
 * ({@code Boolean b = null; return b;}). The two forms compile to different bytecode
 * ({@code ACONST_NULL; ARETURN} vs. {@code ACONST_NULL; ASTORE n; ALOAD n; ARETURN})
 * but have identical semantics. See
 * <a href="https://github.com/spotbugs/spotbugs/issues/4037">issue 4037</a>.
 */
public class Issue4037 {

    /** Direct form: already reported before the fix (control case). */
    public Boolean isValidDirect(String input) {
        if (input == null) {
            return null;
        }
        return Boolean.TRUE;
    }

    /** Indirect form via a local variable: the case the fix targets. */
    public Boolean isValidIndirect(String input) {
        if (input == null) {
            Boolean result = null;
            return result;
        }
        return Boolean.TRUE;
    }

    /** Indirect form inside a richer method body. */
    public Boolean checkStatus(int code) {
        if (code < 0) {
            Boolean result = null;
            return result;
        }
        return code == 0 ? Boolean.TRUE : Boolean.FALSE;
    }

    /** Control: returning a non-null {@code Boolean} must NOT trigger the bug. */
    public Boolean alwaysNonNull(int code) {
        return code == 0 ? Boolean.TRUE : Boolean.FALSE;
    }
}
