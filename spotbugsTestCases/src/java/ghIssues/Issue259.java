package ghIssues;

public class Issue259 {
    public void falsePositive() {
        try (X x = new X(); X y = new X()) {
            throw new IllegalStateException();
        } // Redundant nullcheck of x, which is known to be non-null in test.Bug1169.falsePositive()
    }

    private static class X implements AutoCloseable {
        @Override
        public void close() {
        }
    }
}
