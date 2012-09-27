package sfBugs;

import edu.umd.cs.findbugs.annotations.DesireNoWarning;

public class Bug3553542 {
    
    @DesireNoWarning( "RCN_REDUNDANT_NULLCHECK_OF_NONNULL_VALUE")
    public static void checkIfNullIsReturned(GoodBehavingClass goodBehavingClass) {
        String isNullReturned;

        isNullReturned = goodBehavingClass.isNullReturned();

        if (isNullReturned != null) {
            System.out.println("No it isn't. It has length " + isNullReturned.length());
        } else {
            System.out.println("Yes it is.");
        }
    }

    static class GoodBehavingClass {
        public String isNullReturned() {
            return "noop";
        }
    }

    // Assume this is class is defined in some the third party implementation
    // using this FalsePositive as library code.
    static class BadBehavingClass /* extends GoodBehavingClass */ {
        
        public String isNullReturned() {
            return null;
        }
    }

    public static void main(String[] args) {
        checkIfNullIsReturned(new GoodBehavingClass());
        // Assume this is called in the third party implementation using this
        // FalsePositive as library code.
//        checkIfNullIsReturned(new BadBehavingClass());
    }
}
