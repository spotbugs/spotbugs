package sfBugsNew;

import edu.umd.cs.findbugs.annotations.ExpectWarning;
import edu.umd.cs.findbugs.annotations.NoWarning;

public class Bug1322 {
    public void test() {}
    public void test(Inner arg) {}
    
    public static class Super {
        public void test() {}
        public void test(Inner arg) {}
    }
    
    public class Inner extends Super {
        @ExpectWarning("IA_AMBIGUOUS_INVOCATION_OF_INHERITED_OR_OUTER_METHOD")
        public void ok() {
            test();
        }

        @ExpectWarning("IA_AMBIGUOUS_INVOCATION_OF_INHERITED_OR_OUTER_METHOD")
        public void ok2() {
            Inner inner = new Inner();
            test(inner);
        }

        @NoWarning("IA_AMBIGUOUS_INVOCATION_OF_INHERITED_OR_OUTER_METHOD")
        public void fp() {
            Inner inner = new Inner();
            inner.test();
        }

        @NoWarning("IA_AMBIGUOUS_INVOCATION_OF_INHERITED_OR_OUTER_METHOD")
        public void fp2() {
            Inner inner = new Inner();
            inner.test(this);
        }
    }
}
