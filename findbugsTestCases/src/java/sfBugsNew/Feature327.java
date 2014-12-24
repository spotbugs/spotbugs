package sfBugsNew;

import edu.umd.cs.findbugs.annotations.ExpectWarning;
import edu.umd.cs.findbugs.annotations.NoWarning;

public class Feature327 {
    @ExpectWarning("RpC_REPEATED_CONDITIONAL_TEST")
    public int test(int a, int b, int c) {
        if((a % 2 == 0 && b % 2 == 0 && c % 2 != 0) ||
                (a % 2 == 0 && b % 2 == 0 && c % 2 != 0)) {
            return 1;
        }
        return 0;
    }
    
    @NoWarning("RpC_REPEATED_CONDITIONAL_TEST")
    public int testOk(int a, int b, int c) {
        if((a % 2 == 0 && b % 2 == 0 && c % 2 != 0) ||
                (a % 2 == 0 && b % 2 == 0 && c % 2 == 0)) {
            return 1;
        }
        return 0;
    }
    
    @ExpectWarning("RpC_REPEATED_CONDITIONAL_TEST")
    public int testJmol(String line) {
        if (line.length() > 37 && (line.charAt(36) == 0 && line.charAt(37) == 100
                || line.charAt(36) == 0 && line.charAt(37) == 100)) {
            return 1;
        }
        return 0;
    }
    
    @ExpectWarning("RpC_REPEATED_CONDITIONAL_TEST")
    public int testOr(int a, int b) {
        if((a > 0 && b > 0) || (a > 0 && b > 0)) {
            return 1;
        }
        return 0;
    }
    
    @ExpectWarning("RpC_REPEATED_CONDITIONAL_TEST")
    public int testAnd(int a, int b) {
        if((a > 0 || b > 0) && (a > 0 || b > 0)) {
            return 1;
        }
        return 0;
    }

    @ExpectWarning("RpC_REPEATED_CONDITIONAL_TEST")
    public int testRepeatingAnd(int a, int b, int c) {
        if ((a % 2 == 0 && b % 2 == 0 && c % 2 != 0) || (a % 2 == 0 && b % 2 != 0 && c % 2 == 0)
                || (a % 2 != 0 && b % 2 == 0 && c % 2 == 0) || (a % 2 != 0 && b % 2 != 0 && c % 2 == 0)
                || (a % 2 == 0 && b % 2 != 0 && c % 2 != 0) && (a % 2 == 0 && b % 2 != 0 && c % 2 != 0)) {
            return 1;
        }
        return 0;
    }

    @NoWarning("RpC_REPEATED_CONDITIONAL_TEST")
    public int testRepeatingOk(int a, int b, int c) {
        if ((a % 2 == 0 && b % 2 == 0 && c % 2 != 0) || (a % 2 == 0 && b % 2 != 0 && c % 2 == 0)
                || (a % 2 != 0 && b % 2 == 0 && c % 2 == 0) || (a % 2 == 0 && b % 2 != 0 && c % 2 != 0)
                || (a % 2 != 0 && b % 2 != 0 && c % 2 == 0) && (a % 2 == 0 && b % 2 != 0 && c % 2 != 0)) {
            return 1;
        }
        return 0;
    }
    
    @NoWarning("RpC_REPEATED_CONDITIONAL_TEST")
    public int testRepeatingConfusingOk(String s1, String s2) {
        if(s1.equals("a") && 
                s2.equals("b") || s1.equals("c") && 
                s2.equals("b") || s1.equals("c") && 
           s2.equals("d")) {
            return 1;
        }
        return 0;
    }

    @ExpectWarning("RpC_REPEATED_CONDITIONAL_TEST")
    public void testRepeatingMiddle(int a, int b, int c) {
        if ((a % 2 == 0 && b % 2 == 0 && c % 2 != 0) || (a % 2 == 0 && b % 2 != 0 && c % 2 == 0)
                || (a % 2 == 0 && b % 2 != 0 && c % 2 != 0) || (a % 2 == 0 && b % 2 != 0 && c % 2 != 0)
                || (a % 2 != 0 && b % 2 == 0 && c % 2 == 0) || (a % 2 != 0 && b % 2 != 0 && c % 2 == 0))
            System.out.println("qqq");
    }

    @ExpectWarning("RpC_REPEATED_CONDITIONAL_TEST")
    public void testRepeatingBegin(int a, int b, int c) {
        if ((a % 2 == 0 && b % 2 != 0 && c % 2 != 0) || (a % 2 == 0 && b % 2 != 0 && c % 2 != 0)
                || (a % 2 == 0 && b % 2 == 0 && c % 2 != 0) || (a % 2 == 0 && b % 2 != 0 && c % 2 == 0)
                || (a % 2 != 0 && b % 2 == 0 && c % 2 == 0) || (a % 2 != 0 && b % 2 != 0 && c % 2 == 0))
            System.out.println("qqq");
    }

    @ExpectWarning("RpC_REPEATED_CONDITIONAL_TEST")
    public int testTernaryOr(int a) {
        if(((a % 2 == 0 ? 2 : 3) > 2) ||
                ((a % 2 == 0 ? 2 : 3) > 2)) {
            return 1;
        }
        return 0;
    }

    @ExpectWarning("RpC_REPEATED_CONDITIONAL_TEST")
    public int testTernaryAnd(int a) {
        if(((a % 2 == 0 ? 2 : 3) > 2) &&
                ((a % 2 == 0 ? 2 : 3) > 2)) {
            return 1;
        }
        return 0;
    }
}
