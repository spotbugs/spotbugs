package ghIssues;

import java.util.HashMap;
import java.util.LinkedHashMap;

import edu.umd.cs.findbugs.annotations.ExpectWarning;

/**
 * <a href="https://github.com/spotbugs/spotbugs/issues/3769">#3769</a>.
 */
public class Issue3769 {
    public static void main(String[] args) {
        HashMap<?, ?> map = new HashMap<>();
        @ExpectWarning("BC_IMPOSSIBLE_DOWNCAST")
        LinkedHashMap<?, ?> linkedHashMap = (LinkedHashMap<?, ?>) map.clone();
    }
}
