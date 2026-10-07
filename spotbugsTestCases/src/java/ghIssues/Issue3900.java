package ghIssues;

import java.util.Arrays;

/**
 * Ignoring the result of {@code Arrays.copyOf} or {@code Arrays.copyOfRange} is a bug: the call has no side effect and
 * the copy is lost. The same holds for any pure static method or constructor of a class using {@code assert}: javac then
 * generates a static initializer calling {@code Class.desiredAssertionStatus()}, which used to be taken for a side effect
 * excluding the whole class from the detector ({@code java.util.Arrays} is such a class).
 *
 * @see <a href="https://github.com/spotbugs/spotbugs/issues/3900">GitHub issue #3900</a>
 */
public class Issue3900 {

    public void ignoredCopyOf(int[] values) {
        Arrays.copyOf(values, 10);
    }

    public void ignoredCopyOfObjects(String[] values) {
        Arrays.copyOf(values, 10);
    }

    public void ignoredCopyOfRange(int[] values) {
        Arrays.copyOfRange(values, 1, 3);
    }

    public void ignoredCopyOfRangeObjects(String[] values) {
        Arrays.copyOfRange(values, 1, 3);
    }

    public void ignoredPureStaticOfClassUsingAssert(int[] values) {
        Issue3900UsingAssert.firstHalf(values);
    }

    public void ignoredConstructorOfClassUsingAssert() {
        new Issue3900UsingAssert();
    }

    public int[] usedCopyOf(int[] values) {
        return Arrays.copyOf(values, 10);
    }

    public int[] usedCopyOfRange(int[] values) {
        return Arrays.copyOfRange(values, 1, 3);
    }

    public int[] usedPureStaticOfClassUsingAssert(int[] values) {
        return Issue3900UsingAssert.firstHalf(values);
    }
}

/**
 * Uses {@code assert}, so javac generates a static initializer reading {@code Class.desiredAssertionStatus()}.
 */
class Issue3900UsingAssert {

    static int[] firstHalf(int[] values) {
        return Arrays.copyOf(values, values.length / 2);
    }

    void requireNonEmpty(int[] values) {
        assert values.length > 0;
    }
}
