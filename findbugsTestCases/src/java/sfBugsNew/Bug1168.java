package sfBugsNew;

import javax.annotation.Nonnull;

import edu.umd.cs.findbugs.annotations.DesireNoWarning;

public class Bug1168 {

    @Nonnull
    private Integer foo;

    @DesireNoWarning("NP_NONNULL_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR")
    public Bug1168() {  // Nonnull field foo not initialized
        assignFoo();
        if (foo == null) {
            throw new IllegalStateException();
        }
    }

    private void assignFoo() {
        foo = 2;
    }
}
