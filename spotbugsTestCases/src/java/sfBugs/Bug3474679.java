package sfBugs;

import java.math.BigDecimal;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import edu.umd.cs.findbugs.annotations.ExpectWarning;
import edu.umd.cs.findbugs.annotations.NoWarning;

public class Bug3474679
{
    @ExpectWarning(value = "EC_UNRELATED_TYPES")
    @Test
    public void testEquals1()
    {
        Assertions.assertEquals(new Double(0), new BigDecimal("0")); //correctly flagged as a bug

    }
    @ExpectWarning(value = "EC_UNRELATED_TYPES")
    @Test
    public void testEquals2()
    {
        Assertions.assertEquals(new Double(0), new CustomObject()); //identical bug type is missed
    }
    @NoWarning(value = "EC_UNRELATED_TYPES")
    @Test
    public void testEquals2a()
    {
        Assertions.assertFalse(new Double(0).equals(new CustomObject()));
    }

    @ExpectWarning(value = "EC_UNRELATED_TYPES")
    @Test
    public void testEquals3()
    {
        if (new Double(0).equals(new BigDecimal("0"))) //correctly flagged as a bug
            System.out.println("huh");

    }
    @ExpectWarning(value = "EC_UNRELATED_TYPES")
    @Test
    public void testEquals4()
    {
       if (new Double(0).equals(new CustomObject())) //identical bug type is missed
            System.out.println("huh");;
    }


    @ExpectWarning(value = "EC_UNRELATED_TYPES", num=2)
    @Test
    public void testEqualsFalseNegative()
    {
       Assertions.assertEquals(new Double(0), new CustomObject()); //identical bug type is missed

       if (new Double(0).equals(new CustomObject())) //identical bug type is missed
            ;
    }

    @ExpectWarning(value = "EC_UNRELATED_TYPES", num=4)
    public void testEquals5()
    {
        Assertions.assertEquals(new Double(0), new BigDecimal("0")); //correctly flagged as a bug
        Assertions.assertEquals(new Double(0), new CustomObject()); //correctly flagged as a bug

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

