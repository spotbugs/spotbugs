package ghIssues.issue2547;

public class MyEx extends Exception {
    public MyEx(String errorMsg) {
        super(errorMsg);
    }

    public static MyEx somethingWentWrong(String errorMsg) {
        return new MyEx(errorMsg);
    }
}
