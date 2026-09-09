package ghIssues.issue2547;

public class ExceptionFactory {
    public static MyEx createMyException(int i) {
        return new MyEx(String.format("Error number: %d", i));
    }
}
