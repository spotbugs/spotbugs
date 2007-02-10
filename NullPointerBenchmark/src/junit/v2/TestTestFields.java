package v2;

import junit.framework.TestCase;

/**
 * This class just provides runtest test cases to verify that the true cases can throw null pointer exceptions and
 * that the false positive cases do not.
 * 
 * This code <em>must</em> be included in any static analysis.
 *
 */
public class TestTestFields extends TestCase {
    
    TestFields test;

    public void setUp() {
        test = new TestFields(null);
    }
    public void testTP1() {
        try {
        for(int i = -1; i <= 5; i++)
            test.tp1(i);
        } catch (NullPointerException e) { return; }
        fail();
    }
    public void testFP1() {
        for(int i = -1; i <= 5; i++)
            test.fp1(i);
    }
    public void testTP2() {
        try {     test.tp2(true);
        test.tp2(false);
    } catch (NullPointerException e) { return; }
    fail();
    }
    public void testFP2() {
        test.fp2(true);
        test.fp2(false);
    }
    public void testTP3() {
        try {   test.x = null;
        test.tp3();
        test.x = "x";
        test.tp3();
    } catch (NullPointerException e) { return; }
    fail();
    }
    public void testTF3() {
        test.x = null;
        test.fp3();
        test.x = "x";
        test.fp3();
    }
    public void testTP4() {
        try {    test.tp4(true);
        test.tp4(false);
    } catch (NullPointerException e) { return; }
    fail();
    }
    public void testFP4() {
        test.fp4(true);
        test.fp4(false);
    }
    public void testTP5() {
        try {    test.x = null;
        test.tp5();
        test.x = "x";
        test.tp5();
    } catch (NullPointerException e) { return; }
    fail();
    }
    public void testTP6() {
        try {    test.x = null;
        test.tp6();
        test.x = "x";
        test.tp6();
    } catch (NullPointerException e) { return; }
    fail();
    }
    public void testIFP1() {
        test.ifp1(false);
        test.ifp1(true);      
    }
    public void testITP1() {
        try {    test.itp1(false);
        test.itp1(true);
    } catch (NullPointerException e) { return; }
    fail();
    }
    
    public void testITP2() {
        try {   test.itp2();
    } catch (NullPointerException e) { return; }
    fail();
    }
    
    public void testITP3() {
        try {
        test.itp3(true);
        test.itp3(false);
    } catch (NullPointerException e) { return; }
    fail();
    }
    
}
