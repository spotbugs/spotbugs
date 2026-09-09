package sfBugs;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class Bug2707502 {

    @Test
    public int foo() {
        Object x = null;
        Assertions.assertThrows(NullPointerException.class, () -> {
            x.hashCode();
        });
        return -1;
    }

}
