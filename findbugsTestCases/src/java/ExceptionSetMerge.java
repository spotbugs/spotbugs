import java.io.IOException;
import java.io.InputStream;

// This causes a fatal exception in FindBugs 0.8.1
// Array types can be merged with exception objects

public class ExceptionSetMerge {
    public void f(InputStream in) {
        Object o = new Object[0];

        try {
            System.out.println(in.read());
        } catch (IOException e) {
            o = e;
        }

        System.out.println(o.hashCode());
    }
}

// vim:ts=4
