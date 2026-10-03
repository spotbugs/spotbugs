package ghIssues;

public class Issue4272 {
    static class Base { }

    static final class Derived extends Base { }

    // value is known to be Derived, so this branch is always taken.
    public void knownInstanceof() {
        Base value = new Derived();
        Object result = null;
        if (value instanceof Derived) {
            result = new Object();
        }
        result.toString();
    }

    // value is known to be Derived, so this branch is unreachable.
    public void knownNegatedInstanceof() {
        Base value = new Derived();
        if (!(value instanceof Derived)) {
            Object result = null;
            result.toString();
        }
    }

    // The runtime type of a parameter is unknown, so this dereference remains possible.
    public void unknownInstanceof(Base value) {
        Object result = null;
        if (value instanceof Derived) {
            result = new Object();
        }
        result.toString();
    }
}
