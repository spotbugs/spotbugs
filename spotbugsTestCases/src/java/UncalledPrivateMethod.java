import edu.umd.cs.findbugs.annotations.ExpectWarning;

public class UncalledPrivateMethod {
    // Interesting tidbit:
    // Sun's javac makes class initializer methods "default static",
    // while jikes makes them "private final static", which could
    // lead to spurious warnings.
    private static final Object myObject = new Object();

    @ExpectWarning("UrF")
    String s;

    private void foo(String s) {
        this.s = s;
    }

    private void debug(String s) {
        System.out.println(s);
    }

    @ExpectWarning("UPM")
    private void foobar(int i) {
    }

    private void foobar(double d) {
    }

    public void f(double d) {
        foobar(d);
    }

}
