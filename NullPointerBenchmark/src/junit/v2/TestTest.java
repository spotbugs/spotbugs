package v2;

import junit.framework.TestCase;

/**
 * This class just provides runtest test cases to verify that the true cases can
 * throw null pointer exceptions and that the false positive cases do not.
 * 
 * This code <em>must</em> be included in any static analysis.
 * 
 */
public class TestTest extends TestCase {

    Test test;

    public void setUp() {
        test = new Test();
    }

    public void testTP1() {
        try {
            for (int i = -1; i <= 5; i++)
                test.tp1(i);
        } catch (NullPointerException e) {
            return;
        }
        fail();
    }

    public void testFP1() {
        for (int i = -1; i <= 5; i++)
            test.fp1(i);
    }

    public void testTP2() {
        try {
            test.tp2(true);
            test.tp2(false);
        } catch (NullPointerException e) {
            return;
        }
        fail();
    }

    public void testFP2() {
        test.fp2(true);
        test.fp2(false);
    }

    public void testTP3() {
        try {
            test.tp3(null);
            test.tp3("x");
        } catch (NullPointerException e) {
            return;
        }
        fail();
    }

    public void testTF3() {
        test.fp3(null);
        test.fp3("x");
    }

    public void testTP4() {
        try {
            test.tp4(true);
            test.tp4(false);
        } catch (NullPointerException e) {
            return;
        }
        fail();
    }

    public void testFP4() {
        test.fp4(true);
        test.fp4(false);
    }

    public void testTP5() {
        try {
            test.tp5(null);
            test.tp5("x");
        } catch (NullPointerException e) {
            return;
        }
        fail();
    }

    public void testTP6() {
        try {
            test.tp6(null);
            test.tp6("x");
        } catch (NullPointerException e) {
            return;
        }
        fail();
    }

    public void testIFP1() {
        test.ifp1(false);
        test.ifp1(true);

    }

    public void testITP1() {
        try {
            test.itp1(false);
            test.itp1(true);
        } catch (NullPointerException e) {
            return;
        }
        fail();
    }

    public void testITP2() {
        try {
            test.itp2();
        } catch (NullPointerException e) {
            return;
        }
        fail();
    }

    public void testITP3() {
        try {
            test.itp3(true);
            test.itp3(false);
        } catch (NullPointerException e) {
            return;
        }
        fail();
    }

    public static void main(String args[]) {

        {
            TestTest tt = new TestTest();
        tt.setUp();
        tt.testTP1();
        tt.testTP2();
        tt.testTP3();
        tt.testTP4();
        tt.testTP5();
        tt.testTP6();

        tt.testFP1();
        tt.testFP2();
        tt.testFP4();

        tt.testITP1();
        tt.testITP2();
        tt.testITP3();

        tt.testIFP1();
        }
        {
            TestTestFields tt = new TestTestFields();
            tt.setUp();

            tt.testTP1();
            tt.testTP2();
            tt.testTP3();
            tt.testTP4();
            tt.testTP5();
            tt.testTP6();

            tt.testFP1();
            tt.testFP2();
            tt.testFP4();

            tt.testITP1();
            tt.testITP2();
            tt.testITP3();

            tt.testIFP1();
        }
    }

}
