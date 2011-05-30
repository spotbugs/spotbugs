package bugIdeas;

import junit.framework.TestCase;

public class Ideas_2011_04_24 extends TestCase {


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
    public void testBusted() {
        assertSame(17, getDouble() );
        assertEquals(42.0, getDouble() );
        assertEquals(17, getDouble() );
        assertEquals(42.0, getInt() );
        assertEquals(42, getFoo() );
        assertEquals(42.0, getFoo() );
    }
}
