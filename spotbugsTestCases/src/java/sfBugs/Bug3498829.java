package sfBugs;

import edu.umd.cs.findbugs.annotations.NoWarning;

public class Bug3498829 {
    
    static enum Color {RED, YELLOW, GREEN };
    
    @NoWarning("SF_SWITCH_NO_DEFAULT")
    public static  int getValue(Color c) {
        
        int result = 0;
        switch(c) {
        case RED:
            result = 1;
            break;
        case YELLOW:
            result = 2;
            break;
        case GREEN:
            result = 3;
            break;
        }
        return result;
    }

}
