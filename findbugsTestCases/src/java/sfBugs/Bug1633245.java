package sfBugs;

import javax.annotation.CheckForNull;

// import edu.umd.cs.findbugs.annotations.CheckForNull;

public class Bug1633245 {
    interface Foo {
        int f(@CheckForNull Object x);
    }

    static class FooImpl implements Foo {
        @Override
        public int f(Object x) {
            return x.hashCode();
        }
    }

}
