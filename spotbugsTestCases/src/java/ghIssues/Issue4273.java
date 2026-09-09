package ghIssues;

public class Issue4273 {

    static class Base {}
    static final class Derived extends Base {}

    // value = new Derived(); instanceof Derived is always true — no NP expected
    public void knownInstanceof() {
        Base value = new Derived();
        Object result = null;
        if (value instanceof Derived) result = new Object();
        result.toString();
    }

    // Negated: !(value instanceof Derived) is always false — body never executes, no NP expected
    public void knownNegatedInstanceof() {
        Base value = new Derived();
        Object result = null;
        if (!(value instanceof Derived)) result = new Object();
        result.toString();
    }

    // value is a parameter (unknown type) — instanceof may be false, NP expected
    public void unknownInstanceof(Base value) {
        Object result = null;
        if (value instanceof Derived) result = new Object();
        result.toString();
    }
}
