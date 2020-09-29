package ghIssues;

public class Issue893 {
    public boolean test() throws Exception {
        Boolean b = Boolean.valueOf(true);
        // Line below should not throw IllegalArgumentException during analysis
        b &= Boolean.valueOf(false);
        return b;
    }
}
