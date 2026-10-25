package sfBugs;

import org.jspecify.annotations.Nullable;

public class Bug1795536 {
    interface I {
        public void foo(@Nullable Object arg);
    }

    static abstract class A1 implements I {
    }

    /*
     * static class C2 extends A1 {
     *
     * @Override public void foo(Object arg) {
     * System.out.println(arg.toString()); } }
     */
}
