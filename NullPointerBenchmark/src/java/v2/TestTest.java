package v2;

import junit.framework.TestCase;

public class TestTest extends TestCase {
    
    Test test;

    public void setUp() {
        test = new Test();
    }
    public void testTP1() {
        for(int i = -1; i <= 5; i++)
            test.tp1(i);
    }
    public void testFP1() {
        for(int i = -1; i <= 5; i++)
            test.fp1(i);
    }
    public void testTP2() {
        test.tp2(true);
        test.tp2(false);
    }
    public void testFP2() {
        test.fp2(true);
        test.fp2(false);
    }
    public void testTP3() {
        test.tp3(null);
        test.tp3("x");
    }
    public void testTF3() {
        test.fp3(null);
        test.fp3("x");
    }
    public void testTP4() {
        test.tp4(true);
        test.tp4(false);
    }
    public void testFP4() {
        test.fp4(true);
        test.fp4(false);
    }
    public void testTP5() {
        test.tp5(null);
        test.tp5("x");
    }
    public void testTP6() {
        test.tp6(null);
        test.tp6("x");
    }
    public void testIFP1() {
        
        test.ifp1(false);
        test.ifp1(true);
       
        
    }
    public void testITP1() {
        test.itp1(false);
        test.itp1(true);
    }
    
    public void testITP2() {
        test.itp2();
    }
    
    public void testITP3() {
        test.itp3(true);
        test.itp3(false);
    }
    
}
