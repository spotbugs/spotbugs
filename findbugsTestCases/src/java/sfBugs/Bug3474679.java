package sfBugs;

import java.math.BigDecimal;
import junit.framework.Assert;
import org.junit.Test;
import edu.umd.cs.findbugs.annotations.ExpectWarning;

public class Bug3474679
{
    @ExpectWarning(value = "EC_UNRELATED_TYPES", num=4)
    @Test
    public void testEqualsOK()
    {
        Assert.assertEquals(new Double(0), new BigDecimal("0")); //correctly flagged as a bug
        Assert.assertEquals(new Double(0), new CustomObject()); //identical bug type is missed

        if (new Double(0).equals(new BigDecimal("0"))) //correctly flagged as a bug
            ;
        if (new Double(0).equals(new CustomObject())) //identical bug type is missed
            ;
    }

    @ExpectWarning(value = "EC_UNRELATED_TYPES", num=2)
    @Test
    public void testEqualsFalseNegative()
    {
       Assert.assertEquals(new Double(0), new CustomObject()); //identical bug type is missed

       if (new Double(0).equals(new CustomObject())) //identical bug type is missed
            ;
    }
    
    @ExpectWarning(value = "EC_UNRELATED_TYPES", num=4)
    public void testEquals2()
    {
        Assert.assertEquals(new Double(0), new BigDecimal("0")); //correctly flagged as a bug
        Assert.assertEquals(new Double(0), new CustomObject()); //correctly flagged as a bug

        if (new Double(0).equals(new BigDecimal("0"))) //correctly flagged as a bug
            ;
        if (new Double(0).equals(new CustomObject())) //correctly flagged as a bug
            ;
    }

    private static class CustomObject
    {
        //NOP
    }
}

