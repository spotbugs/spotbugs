package ghIssues.issue2402;
import java.util.HashMap;
public class TestC {
    static class UMap extends HashMap<TestC, String> {}
    ;
    static HashMap<TestC, String> m =
            new HashMap<TestC, String>();

    static int foo(HashMap<TestC, String> map) {
        return map.size();
    }
    @Override
    public boolean equals(Object o) {
        return hashCode() == o.hashCode();
    }
    public static String add(TestC b, String s) {
        return m.put(b, s);
    }
    public static String get(TestC b) {
        return m.get(b);
    }
}
