package sfBugs;

import java.math.BigDecimal;

import edu.umd.cs.findbugs.annotations.Confidence;
import edu.umd.cs.findbugs.annotations.ExpectWarning;
import edu.umd.cs.findbugs.annotations.NoWarning;

public class RFE2891944 {

    @ExpectWarning("DMI_BIGDECIMAL_CONSTRUCTED_FROM_DOUBLE")
    public static void bug1() {
        BigDecimal bd = new BigDecimal(0.1);
        System.out.println(bd);
    }

    @NoWarning(value="DMI_BIGDECIMAL_CONSTRUCTED_FROM_DOUBLE", confidence=Confidence.MEDIUM)
    public static void bug2() {
        BigDecimal bd = new BigDecimal(100.0);
        System.out.println(bd);
    }

    @NoWarning(value="DMI_BIGDECIMAL_CONSTRUCTED_FROM_DOUBLE", confidence=Confidence.MEDIUM)
    public static void bug3() {
        BigDecimal bd = new BigDecimal(1.0);
        System.out.println(bd);
    }
    
    public static void main(String args[]) {
        bug1();
        bug2();
        bug3();
    }

}
