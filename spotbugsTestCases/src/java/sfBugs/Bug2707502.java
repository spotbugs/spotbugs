package sfBugs;

import org.junit.Test;

class Bug2707502 {

    @Test(expected = NullPointerException.class)
    public int foo() {
        Object x = null;
        return x.hashCode();
    }

}
