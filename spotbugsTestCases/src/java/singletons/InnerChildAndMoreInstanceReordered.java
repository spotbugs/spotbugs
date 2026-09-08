package singletons;

public class InnerChildAndMoreInstanceReordered {
    private static class Unknown extends InnerChildAndMoreInstanceReordered {
    }

    private static final InnerChildAndMoreInstanceReordered INSTANCE = new InnerChildAndMoreInstanceReordered();

    private static final InnerChildAndMoreInstanceReordered UNKNOWN = new Unknown();

    public static InnerChildAndMoreInstanceReordered instance() {
        return INSTANCE;
    }

    public static InnerChildAndMoreInstanceReordered getUnknown() {
        return UNKNOWN;
    }
}
