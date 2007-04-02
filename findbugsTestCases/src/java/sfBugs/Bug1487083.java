package sfBugs;

public class Bug1487083 {
    static public int falsePos;
    static {
        try {
            falsePos = Integer.parseInt(System.getProperty("false.positive"));
        } catch (NumberFormatException nfe) {
            falsePos = 10;
        }
    }
}