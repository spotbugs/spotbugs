package sfBugsNew;

import edu.umd.cs.findbugs.annotations.ExpectWarning;
import edu.umd.cs.findbugs.annotations.NoWarning;

public class Bug1392 {
    static enum TrafficLight {
        RED, YELLOW, GREEN;

        // introducing a public static final <EnumType> field introduces
        // a SF_SWITCH_NO_DEFAULT bug
        public static final TrafficLight GO = GREEN;
    }
    
    @NoWarning("SF_SWITCH_NO_DEFAULT")
    public void allCovered(TrafficLight light) {
        // default case is missing, but all cases are covered
        switch (light) {
            case RED:
                System.out.println("red");
                break;
            case YELLOW:
                System.out.println("yellow");
                break;
            case GREEN:
                System.out.println("green");
                break;
        }
    }
    
    @ExpectWarning("SF_SWITCH_NO_DEFAULT")
    public void notAllCovered(TrafficLight light) {
        // default case is missing, and not all the cases are covered
        switch (light) {
        case YELLOW:
            System.out.println("yellow");
            break;
        case GREEN:
            System.out.println("green");
            break;
        }
    }
}
