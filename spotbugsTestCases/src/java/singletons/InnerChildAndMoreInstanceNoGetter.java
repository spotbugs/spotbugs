package singletons;

public class InnerChildAndMoreInstanceNoGetter {
    private static class Unknown extends InnerChildAndMoreInstanceNoGetter {
    }

    private static final InnerChildAndMoreInstanceNoGetter INSTANCE = new InnerChildAndMoreInstanceNoGetter();

    private static final InnerChildAndMoreInstanceNoGetter UNKNOWN = new Unknown();

    public static InnerChildAndMoreInstanceNoGetter getUnknown() {
        return UNKNOWN;
    }
}
