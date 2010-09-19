package sfBugs;

import edu.umd.cs.findbugs.annotations.NoWarning;

public class Bug3047257 {
    int x;

    public Bug3047257(int x) {
        this.x = x;
    }

    @Override
    public int hashCode() {
        return x;

    }

    @NoWarning("BC_EQUALS_METHOD_SHOULD_WORK_FOR_ALL_OBJECTS")
    @Override
    public boolean equals(Object o) {
        if (!getClass().isInstance(o))
            return false;
        Bug3047257 i = (Bug3047257) o;
        return this.x == i.x;
    }

}
