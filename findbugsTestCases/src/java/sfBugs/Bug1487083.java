package sfBugs;

import edu.umd.cs.findbugs.annotations.ExpectWarning;
import edu.umd.cs.findbugs.annotations.NoWarning;

public class Bug1487083 {
    @NoWarning("MS_SHOULD_BE_FINAL")
    @ExpectWarning("MS_SHOULD_BE_REFACTORED_TO_BE_FINAL")
    static public int falsePos;

    static {
        try {
            falsePos = Integer.parseInt(System.getProperty("false.positive"));
        } catch (NumberFormatException nfe) {
            falsePos = 10;
        }
    }
}
