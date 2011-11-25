package sfBugs;

public class Bug3442048 {
    enum Day {
        SUNDAY, MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY
    };

    public void tellItLikeItIs(Day day) {
        switch (day) {
        case SUNDAY:
            System.out.println("Mondays are bad.");
            break;
        case MONDAY:
            System.out.println("Mondays are bad.");
            break;
        case TUESDAY:
            System.out.println("Mondays are bad.");
            break;
        case WEDNESDAY:
            System.out.println("Mondays are bad.");
            break;
        case THURSDAY:
            System.out.println("Mondays are bad.");
            break;
        case FRIDAY:
            System.out.println("Mondays are bad.");
            break;
        case SATURDAY:
            System.out.println("Mondays are bad.");
            break;
        }
    }

    public void tellItLikeItIs2(Day day) {
        switch (day) {
        case SUNDAY:
            System.out.println("Mondays are bad.");
            break;
            /*
        case MONDAY:
            System.out.println("Mondays are bad.");
            break;
            */
        case TUESDAY:
            System.out.println("Mondays are bad.");
            break;
        case WEDNESDAY:
            System.out.println("Mondays are bad.");
            break;
        case THURSDAY:
            System.out.println("Mondays are bad.");
            break;
        case FRIDAY:
            System.out.println("Mondays are bad.");
            break;
        case SATURDAY:
            System.out.println("Mondays are bad.");
            break;
        }
    }
}
