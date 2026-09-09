package sfBugsNew;

import edu.umd.cs.findbugs.annotations.NoWarning;

public class Bug1291 {
    Exception exception;

    public void test() throws ArrayIndexOutOfBoundsException, UnsupportedOperationException {
    }

    protected void a() {
        try {
            test();
        } catch (ArrayIndexOutOfBoundsException | UnsupportedOperationException ex) {
            exception = ex;
        }
    }

    @NoWarning("BC_VACUOUS_INSTANCEOF")
    protected void b(int state) {
        if (exception instanceof ArrayIndexOutOfBoundsException) {
            return;
        }
    }

    @NoWarning("BC_IMPOSSIBLE_INSTANCEOF")
    protected void c(int state) {
        if (exception instanceof UnsupportedOperationException) {
            return;
        }
    }

}
