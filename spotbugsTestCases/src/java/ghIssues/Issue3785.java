package ghIssues;

public final class Issue3785 {
    private final int value;

    private Issue3785(int value) {
        this.value = value;
    }

    public static Issue3785 create(boolean first) {
        return new Issue3785(first ? 1 : 2);
    }

    public int value() {
        return value;
    }

    public static int impossibleCastInLoop(Object[] items) {
        int total = 0;
        for (int i = 0; i < items.length; i++) {
            Object item = items[i];
            if (item instanceof Integer) {
                total += ((String) item).length();
            }
        }
        return total;
    }
}
