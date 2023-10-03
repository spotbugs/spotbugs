package sfBugsNew;

import edu.umd.cs.findbugs.annotations.ExpectWarning;
import edu.umd.cs.findbugs.annotations.NoWarning;

public class Feature320 {
    public int add(int a, int b) {
        return a+b;
    }

    // side effect is here
    public int printAndAdd(int a, int b) {
        System.out.println(a+"+"+b+"="+(a+b));
        return a+b;
    }

    @ExpectWarning("RpC_REPEATED_CONDITIONAL_TEST")
    public boolean check1(int a, int b) {
        return add(a, b) < 10 && add(a, b) < 10;
    }

    @NoWarning("RpC_REPEATED_CONDITIONAL_TEST")
    public boolean check2(int a, int b) {
        return printAndAdd(a, b) < 10 && printAndAdd(a, b) < 10;
    }
}
