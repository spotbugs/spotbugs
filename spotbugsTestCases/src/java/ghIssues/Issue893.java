package ghIssues;

public class Issue893 {
    public boolean test() {
        Boolean b = Boolean.valueOf(true);
        // Line below should not throw IllegalArgumentException during analysis
        b &= Boolean.valueOf(false);
        return b;
    }
}
