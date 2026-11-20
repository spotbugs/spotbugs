package ghIssues;

import java.util.List;

/**
 * A null check that does not copy the parameter still exposes it
 * (<a href="https://github.com/spotbugs/spotbugs/issues/4033">#4033</a>).
 */
public final class Issue4033Uncopied {
    private final List<String> names;

    public Issue4033Uncopied(List<String> names) {
        if (names != null) {
            names.size();
        }
        this.names = names;
    }

    public List<String> getNames() {
        return names;
    }
}
