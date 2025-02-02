package sfBugsNew;

import edu.umd.cs.findbugs.annotations.ExpectWarning;
import edu.umd.cs.findbugs.annotations.NoWarning;

public class Feature301 {
    static abstract class Parent {}
    static class Child extends Parent {}
    static class GrantChild extends Child {}

    protected Number[] field = null;
    protected Number[] field2 = null;
    protected Parent[] parents = null;

    @ExpectWarning("CAA_COVARIANT_ARRAY_LOCAL")
    public void caaLocal() {
        Number[] numbers = new Integer[10];
        numbers[0] = 1;
    }

    @ExpectWarning("CAA_COVARIANT_ARRAY_FIELD")
    public void caaField() {
        field = new Integer[10];
        field2 = new Double[10];
    }

    @NoWarning("CAA_COVARIANT_ARRAY_FIELD")
    public void caaField2() {
        field2 = new Number[10];
    }

    @ExpectWarning("CAA_COVARIANT_ARRAY_FIELD")
    public void caaParent() {
        parents = new Child[10];
    }

    @ExpectWarning("CAA_COVARIANT_ARRAY_RETURN")
    public Number[] caaReturn() {
        return new Integer[10];
    }

    @ExpectWarning("CAA_COVARIANT_ARRAY_ELEMENT_STORE")
    public void caaStore() {
        field[0] = 1.0;
    }

    @NoWarning("CAA_COVARIANT_ARRAY_ELEMENT_STORE")
    public void caaStoreOk() {
        field2[0] = 1;
    }

    // parent is Child[] array, but every non-abstract subclass of Parent is also subclass of Child
    // so no ArrayStoreException will occur in current project and we don't report this method
    // it's enough that caaParent() was reported with low priority
    @NoWarning("CAA_COVARIANT_ARRAY_ELEMENT_STORE")
    public void caaStoreNoReport(Parent p) {
        parents[0] = p;
    }
}
