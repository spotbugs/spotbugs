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
        double a = 3.141592;
    }

    @ExpectWarning("CNT_ROUGH_CONSTANT_VALUE")
    public void testPi2() {
        double a = 3.141592612345; // Something different, but too close to PI, thus suspicious
    }

    @ExpectWarning("CNT_ROUGH_CONSTANT_VALUE")
    public void testPi3() {
        float a = 3.1415f;
    }

    @ExpectWarning("CNT_ROUGH_CONSTANT_VALUE")
    public void testPi4() {
        double a = PI_DOUBLE;
    }

    @ExpectWarning("CNT_ROUGH_CONSTANT_VALUE")
    public void testPi5() {
        double a = PI_FLOAT;
    }

    @ExpectWarning("CNT_ROUGH_CONSTANT_VALUE")
    public void testPi6() {
        float a = PI_FLOAT;
    }

    @ExpectWarning("CNT_ROUGH_CONSTANT_VALUE")
    public void test2Pi1() {
        double a = 2*3.141592;
    }

    @ExpectWarning("CNT_ROUGH_CONSTANT_VALUE")
    public void test2Pi2() {
        double a = 2*3.1416;
    }

    @ExpectWarning("CNT_ROUGH_CONSTANT_VALUE")
    public void test2Pi3() {
        double a = 2*3.1415;
    }

    @ExpectWarning("CNT_ROUGH_CONSTANT_VALUE")
    public void test2Pi4() {
        double a = 6.2831;
    }

    @ExpectWarning("CNT_ROUGH_CONSTANT_VALUE")
    public void testE1() {
        double a = 2.71828;
    }

    @ExpectWarning("CNT_ROUGH_CONSTANT_VALUE")
    public void testE2() {
        float a = 2.71828f;
    }

}
