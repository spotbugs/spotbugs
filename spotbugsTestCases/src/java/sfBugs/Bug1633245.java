package sfBugs;

import org.jspecify.annotations.Nullable;

public class Bug1633245 {
    interface Foo {
        int f(@Nullable Object x);
    }

    static class FooImpl implements Foo {
        @Override
        public int f(Object x) {
            return x.hashCode();
        }
    }

}
