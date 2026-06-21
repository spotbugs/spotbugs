package ghIssues;

import edu.umd.cs.findbugs.annotations.ExpectWarning;

class Issue3456 {
    private static final int C = 0b1010;
    private static final int D = 0b1100;

    @ExpectWarning("BIT_AND")
    public boolean showBugWithTemporary(int e) {
        int temporary = (e & C);
        return temporary == D;
    }

    @ExpectWarning("BIT_AND")
    public boolean showBugDirect(int e) {
        return (e & C) == D;
    }

    @ExpectWarning("BIT_AND_ZZ")
    public boolean showBugZeroMaskWithTemporary(int number) {
        int temporary = number & 0;
        return temporary == 0;
    }
}
