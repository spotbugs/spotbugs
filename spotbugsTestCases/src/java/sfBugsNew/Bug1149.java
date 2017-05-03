package sfBugsNew;

import edu.umd.cs.findbugs.annotations.DesireNoWarning;
import edu.umd.cs.findbugs.annotations.ExpectWarning;
import edu.umd.cs.findbugs.annotations.NoWarning;

public class Bug1149 {

    static class Foo {
        public void setEnabled(boolean b) {
        }
    }

    static class Bar {
        public void setEnabled(boolean b) {
        }

        class Nested extends Foo {
            @DesireNoWarning("IA_AMBIGUOUS_INVOCATION_OF_INHERITED_OR_OUTER_METHOD")
            public Nested() {
                this.setEnabled(true);
            }
            @ExpectWarning("IA_AMBIGUOUS_INVOCATION_OF_INHERITED_OR_OUTER_METHOD")
            public Nested(boolean b) {
                setEnabled(b);
            }
            @NoWarning("IA_AMBIGUOUS_INVOCATION_OF_INHERITED_OR_OUTER_METHOD")
            public Nested(int x) {
                super.setEnabled(x > 0);
            }
        }
    }

}
