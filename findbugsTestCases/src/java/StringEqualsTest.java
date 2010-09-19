public class StringEqualsTest {
    String x;

    public boolean test(String s) {
        return s == "hello";
    }

    public boolean test2(String s) {
        return s == x;
    }

    boolean test3(String s) {
        return s == "hello";
    }
}
