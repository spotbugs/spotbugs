package ghIssues;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;

public class Issue3243 {

    @NonNull
    String hoge = getHoge();

    public void bugSample() {
        System.out.println(hoge);
    }

    @Nullable
    private String getHoge() {
        return "sample";
    }
}
