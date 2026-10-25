package sfBugsNew;

import edu.umd.cs.findbugs.annotations.DesireWarning;
import edu.umd.cs.findbugs.annotations.ExpectWarning;

import com.google.common.base.Objects;

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
        System.out.println(Objects.equal(x, y));
    }

    @ExpectWarning("EC_UNRELATED_TYPES")
    public void test4() {
        Integer x = 5;
        String y = "5";
        org.junit.jupiter.api.Assertions.assertEquals(x, y);
    }

    @ExpectWarning("EC_UNRELATED_TYPES")
    public void test5() {
        Integer x = 5;
        String y = "5";
        org.testng.Assert.assertEquals(x, y);
    }

    @DesireWarning("EC_UNRELATED_TYPES")
    public void test6() {
        Integer x = 5;
        String y = "5";
        org.testng.Assert.assertEquals(x, y, "oops");
    }

}
