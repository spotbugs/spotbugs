package ghIssues;

public class Issue4296 {

    static class Base {}
    static final class Derived extends Base {}

    // value is created as new Derived(); instanceof Derived is always true;
    // result is always assigned — no NP_NULL_ON_SOME_PATH expected.
    public void knownInstanceof() {
        Base value = new Derived();
        Object result = null;
        if (value instanceof Derived) result = new Object();
        result.toString();
    }

    // value is a parameter (unknown type at compile time) — warning is expected.
    public void unknownInstanceof(Base value) {
        Object result = null;
        if (value instanceof Derived) result = new Object();
        result.toString();
    }
}
