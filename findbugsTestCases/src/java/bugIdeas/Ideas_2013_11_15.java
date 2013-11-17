package bugIdeas;

import java.math.BigDecimal;
import java.math.BigInteger;

import edu.umd.cs.findbugs.annotations.ExpectWarning;
import edu.umd.cs.findbugs.annotations.NoWarning;

public class Ideas_2013_11_15 {
    @ExpectWarning("VA_FORMAT_STRING_BAD_CONVERSION_FROM_ARRAY")
    public void passingAnArray() {
        System.out.println(System.out.printf("%s", new int[] { 42, 17 }));
    }
    
    @NoWarning("FS")
    public void valuesOtherThanInt() {
        System.out.println(String.format("%d%n%d", 42, (short) 42));
        System.out.println(String.format("%d%n", new BigInteger("42")));
        System.out.println(String.format("%f%n", new BigDecimal(42)));
    }
}
