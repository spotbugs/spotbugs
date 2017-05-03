package sfBugsNew;

import edu.umd.cs.findbugs.annotations.DesireNoWarning;
import edu.umd.cs.findbugs.annotations.ExpectWarning;

public class Bug1172 {
    
    
    @ExpectWarning("SF_SWITCH_FALLTHROUGH")
    public int testFallThrough(int x) {
        switch (x) {
        case 0:
            System.out.println("Hello");
        case 1:
            return 17;
        case 2:
            return 345;
        }
        return 0;
    }

    /** Getting this requires parsing source */
    @DesireNoWarning("SF_SWITCH_FALLTHROUGH")
    public int testFallThroughGood(int x) {
        
        switch (x) {
        case 0:
            System.out.println("Hello");
            // intentional fall-through"
        case 1:
            return 17;
        case 2:
            return 345;
        }
        return 0;
    }

}
