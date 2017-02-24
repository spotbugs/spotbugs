package sfBugsNew;
import edu.umd.cs.findbugs.annotations.ExpectWarning;
import edu.umd.cs.findbugs.annotations.NoWarning;

public class Feature294 {
    private static final float PI_FLOAT = 3.14159f;
    private static final double PI_DOUBLE = 3.14159;

    @NoWarning("CNT_ROUGH_CONSTANT_VALUE")
    public void testNan() {
        double a;
        a = Double.NaN;
        a = Double.POSITIVE_INFINITY;
        a = Double.NEGATIVE_INFINITY;
        float b;
        b = Float.NaN;
        b = Float.POSITIVE_INFINITY;
        b = Float.NEGATIVE_INFINITY;
    }

    @NoWarning("CNT_ROUGH_CONSTANT_VALUE")
    public void testNoWarning() {
        double a;
        a = Math.PI; // Exact
        a = 3.141512345; // Explicitly specified something different from PI
        a = 3.1417;
        a = 3.1414;
        float b;
        b = (float)Math.PI; // Exact value converted to float
        b = 3.141512345f; // Explicitly specified something different from PI
        b = 3.1417f;
        b = 3.1414f;
    }

    @ExpectWarning("CNT_ROUGH_CONSTANT_VALUE")
    public void testPi1() {
        System.out.println(3.141592);
    }

    @ExpectWarning("CNT_ROUGH_CONSTANT_VALUE")
    public void testPi2() {
        System.out.println(3.141592612345); // Something different, but too close to PI, thus suspicious
    }

    @ExpectWarning("CNT_ROUGH_CONSTANT_VALUE")
    public void testPi3() {
        System.out.println(3.1415f);
    }

    @ExpectWarning("CNT_ROUGH_CONSTANT_VALUE")
    public void testPi4() {
        System.out.println(PI_DOUBLE);
    }

    @ExpectWarning("CNT_ROUGH_CONSTANT_VALUE")
    public void testPi5() {
        System.out.println(PI_FLOAT);
    }

    @ExpectWarning("CNT_ROUGH_CONSTANT_VALUE")
    public void testPi6() {
        System.out.println(PI_FLOAT);
    }

    @ExpectWarning("CNT_ROUGH_CONSTANT_VALUE")
    public void test2Pi1() {
        System.out.println(2*3.141592);
    }

    @ExpectWarning("CNT_ROUGH_CONSTANT_VALUE")
    public void test2Pi2() {
        System.out.println(2*3.1416);
    }

    @ExpectWarning("CNT_ROUGH_CONSTANT_VALUE")
    public void test2Pi3() {
        System.out.println(2*3.1415);
    }

    @ExpectWarning("CNT_ROUGH_CONSTANT_VALUE")
    public void test2Pi4() {
        System.out.println(6.2831);
    }

    @ExpectWarning("CNT_ROUGH_CONSTANT_VALUE")
    public void testE1() {
        System.out.println(2.7183);
    }

    @ExpectWarning("CNT_ROUGH_CONSTANT_VALUE")
    public void testE2() {
        System.out.println(2.71828f);
    }

    @NoWarning("CNT_ROUGH_CONSTANT_VALUE")
    public void testE2Digits() {
        // Too far away from real value and E is not very popular number
        System.out.println(2.72);
    }

    @ExpectWarning("CNT_ROUGH_CONSTANT_VALUE")
    public void testPi2Digits() {
        // Pi is more popular, thus likely a bug
        System.out.println(3.14);
    }

    @NoWarning("CNT_ROUGH_CONSTANT_VALUE")
    public void testDoubleArray() {
        double[] arr = new double[] {2.7183, 2.7184, 2.7185};
        System.out.println(arr[0]);
    }

    @NoWarning("CNT_ROUGH_CONSTANT_VALUE")
    public void testDoubleArray2() {
        Double[] arr = new Double[] {2.7183, 2.7184, 2.7185};
        System.out.println(arr[0]);
    }

    @ExpectWarning("CNT_ROUGH_CONSTANT_VALUE")
    public void testDoubleArrayHighPrecision() {
        Double[] arr = new Double[] {2.718281828, 2.719, 2.72};
        System.out.println(arr[0]);
    }
}
