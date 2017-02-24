package bugIdeas;

public class Ideas_2009_01_16 {
    int x;

    public int getValueForYear(int year) {
        switch (year) {
        case 2005:
            return 1;
        case 2006:
            return 1;
        case 2007:
            return 1;
        case 2008:
            return 1;
        default:
            throw new IllegalArgumentException("bad year: " + year);
        }
    }

    public static String getAge(Ideas_2009_01_16 x) {
        return "x = " + x.toString();

    }
}
