package sfBugsNew;

import edu.umd.cs.findbugs.annotations.DesireWarning;
import edu.umd.cs.findbugs.annotations.ExpectWarning;

public class Bug1197 {

    @ExpectWarning("EC_UNRELATED_TYPES")
    public void test1() {
        Integer x = 5;
        String y = "5";
        System.out.println(java.util.Objects.equals(x, y));
    }

    @ExpectWarning("EC_UNRELATED_TYPES")
    public void test2() {
        Integer x = 5;
        String y = "5";
        System.out.println(x.equals(y));
    }

    @ExpectWarning("EC_UNRELATED_TYPES")
    public void test3() {
        Integer x = 5;
        String y = "5";
        System.out.println(com.google.common.base.Objects.equal(x, y));
    }

    @ExpectWarning("EC_UNRELATED_TYPES")
    public void test4() {
        Integer x = 5;
        String y = "5";
        junit.framework.Assert.assertEquals(x, y);
    }

    @ExpectWarning("EC_UNRELATED_TYPES")
    public void test5() {
        Integer x = 5;
        String y = "5";
        org.junit.Assert.assertEquals(x, y);
    }

    @ExpectWarning("EC_UNRELATED_TYPES")
    public void test6() {
        Integer x = 5;
        String y = "5";
        org.testng.Assert.assertEquals(x, y);
    }

    @DesireWarning("EC_UNRELATED_TYPES")
    public void test7() {
        Integer x = 5;
        String y = "5";
        org.testng.Assert.assertEquals(x, y, "oops");
    }

}
