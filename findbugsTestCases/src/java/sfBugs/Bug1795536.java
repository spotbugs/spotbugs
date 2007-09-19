package sfBugs;

import edu.umd.cs.findbugs.annotations.CheckForNull;

public class Bug1795536 {
    interface I {
        public void foo(@CheckForNull Object arg);
    }

    static abstract class A1 implements I {
    }

    /*
    static class C2 extends A1 {
        @Override
        public void foo(Object arg) {
            System.out.println(arg.toString());
        }
    }
        */
}
