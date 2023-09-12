package ghIssues.issue2547;

public class Issue2547 {
    // compliant
    public void throwingExCtor() throws MyEx {
        throw new MyEx("");
    }

    // non-compliant
    public void notThrowingExCtor() {
        new MyEx("");
    }

    // compliant
    public void createAndThrowExCtor() throws MyEx {
        MyEx e = new MyEx("");
        throw e;
    }

    // non-compliant
    public void notThrowingExCtorCaller() {
        MyEx.somethingWentWrong("");
    }

    // compliant
    public void createAndThrowExCtorCaller() throws MyEx {
        MyEx e = MyEx.somethingWentWrong("");
        throw e;
    }

    // non-compliant
    public void notThrowingExCtorCallerOutside() {
        ExceptionFactory.createMyException(5);
    }

    // compliant
    public void createAndThrowExCtorCallerOutside() throws MyEx {
        MyEx e = ExceptionFactory.createMyException(5);
        throw e;
    }
}
