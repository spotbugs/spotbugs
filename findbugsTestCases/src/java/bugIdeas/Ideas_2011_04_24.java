package bugIdeas;

import junit.framework.TestCase;

public class Ideas_2011_04_24 extends TestCase {


    public double getDouble() {
        return 17.0;
    }
    public double getInt() {
        return 42;
    }
    public String getFoo() {
        return "Foo";
    }
    public void testBusted() {
        assertSame(17, getDouble() );
        assertEquals(17, getDouble() );
        assertEquals(42.0, getInt() );
        assertEquals(42, getFoo() );
        assertEquals(42.0, getFoo() );
    }

    public void testOK() {
        assertEquals(42.0, getInt() );

    }
}
