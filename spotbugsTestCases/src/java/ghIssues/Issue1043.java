package ghIssues;

import java.util.ArrayList;
import java.util.List;

public class Issue1043 {

    private static String className = Issue1043.class.getSimpleName();

    private List<String> strings;

    public void add1() {
        strings.add(className);
    }

    public void add2() {
        final List<String> localStrings = new ArrayList<>();
        localStrings.add(className);
    }
}
