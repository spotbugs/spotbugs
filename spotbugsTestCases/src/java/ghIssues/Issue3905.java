package ghIssues;

import edu.umd.cs.findbugs.annotations.ExpectWarning;

class Issue3905 {
    @ExpectWarning("SF_SWITCH_NO_DEFAULT")
    public void testWithAssignmentInLastCase(int x) {
        switch (x) {
        case 0:
            x = 0;
            break;
        case 1:
            x = 1;
            break;
        }
    }

    @ExpectWarning("SF_SWITCH_NO_DEFAULT")
    public void testWithBreakOnlyInLastCase(int x) {
        switch (x) {
        case 0:
            x = 0;
            break;
        case 1:
            break;
        }
    }
}
