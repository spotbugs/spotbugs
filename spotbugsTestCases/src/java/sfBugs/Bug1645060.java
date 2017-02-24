package sfBugs;

public class Bug1645060 {

    /*
     * SourceForge.net message: IM_BAD_CHECK_FOR_ODD thrown on code: if
     * (Math.abs(myint) % 2 == 1) return null;
     * 
     * This code using Math.abs() is perfectly valid and works just fine for
     * negative numbers, contrary to the bug reported by Findbugs.
     * 
     * 
     * ################### IM: Check for oddness that won't work for negative
     * numbers (IM_BAD_CHECK_FOR_ODD)
     * 
     * The code uses x % 2 == 1 to check to see if a value is odd, but this
     * won't work for negative numbers (e.g., (-5) % 2 == -1). If this code is
     * intending to check for oddness, consider using x & 1 == 1, or x % 2 != 0.
     * ###################
     * 
     * They seem to have a valid agruement.
     */

    /**
     * @param args
     */
    public String checkForEven(int myint) {

        if (Math.abs(myint) % 2 == 1)
            return null;

        return "even";
    }

}
