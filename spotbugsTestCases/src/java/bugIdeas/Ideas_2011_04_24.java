package bugIdeas;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class Ideas_2011_04_24 {


    public double getDouble() {
        return 17.0;
    }
    public int getInt() {
        return 42;
    }
    public String getFoo() {
        return "Foo";
    }

    public boolean b(String s) {
        return s.length() == 0 || s == null;
    }

    @Test
    public void testBusted() {
        Assertions.assertSame(17, getDouble() );
        Assertions.assertEquals(42.0, getDouble() );
        Assertions.assertEquals(17, getDouble() );
        Assertions.assertEquals(42.0, getInt() );
        Assertions.assertEquals(42, getFoo() );
        Assertions.assertEquals(42.0, getFoo() );
    }
}
