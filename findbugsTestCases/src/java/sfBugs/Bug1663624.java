package sfBugs;

import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;

@DefaultAnnotation(NonNull.class)
public class Bug1663624 {

    private interface SomeInterface {

        public boolean condition1();

        public boolean condition2();

        public Object get();

    }

    public void show(SomeInterface reference) {
        Object object = reference.condition1() ? reference.get() : null;
        if (reference.condition2()) {
            foo(object); // (*) object may bu null!
        }
    }

    private void foo(Object caretSelection) {
    }

}
