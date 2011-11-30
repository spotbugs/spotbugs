package sfBugs;

import edu.umd.cs.findbugs.annotations.ExpectWarning;
import edu.umd.cs.findbugs.annotations.NoWarning;

public class Bug3442048 {
    enum Day {
        SUNDAY, MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY
    };
    
    @ExpectWarning("SF_SWITCH_NO_DEFAULT")
    public void switchOnInt(int x) {
        switch (x) {
        case 1:
            System.out.println("1 is bad.");
            break;
        case 2: 
            System.out.println("2 is bad.");
            break;
        case 3:
            System.out.println("3 is bad.");
            break;
        }
        System.out.println("How is your number?");
    }

    @NoWarning("SF_SWITCH_NO_DEFAULT")
    public void tellItLikeItIs(Day day) {
        switch (day) {
        case SUNDAY:
            System.out.println("Mondays are bad, but Sunday is OK.");
            break;
        case MONDAY:
            System.out.println("Mondays are bad.");
            break;
        case TUESDAY:
            System.out.println("Mondays are bad, but Tuesday is OK.");
            break;
        case WEDNESDAY:
            System.out.println("Mondays are bad, but Wednesday is OK.");
            break;
        case THURSDAY:
            System.out.println("Mondays are bad, but Thursday is OK.");
            break;
        case FRIDAY:
            System.out.println("Mondays are bad, but Friday is OK.");
            break;
        case SATURDAY:
            System.out.println("Mondays are bad, but Saturday is OK.");
            break;
        }
        System.out.println("How is your day?");
    }

    @ExpectWarning("SF_SWITCH_NO_DEFAULT")
    public void tellItLikeItIs2(Day day) {
        switch (day) {
        case SUNDAY:
            System.out.println("Mondays are bad, but Sunday is OK.");
            break;
//        case MONDAY:
//            System.out.println("Mondays are bad.");
//            break;
        case TUESDAY:
            System.out.println("Mondays are bad, but Tuesday is OK.");
            break;
        case WEDNESDAY:
            System.out.println("Mondays are bad, but Wednesday is OK.");
            break;
        case THURSDAY:
            System.out.println("Mondays are bad, but Thursday is OK.");
            break;
        case FRIDAY:
            System.out.println("Mondays are bad, but Friday is OK.");
            break;
        case SATURDAY:
            System.out.println("Mondays are bad, but Saturday is OK.");
            break;
        }
        System.out.println("How is your day?");
    }
    
    @ExpectWarning("SF_SWITCH_NO_DEFAULT")
    
    public void tellItLikeItIs3(Day day) {
        switch (day) {
//        case SUNDAY:
//            System.out.println("Mondays are bad, but Sunday is OK.");
//            break;
//        case MONDAY:
//            System.out.println("Mondays are bad.");
//            break;
//        case TUESDAY:
//            System.out.println("Mondays are bad, but Tuesday is OK.");
//            break;
//        case WEDNESDAY:
//            System.out.println("Mondays are bad, but Wednesday is OK.");
//            break;
        case THURSDAY:
            System.out.println("Mondays are bad, but Thursday is OK.");
            break;
        case FRIDAY:
            System.out.println("Mondays are bad, but Friday is OK.");
            break;
        case SATURDAY:
            System.out.println("Mondays are bad, but Saturday is OK.");
            break;
        }
        System.out.println("How is your day?");
    }

}
