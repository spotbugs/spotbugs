package ghIssues.issue2402;
import java.util.HashMap;
public class TestB {
    static class UMap extends HashMap<TestB, String> {}
    ;
    static HashMap<TestB, String> m =
            new HashMap<TestB, String>();

    static int foo(HashMap<TestB, String> map) {
        return map.size();
    }
    @Override
    public boolean equals(Object o) {
        return hashCode() == o.hashCode();
    }
    public static String add(TestB b, String s) {
        return m.put(b, s);
    }
    public static String get(TestB b) {
        return m.get(b);
    }
}
