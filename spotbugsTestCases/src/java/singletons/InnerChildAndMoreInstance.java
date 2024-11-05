package singletons;

public class InnerChildAndMoreInstance {
    private static class Unknown extends InnerChildAndMoreInstance {
    }

    private static final InnerChildAndMoreInstance UNKNOWN = new Unknown();

    private static final InnerChildAndMoreInstance INSTANCE = new InnerChildAndMoreInstance();

    public static InnerChildAndMoreInstance getUnknown() {
        return UNKNOWN;
    }
    public static InnerChildAndMoreInstance instance() {
        return INSTANCE;
    }
}
