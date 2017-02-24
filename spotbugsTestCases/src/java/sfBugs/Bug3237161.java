package sfBugs;

import edu.umd.cs.findbugs.annotations.NoWarning;

public class Bug3237161 {
    public String methodWhichReturnsString() {
        return "";
    }

    public String methodWhichReturnsNullString() {
        return null;
    }

    @NoWarning("RCN")
    public void testRedundantNullCheck() {

        String nullString = methodWhichReturnsNullString();
        // No warning here
        if (nullString == null) {
            nullString = "Null";
        }

        String testString = methodWhichReturnsString();
        // This method can be overridden so can return null like in SubClass
        // but we get a warning saying that this is a redundant null check
        if (testString == null) {
            testString = "Null";
        }
    }

    static class SubClass extends Bug3237161 {
        @Override
        // In that case it always returns null so null check is a good thing
        public String methodWhichReturnsString() {
            return null;
        }
    }
}


