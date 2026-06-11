package ghIssues;

import edu.umd.cs.findbugs.annotations.ExpectWarning;

class Issue3464 {
    @ExpectWarning("MS_SHOULD_BE_FINAL")
    public static StringBuilder mutableStaticField = new StringBuilder("Initial Value");

    public String showBug(String newValue) {
        mutableStaticField.setLength(0);
        mutableStaticField.append(newValue);
        return mutableStaticField.toString();
    }
}
