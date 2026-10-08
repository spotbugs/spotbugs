package ghIssues;

public class Issue4147StoreBeforeGuard {

    Object saved;

    int storeThenGuardField(Object arg) {
        saved = arg;
        if (saved == null) {
            throw new NullPointerException();
        }
        return saved.hashCode();
    }

    void storeThenGuard(Object arg) {
        saved = arg;
        if (arg == null) {
            throw new NullPointerException();
        }
        // Field is loaded, so a store of arg is forward-substituted.
        saved.hashCode();
    }

    void callerPassesNull() {
        storeThenGuard(null);
    }

    int guardThroughLocal() {
        Object tmp = saved;
        if (tmp == null) {
            throw new NullPointerException();
        }
        return saved.hashCode();
    }
}
