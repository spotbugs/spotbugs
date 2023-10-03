package ghIssues.issue2402;
import java.util.HashMap;
public class TestD {
    static class UMap extends HashMap<TestD, String> {}
    ;
    static HashMap<TestD, String> m =
            new HashMap<TestD, String>();

    static int foo(HashMap<TestD, String> map) {
        return map.size();
    }
    @Override
    public boolean equals(Object o) {
        return hashCode() == o.hashCode();
    }
    public static String add(TestD b, String s) {
        return m.put(b, s);
    }
    public static String get(TestD b) {
        return m.get(b);
    }
}
