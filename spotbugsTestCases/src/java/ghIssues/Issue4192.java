package ghIssues;

/**
 * INT_BAD_COMPARISON_WITH_SIGNED_BYTE must only fire on comparisons of a signed
 * byte (range -128..127) that are always true or never true, not on meaningful
 * ones. See https://github.com/spotbugs/spotbugs/issues/4192
 */
public class Issue4192 {

    /** False positive fixed by #4192: true for -128..126, false at 127. */
    public boolean lessThan127(byte b) {
        return b < 127;
    }

    /** False positive fixed by #4192: true only at 127. */
    public boolean greaterOrEqual127(byte b) {
        return b >= 127;
    }

    /** Reported: always true (b is at most 127). */
    public boolean lessOrEqual127(byte b) {
        return b <= 127;
    }

    /** Reported: never true (b is at most 127). */
    public boolean greaterThan127(byte b) {
        return b > 127;
    }

    /** Reported: out-of-range unsigned confusion, never true. */
    public boolean equal255(byte b) {
        return b == 255;
    }

    /** Reported: out of range above the maximum, always true. */
    public boolean lessThan128(byte b) {
        return b < 128;
    }

    /** Reported: out of range below the minimum, always true. */
    public boolean greaterThanMinus129(byte b) {
        return b > -129;
    }

    /** Not reported: equality against the boundary is meaningful. */
    public boolean equal127(byte b) {
        return b == 127;
    }
}
