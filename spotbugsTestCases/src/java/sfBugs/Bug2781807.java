package sfBugs;

import edu.umd.cs.findbugs.annotations.ExpectWarning;

public class Bug2781807 {
    private Object field;

    @ExpectWarning("BC_IMPOSSIBLE_INSTANCEOF,BC_IMPOSSIBLE_CAST")
    public void method() {
        if (field instanceof String) {
            String fieldText = (String) field;
            field = Integer.valueOf(fieldText);
        }
    }
}
