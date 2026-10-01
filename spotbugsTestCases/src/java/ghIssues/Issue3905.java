package ghIssues;

/**
 * Test cases for GitHub issue #3905:
 * SF_SWITCH_NO_DEFAULT false negative when last case contains only a break statement.
 */
public class Issue3905 {

    // True positive: last case has logic before the break - should report SF_SWITCH_NO_DEFAULT
    public void lastCaseWithLogic(int x) {
        switch (x) {
            case 0: x = 0; break;
            case 1: x = 1; break;
        }
    }

    // False negative (now fixed): last case contains only a break - should report SF_SWITCH_NO_DEFAULT
    public void lastCaseOnlyBreak(int x) {
        switch (x) {
            case 0: x = 0; break;
            case 1: break;
        }
    }

    // Single case with only a break and no default - should report SF_SWITCH_NO_DEFAULT
    public void singleCaseOnlyBreak(int x) {
        switch (x) {
            case 0: break;
        }
    }

    // Should NOT report SF_SWITCH_NO_DEFAULT: has explicit default
    public void lastCaseOnlyBreakWithDefault(int x) {
        switch (x) {
            case 0: x = 0; break;
            case 1: break;
            default: break;
        }
    }

    // Should NOT report SF_SWITCH_NO_DEFAULT: has explicit empty default
    public void withExplicitEmptyDefault(int x) {
        switch (x) {
            case 0: x = 0; break;
            default:
        }
    }
}
