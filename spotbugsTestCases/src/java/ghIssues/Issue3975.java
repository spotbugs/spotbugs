package ghIssues;

/**
 * Regression for <a href="https://github.com/spotbugs/spotbugs/issues/3975">#3975</a>.
 */
public class Issue3975 {

    static class IntFieldDoubleAssignment {
        int value = 3;
        boolean b = 1 > (value = 2);
    }

    static class LongFieldDoubleAssignment {
        long value = 3L;
        boolean b = 1 > (value = 2L);
    }

    static class DoubleFieldDoubleAssignment {
        double value = 3.0;
        boolean b = 1.0 > (value = 2.0);
    }
}
