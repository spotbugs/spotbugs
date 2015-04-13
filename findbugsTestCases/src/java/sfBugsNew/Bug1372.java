package sfBugsNew;

import edu.umd.cs.findbugs.annotations.NoWarning;

public class Bug1372 {

    @NoWarning("NP_LOAD_OF_KNOWN_NULL_VALUE")
    public void test(final boolean test) {
        String s = null;
        try {
            if (test) {
                return;
            }
            throw new IllegalStateException("Illegal Exception");
        } catch (final Exception e) {
            s = "test";
            throw new RuntimeException("Runtime Exception", e);
        } finally {
            method(s);
        }
    }

    private void method(final String method) {
    }

}
