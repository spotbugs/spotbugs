package sfBugs;

import org.testng.annotations.Test;

import edu.umd.cs.findbugs.annotations.NoWarning;

public class Bug3464107 {
    
    @Test
    @NoWarning("EC_BAD_ARRAY_COMPARE")
    public void test() {
        int[] numbers = { 1, 2, 3 };
        int[] numbers2 = { 1, 2, 3 };
        org.testng.Assert.assertEquals(numbers, numbers2);
        org.testng.Assert.assertFalse(numbers.equals(numbers2));
        
    }

}
