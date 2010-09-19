package sfBugs;

import edu.umd.cs.findbugs.annotations.DesireNoWarning;

public class Bug1487083 {
    @DesireNoWarning("MS")
    static public int falsePos;
    static {
		try {
            falsePos = Integer.parseInt(System.getProperty("false.positive"));
        } catch (NumberFormatException nfe) {
            falsePos = 10;
        }
    }
}
