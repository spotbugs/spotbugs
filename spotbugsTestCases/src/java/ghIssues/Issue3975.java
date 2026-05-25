package ghIssues;

public class Issue3975 {

    static class IntDoubleAssignment {
        int value = 3;
        boolean b = 1 > (value = 2);
    }

    static class LongDoubleAssignment {
        long value = 3L;
        boolean b = 1 > (value = 2L);
    }

    static class DoubleDoubleAssignment {
        double value = 3.0;
        boolean b = 1 > (value = 2.0);
    }
}
