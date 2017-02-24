package sfBugs;

import junit.framework.TestCase;

public class Bug1647660 extends TestCase {

    public static class Test1 extends Bug1647660 {

        @Override
        Number getNumber(int i) {
            return Integer.valueOf(i);
        }

    }

    public static class Test2 extends Bug1647660 {

        @Override
        Number getNumber(int i) {
            return Long.valueOf(i);
        }

    }

    Number getNumber(int i) {
        return Short.valueOf((short) i);
    }

    public void test1() {
        Number n = getNumber(1);
        assertEquals(1, n.byteValue());
    }

    public void test2() {
        Number n = getNumber(2);
        assertEquals(2, n.byteValue());
    }
}
