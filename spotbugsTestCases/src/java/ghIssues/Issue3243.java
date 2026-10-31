package ghIssues;

// TODO: Migrate this test to JSpecify annotations once the legacy SpotBugs
// annotation handling is removed. JSpecify was previously ignored here because
// it was not yet supported; with JSpecify support enabled, the nullable return
// contract correctly produces an NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE finding.
// This test currently retains the legacy annotations to exercise the existing
// annotation handling independently of that migration.
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;

public class Issue3243 {

    @NonNull
    String hoge = getHoge();

    public void bugSample() {
        System.out.println(hoge);
    }

    private @Nullable String getHoge() {
        return "sample";
    }
}
