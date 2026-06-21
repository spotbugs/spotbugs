package ghIssues;

import java.util.List;
import java.util.Set;

/**
 * <a href="https://github.com/spotbugs/spotbugs/issues/4033">#4033</a>.
 */
public class Issue4033 {

    private static final List<String> NAMES_OK = List.of("a", "b", "c");
    private static final Set<Integer> CODES_OK = Set.of(1, 2, 3);

    private static final List<String> NAMES = List.copyOf(List.of("a", "b", "c"));
    private static final Set<Integer> CODES = Set.copyOf(Set.of(1, 2, 3));
    private final List<String> tags = List.copyOf(List.of("x", "y", "z"));

    public List<String> getNamesOk() {
        return NAMES_OK;
    }

    public Set<Integer> getCodesOk() {
        return CODES_OK;
    }

    public List<String> getNames() {
        return NAMES;
    }

    public Set<Integer> getCodes() {
        return CODES;
    }

    public List<String> getTags() {
        return tags;
    }
}
