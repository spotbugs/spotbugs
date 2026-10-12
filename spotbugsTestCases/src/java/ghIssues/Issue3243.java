package ghIssues;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

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
